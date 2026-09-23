package flashcard.progress;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * SQLite 데이터베이스를 이용한 학습 진도 저장소 구현체.
 * 데이터베이스 파일 및 테이블이 존재하지 않으면 자동으로 생성합니다.
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

    /** 필요한 테이블을 자동 생성합니다. */
    private void initTables() {
        String createWordProgressTable =
                "CREATE TABLE IF NOT EXISTS word_progress (" +
                "    deck_name  TEXT    NOT NULL," +
                "    word       TEXT    NOT NULL," +
                "    is_known   INTEGER NOT NULL DEFAULT 0," +
                "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "    PRIMARY KEY (deck_name, word)" +
                ");";

        String createDeckProgressTable =
                "CREATE TABLE IF NOT EXISTS deck_progress (" +
                "    deck_name  TEXT PRIMARY KEY," +
                "    last_index INTEGER NOT NULL DEFAULT 0," +
                "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createWordProgressTable);
            stmt.execute(createDeckProgressTable);
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
        String normalized = normalizeWord(word);
        if (normalized.isEmpty()) return;

        String sql = "INSERT INTO word_progress (deck_name, word, is_known, updated_at) " +
                     "VALUES (?, ?, ?, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT(deck_name, word) DO UPDATE SET " +
                     "is_known = excluded.is_known, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, deckName);
            pstmt.setString(2, normalized);
            pstmt.setInt(3, isKnown ? 1 : 0);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("단어 암기 상태를 저장할 수 없습니다.", e);
        }
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

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt1 = conn.prepareStatement(deleteWords);
                 PreparedStatement pstmt2 = conn.prepareStatement(deleteDeck)) {
                pstmt1.setString(1, deckName);
                pstmt1.executeUpdate();

                pstmt2.setString(1, deckName);
                pstmt2.executeUpdate();

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
}
