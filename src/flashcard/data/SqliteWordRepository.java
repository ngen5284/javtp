package flashcard.data;

import flashcard.model.Word;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reads an existing easy/hard word deck without modifying it. */
public class SqliteWordRepository implements WordRepository {
    private final Path databasePath;

    public SqliteWordRepository(Path databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath, "databasePath").toAbsolutePath();
        if (!Files.isRegularFile(this.databasePath)) {
            throw new IllegalArgumentException("단어 DB를 찾을 수 없습니다: " + this.databasePath);
        }
    }

    @Override
    public List<Word> getAllWords() {
        List<Word> words = new ArrayList<>();
        String sql = "SELECT word, meaning FROM words ORDER BY id";

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                words.add(new Word(resultSet.getString("word"), resultSet.getString("meaning")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("단어 DB를 읽을 수 없습니다: " + databasePath, e);
        }

        return words;
    }
}
