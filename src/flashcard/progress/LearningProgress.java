package flashcard.progress;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 특정 덱(난이도)의 학습 진도 정보를 담는 데이터 모델 클래스.
 */
public class LearningProgress {

    private final String deckName;
    private final Set<String> knownWords;
    private int lastIndex;

    public LearningProgress(String deckName) {
        this(deckName, new HashSet<>(), 0);
    }

    public LearningProgress(String deckName, Set<String> knownWords, int lastIndex) {
        this.deckName = Objects.requireNonNull(deckName, "deckName은 null일 수 없습니다.");
        this.knownWords = new HashSet<>(knownWords != null ? knownWords : Collections.emptySet());
        this.lastIndex = Math.max(0, lastIndex);
    }

    public String getDeckName() {
        return deckName;
    }

    /** 외운 단어 목록(읽기 전용 Set)을 반환합니다. */
    public Set<String> getKnownWords() {
        return Collections.unmodifiableSet(knownWords);
    }

    /** 특정 단어를 외웠는지 여부를 확인합니다. */
    public boolean isKnown(String word) {
        if (word == null) return false;
        return knownWords.contains(word.trim().toLowerCase(Locale.ROOT));
    }

    /** 단어의 암기 상태를 설정합니다. */
    public void setKnown(String word, boolean known) {
        if (word == null || word.isBlank()) return;
        String key = word.trim().toLowerCase(Locale.ROOT);
        if (known) {
            knownWords.add(key);
        } else {
            knownWords.remove(key);
        }
    }

    /** 현재까지 외운 단어 수 */
    public int getKnownCount() {
        return knownWords.size();
    }

    /** 마지막으로 학습하던 카드의 인덱스 */
    public int getLastIndex() {
        return lastIndex;
    }

    public void setLastIndex(int lastIndex) {
        this.lastIndex = Math.max(0, lastIndex);
    }

    /**
     * 전체 단어 수 대비 암기 진도율(0.0% ~ 100.0%)을 반환합니다.
     */
    public double getProgressRate(int totalWordCount) {
        if (totalWordCount <= 0) return 0.0;
        double rate = ((double) knownWords.size() / totalWordCount) * 100.0;
        return Math.min(100.0, Math.round(rate * 10.0) / 10.0);
    }

    @Override
    public String toString() {
        return String.format("LearningProgress[deck='%s', knownCount=%d, lastIndex=%d]",
                deckName, knownWords.size(), lastIndex);
    }
}
