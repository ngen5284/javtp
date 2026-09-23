package flashcard.data;

import java.util.List;
import flashcard.model.Word;

/**
 * 단어 목록을 제공하는 역할의 인터페이스.
 * 하드코딩, 파일, 데이터베이스, 서버 API 등 실제 구현 방식에 상관없이
 * GUI(FlashCardFrame)는 이 인터페이스만 통해서 단어 목록을 가져온다.
 */
public interface WordRepository {

    /** 전체 단어 목록을 반환한다. */
    List<Word> getAllWords();
}
