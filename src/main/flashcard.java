package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class flashcard {
	private static final String EASY_words =
            "jdbc:sqlite:data/easy.db";
	private static final String HARD_words =
            "jdbc:sqlite:data/hard.db";
	
	public static void main(String[] args) {
		
		String sql = "SELECT id, word, meaning FROM words ORDER BY id";

        try (
            Connection connection = DriverManager.getConnection(EASY_words);
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String word = resultSet.getString("word");
                String meaning = resultSet.getString("meaning");

                System.out.printf("%d. %s : %s%n", id, word, meaning);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
	}

}
