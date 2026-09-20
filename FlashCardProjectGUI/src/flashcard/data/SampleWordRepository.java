package flashcard.data;

import java.util.Arrays;
import java.util.List;
import flashcard.model.Word;

/**
 * [임시용] GUI 테스트를 위해 단어 10개를 제공하는 WordRepository 구현체.
 * 실제 구현체가 완성되면 Main.java에서 이 클래스 대신 그 구현체로 교체하고, 이 파일은 지워도 됨.
 */
public class SampleWordRepository implements WordRepository {

    @Override
    public List<Word> getAllWords() {
        return Arrays.asList(
                new Word("apple", "사과", "I ate an apple for breakfast."),
                new Word("journey", "여행, 여정", "Life is a journey, not a destination."),
                new Word("achieve", "성취하다, 이루다", "She achieved her goal after years of effort."),
                new Word("brave", "용감한", "The brave firefighter saved the child."),
                new Word("curious", "호기심 많은", "The curious cat explored every corner."),
                new Word("diligent", "성실한, 부지런한", "He is a diligent student who studies every day."),
                new Word("essential", "필수적인", "Water is essential for life."),
                new Word("fragile", "부서지기 쉬운", "Please handle the fragile package with care."),
                new Word("generous", "관대한, 후한", "She is generous with her time and money."),
                new Word("harvest", "수확(하다)", "Farmers harvest crops in autumn.")
        );
    }
}
