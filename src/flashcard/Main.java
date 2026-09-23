package flashcard;

import flashcard.data.SampleWordRepository;
import flashcard.data.SqliteWordRepository;
import flashcard.data.WordRepository;
import flashcard.gui.FlashCardFrame;
import flashcard.progress.ProgressRepository;
import flashcard.progress.SqliteProgressRepository;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point. Run with no arguments for easy, or pass hard/sample. */
public class Main {
    public static void main(String[] args) {
        String deck = args.length == 0 ? "easy" : args[0].toLowerCase();
        if (args.length > 1 || !(deck.equals("easy") || deck.equals("hard") || deck.equals("sample"))) {
            System.err.println("사용법: flashcard.Main [easy|hard|sample]");
            return;
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep Swing's default theme if the platform theme is unavailable.
        }

        SwingUtilities.invokeLater(() -> {
            try {
                WordRepository words = deck.equals("sample")
                        ? new SampleWordRepository()
                        : new SqliteWordRepository(Path.of("data", deck + ".db"));
                ProgressRepository progress = new SqliteProgressRepository();
                FlashCardFrame frame = new FlashCardFrame(words, progress, deck);
                frame.setVisible(true);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null, e.getMessage(), "플래시카드 실행 오류",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
