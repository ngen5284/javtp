package flashcard.progress;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SQLite 데이터베이스를 이용한 학습 진도 저장소 구현체.
 * 암기 여부뿐 아니라 단어별 복습 통계 및 일일 학습량도 영구 보존합니다.
 */
public class SqliteProgressRepository implements ProgressRepository {

    private static final String DEFAULT_DB_PATH = "data/progress.db";
    private final String jdbcUrl;

    /** 기본 경로("data/progress.db")를 사용하는 생성자 */
    public SqliteProgressRepository() {
        this(DEFAULT_DB_PATH);
    }

    /** 커스텀 DB 경로를 지정할 수 있는 생성자 */
    public SqliteProgressRepository(String dbPath) {
        ensureParentDirectoryExists(dbPath);
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
        initTables();
    }

    /** 필요한 테이블을 자동 생성 및 마이그레이션합니다. */
    private void initTables() {
        String createWordProgressTable =
                "CREATE TABLE IF NOT EXISTS word_progress (" +
                "    deck_name       TEXT    NOT NULL," +
                "    word            TEXT    NOT NULL," +
                "    is_known        INTEGER NOT NULL DEFAULT 0," +
                "    correct_count   INTEGER NOT NULL DEFAULT 0," +
                "    incorrect_count INTEGER NOT NULL DEFAULT 0," +
                "    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    PRIMARY KEY (deck_name, word)" +
                ");";

        String createDeckProgressTable =
                "CREATE TABLE IF NOT EXISTS deck_progress (" +
                "    deck_name  TEXT PRIMARY KEY," +
                "    last_index INTEGER NOT NULL DEFAULT 0," +
                "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        String createDailyLogTable =
                "CREATE TABLE IF NOT EXISTS daily_study_log (" +
                "    study_date TEXT NOT NULL," +
                "    deck_name  TEXT NOT NULL," +
                "    count      INTEGER NOT NULL DEFAULT 0," +
                "    PRIMARY KEY (study_date, deck_name)" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createWordProgressTable);
            stmt.execute(createDeckProgressTable);
            stmt.execute(createDailyLogTable);

            // 기존 DB가 있던 경우를 위한 컬럼 마이그레이션 (실패해도 무시)
            try {
                stmt.execute("ALTER TABLE word_progress ADD COLUMN correct_count INTEGER NOT NULL DEFAULT 0;");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE word_progress ADD COLUMN incorrect_count INTEGER NOT NULL DEFAULT 0;");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            throw new IllegalStateException("학습 진도 DB를 초기화할 수 없습니다.", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void ensureParentDirectoryExists(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private String normalizeWord(String word) {
        return word == null ? "" : word.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void setWordKnown(String deckName, String word, boolean isKnown) {
        // 단어 외움/안외움 상태 설정 및 복습 카운트 자동 연동
        recordReview(deckName, word, isKnown);
    }

    @Override
    public boolean isWordKnown(String deckName, String word) {
        String normalized = normalizeWord(word);
        if (normalized.isEmpty()) return false;

        String sql = "SELECT is_known FROM word_progress WHERE deck_name = ? AND word = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deckName);
            pstmt.setString(2, normalized);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("is_known") == 1;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("단어 암기 상태를 읽을 수 없습니다.", e);
        }
        return false;
    }

    @Override
    public Set<String> getKnownWords(String deckName) {
        Set<String> knownWords = new HashSet<>();
        String sql = "SELECT word FROM word_progress WHERE deck_name = ? AND is_known = 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deckName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    knownWords.add(rs.getString("word"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("외운 단어 목록을 읽을 수 없습니다.", e);
        }
        return knownWords;
    }

    @Override
    public void saveLastIndex(String deckName, int lastIndex) {
        String sql = "INSERT INTO deck_progress (deck_name, last_index, updated_at) " +
                     "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT(deck_name) DO UPDATE SET " +
                     "last_index = excluded.last_index, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deckName);
            pstmt.setInt(2, Math.max(0, lastIndex));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("마지막 카드 위치를 저장할 수 없습니다.", e);
        }
    }

    @Override
    public int getLastIndex(String deckName) {
        String sql = "SELECT last_index FROM deck_progress WHERE deck_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deckName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("last_index");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("마지막 카드 위치를 읽을 수 없습니다.", e);
        }
        return 0;
    }

    @Override
    public LearningProgress loadProgress(String deckName) {
        Set<String> knownWords = getKnownWords(deckName);
        int lastIndex = getLastIndex(deckName);
        return new LearningProgress(deckName, knownWords, lastIndex);
    }

    @Override
    public void resetProgress(String deckName) {
        String deleteWords = "DELETE FROM word_progress WHERE deck_name = ?";
        String deleteDeck = "DELETE FROM deck_progress WHERE deck_name = ?";
        String deleteDaily = "DELETE FROM daily_study_log WHERE deck_name = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(deleteWords);
                 PreparedStatement pstmt2 = conn.prepareStatement(deleteDeck);
                 PreparedStatement pstmt3 = conn.prepareStatement(deleteDaily)) {
                pstmt1.setString(1, deckName);
                pstmt1.executeUpdate();

                pstmt2.setString(1, deckName);
                pstmt2.executeUpdate();

                pstmt3.setString(1, deckName);
                pstmt3.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("학습 진도를 초기화할 수 없습니다.", e);
        }
    }

    // ========================================================
    // [기능 1] 단어별 오답/정답 횟수 기록 및 취약 단어 TOP N 추출
    // ========================================================

    @Override
    public void recordReview(String deckName, String word, boolean isKnown) {
        String normalized = normalizeWord(word);
        if (normalized.isEmpty()) return;

        int addCorrect = isKnown ? 1 : 0;
        int addIncorrect = isKnown ? 0 : 1;

        String wordSql =
                "INSERT INTO word_progress (deck_name, word, is_known, correct_count, incorrect_count, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(deck_name, word) DO UPDATE SET " +
                "is_known = excluded.is_known, " +
                "correct_count = correct_count + excluded.correct_count, " +
                "incorrect_count = incorrect_count + excluded.incorrect_count, " +
                "updated_at = CURRENT_TIMESTAMP";

        String today = LocalDate.now().toString();
        String dailySql =
                "INSERT INTO daily_study_log (study_date, deck_name, count) " +
                "VALUES (?, ?, 1) " +
                "ON CONFLICT(study_date, deck_name) DO UPDATE SET " +
                "count = count + 1";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(wordSql);
                 PreparedStatement pstmt2 = conn.prepareStatement(dailySql)) {

                pstmt1.setString(1, deckName);
                pstmt1.setString(2, normalized);
                pstmt1.setInt(3, isKnown ? 1 : 0);
                pstmt1.setInt(4, addCorrect);
                pstmt1.setInt(5, addIncorrect);
                pstmt1.executeUpdate();

                pstmt2.setString(1, today);
                pstmt2.setString(2, deckName);
                pstmt2.executeUpdate();

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("복습 결과 기록에 실패했습니다.", e);
        }
    }

    @Override
    public List<WordReviewStat> getDifficultWords(String deckName, int limit) {
        List<WordReviewStat> result = new ArrayList<>();
        String sql =
                "SELECT word, correct_count, incorrect_count " +
                "FROM word_progress " +
                "WHERE deck_name = ? AND incorrect_count > 0 " +
                "ORDER BY incorrect_count DESC, correct_count ASC " +
                "LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deckName);
            pstmt.setInt(2, Math.max(1, limit));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new WordReviewStat(
                            rs.getString("word"),
                            rs.getInt("correct_count"),
                            rs.getInt("incorrect_count")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("취약 단어 목록을 읽을 수 없습니다.", e);
        }
        return result;
    }

    // ========================================================
    // [기능 3] 일일 학습량 및 최근 학습 통계 추적
    // ========================================================

    @Override
    public int getTodayLearnedCount(String deckName) {
        String today = LocalDate.now().toString();
        String sql = "SELECT count FROM daily_study_log WHERE study_date = ? AND deck_name = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            pstmt.setString(2, deckName);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("오늘의 학습량을 읽을 수 없습니다.", e);
        }
        return 0;
    }

    @Override
    public Map<String, Integer> getWeeklyStudyStats(String deckName) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        // 최근 7일 날짜를 0으로 초기화
        for (int i = 6; i >= 0; i--) {
            stats.put(today.minusDays(i).toString(), 0);
        }

        String startDate = today.minusDays(6).toString();
        String sql = "SELECT study_date, count FROM daily_study_log WHERE deck_name = ? AND study_date >= ? ORDER BY study_date ASC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deckName);
            pstmt.setString(2, startDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stats.put(rs.getString("study_date"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("주간 학습 통계를 읽을 수 없습니다.", e);
        }
        return stats;
    }
}
