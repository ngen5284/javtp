package flashcard.data;

import java.util.Collections;
import java.util.List;
import flashcard.model.Word;

/**
 * 단어를 하나도 담고 있지 않은 WordRepository 구현체.
 * 실제 단어 데이터를 가져오는 구현체가 준비되기 전까지 임시로 사용한다.
 */
public class EmptyWordRepository implements WordRepository {

    @Override
    public List<Word> getAllWords() {
        return Collections.emptyList();
    }
}
