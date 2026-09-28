package flashcard.progress;

import java.util.Objects;

/**
 * 단어별 복습 이력 통계 정보(정답/오답 횟수, 정답률 등)를 담는 클래스.
 */
public class WordReviewStat {

    private final String word;
    private final int correctCount;
    private final int incorrectCount;

    public WordReviewStat(String word, int correctCount, int incorrectCount) {
        this.word = Objects.requireNonNull(word, "word는 null일 수 없습니다.");
        this.correctCount = Math.max(0, correctCount);
        this.incorrectCount = Math.max(0, incorrectCount);
    }

    public String getWord() {
        return word;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getIncorrectCount() {
        return incorrectCount;
    }

    /** 총 복습(테스트) 횟수 */
    public int getTotalReviewCount() {
        return correctCount + incorrectCount;
    }

    /** 정답률 (0.0% ~ 100.0%) */
    public double getAccuracyRate() {
        int total = getTotalReviewCount();
        if (total == 0) return 0.0;
        double rate = ((double) correctCount / total) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    @Override
    public String toString() {
        return String.format("%s (틀림: %d회, 맞춤: %d회, 정답률: %.1f%%)",
                word, incorrectCount, correctCount, getAccuracyRate());
    }
}
