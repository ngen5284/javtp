package flashcard;

import javax.swing.SwingUtilities;

import flashcard.data.SampleWordRepository;
import flashcard.data.WordRepository;
import flashcard.gui.FlashCardFrame;

/**
 * 프로그램 진입점. WordRepository 구현체를 만들어 FlashCardFrame에 주입하고
 * 화면을 띄운다.
 *
 * [임시용] 지금은 GUI 테스트를 위해 SampleWordRepository(단어 10개 하드코딩)를 사용중.
 */
public class Main {

    public static void main(String[] args) {
        // Swing 컴포넌트는 이벤트 디스패치 스레드(EDT)에서 생성/실행해야 한다.
        SwingUtilities.invokeLater(() -> {
            WordRepository repository = new SampleWordRepository();
            FlashCardFrame frame = new FlashCardFrame(repository);
            frame.setVisible(true);
        });
    }
}
