package flashcard;

import flashcard.data.SampleWordRepository;
import flashcard.data.SqliteWordRepository;
import flashcard.data.WordRepository;
import flashcard.gui.FlashCardFrame;
import flashcard.gui.MainMenuFrame;
import flashcard.progress.ProgressRepository;
import flashcard.progress.SqliteProgressRepository;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** 메인 화면(난이도 선택)이 뜨고, easy/hard/sample을 인자로 주면 해당 덱을 연다.**/
public class Main {
    public static void main(String[] args) {
        String deck = args.length == 0 ? null : args[0].toLowerCase();
        if (args.length > 1 || (deck != null
                && !(deck.equals("easy") || deck.equals("hard") || deck.equals("sample")))) {
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
        		ProgressRepository progress = new SqliteProgressRepository();
        		if(deck == null) {
        			new MainMenuFrame(Main::createWordRepository, progress).setVisible(true);
        		} else {
        			new FlashCardFrame(createWordRepository(deck), progress, deck).setVisible(true);
        		}
        	} catch (RuntimeException e) {
        		JOptionPane.showMessageDialog(null, e.getMessage(), "플래시카드 실행 오류", 
        				JOptionPane.ERROR_MESSAGE);
        		e.printStackTrace();
        	}
    });
}
    
    private static WordRepository createWordRepository(String deck) {
    	return deck.equals("sample")
    			? new SampleWordRepository()
    			: new SqliteWordRepository(Path.of("data", deck + ".db"));
    }
}