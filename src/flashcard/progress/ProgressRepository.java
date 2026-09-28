package flashcard.progress;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 학습 진도 데이터를 저장하고 불러오는 저장소 인터페이스.
 * 데이터베이스, 로컬 파일 등 구현 방식에 상관없이 일관된 메서드를 제공합니다.
 */
public interface ProgressRepository {

    /**
     * 특정 단어의 암기 완료 여부를 저장합니다.
     *
     * @param deckName 덱 이름 (예: "easy", "hard")
     * @param word     영단어
     * @param isKnown  외웠는지 여부 (true: 외움, false: 모름)
     */
    void setWordKnown(String deckName, String word, boolean isKnown);

    /**
     * 특정 단어를 이미 외웠는지 확인합니다.
     *
     * @param deckName 덱 이름
     * @param word     영단어
     * @return 외운 단어이면 true, 아니면 false
     */
    boolean isWordKnown(String deckName, String word);

    /**
     * 특정 덱에서 외운 단어들의 목록을 반환합니다.
     *
     * @param deckName 덱 이름
     * @return 외운 영단어 Set (소문자 기준)
     */
    Set<String> getKnownWords(String deckName);

    /**
     * 마지막으로 학습하던 카드의 위치(인덱스)를 저장합니다.
     *
     * @param deckName  덱 이름
     * @param lastIndex 마지막 카드 인덱스 (0부터 시작)
     */
    void saveLastIndex(String deckName, int lastIndex);

    /**
     * 마지막으로 학습하던 카드의 위치(인덱스)를 불러옵니다.
     * 저장된 정보가 없으면 0을 반환합니다.
     *
     * @param deckName 덱 이름
     * @return 마지막 카드 인덱스
     */
    int getLastIndex(String deckName);

    /**
     * 해당 덱의 전체 학습 진도(외운 단어 목록 + 마지막 위치)를 한 번에 불러옵니다.
     *
     * @param deckName 덱 이름
     * @return LearningProgress 객체
     */
    LearningProgress loadProgress(String deckName);

    /**
     * 해당 덱의 학습 진도를 초기화(삭제)합니다.
     *
     * @param deckName 덱 이름
     */
    void resetProgress(String deckName);

    // ========================================================
    // [기능 1] 단어별 오답/정답 횟수 기록 및 취약 단어 TOP N 추출
    // ========================================================

    /**
     * 단어 복습 결과를 기록하고 정답/오답 횟수를 누적합니다.
     * 또한 당일 일일 학습량에도 자동으로 반영됩니다.
     *
     * @param deckName 덱 이름
     * @param word     영단어
     * @param isKnown  외움(정답) 여부 (true: 정답 카운트+1, false: 오답 카운트+1)
     */
    void recordReview(String deckName, String word, boolean isKnown);

    /**
     * 가장 많이 틀린 취약 단어 목록을 내림차순으로 가져옵니다.
     *
     * @param deckName 덱 이름
     * @param limit    최대 반환 개수 (예: 5)
     * @return 취약 단어 통계 목록
     */
    List<WordReviewStat> getDifficultWords(String deckName, int limit);

    // ========================================================
    // [기능 3] 일일 학습량 및 최근 학습 통계 추적
    // ========================================================

    /**
     * 오늘(당일) 학습(복습)한 단어 수를 반환합니다.
     *
     * @param deckName 덱 이름
     * @return 오늘 학습한 단어 수
     */
    int getTodayLearnedCount(String deckName);

    /**
     * 최근 7일간의 날짜별 학습 단어 수 통계를 반환합니다.
     *
     * @param deckName 덱 이름
     * @return 날짜("YYYY-MM-DD") -> 학습한 단어 수 맵 (날짜 오름차순)
     */
    Map<String, Integer> getWeeklyStudyStats(String deckName);
}
