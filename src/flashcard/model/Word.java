package flashcard.model;

/**
 * 영단어 카드 한 장의 데이터(영단어, 뜻, 예문)를 담는 모델 클래스.
 */
public class Word {

    private final String english;   // 영단어 (예: "apple")
    private final String meaning;   // 뜻 (예: "사과")
    private final String example;   // 예문 (없으면 빈 문자열)

    public Word(String english, String meaning, String example) {
        this.english = english;
        this.meaning = meaning;
        this.example = example == null ? "" : example;
    }

    // 예문 없이 단어만 만들 때 쓰는 생성자
    public Word(String english, String meaning) {
        this(english, meaning, "");
    }

    public String getEnglish() {
        return english;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getExample() {
        return example;
    }

    public boolean hasExample() {
        return example != null && !example.isBlank();
    }

    @Override
    public String toString() {
        return english + " - " + meaning;
    }
}
