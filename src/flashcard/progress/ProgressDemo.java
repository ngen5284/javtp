package flashcard.progress;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 학습 진도 저장 모듈의 신규 기능(취약 단어 분석 & 일일 학습량 통계)을 시연하는 데모 클래스.
 */
public class ProgressDemo {

    public static void main(String[] args) {
        System.out.println("=====================================================");
        System.out.println("   [정기훈 담당] 학습 진도 저장 & 분석 모듈 데모     ");
        System.out.println("=====================================================\n");

        String deckName = "easy";
        ProgressRepository repo = new SqliteProgressRepository("data/progress.db");

        // 1. 기존 데이터 초기화
        System.out.println("▶ [1] '" + deckName + "' 덱 진도 초기화");
        repo.resetProgress(deckName);

        // 2. 단어 학습 및 복습 시뮬레이션
        System.out.println("\n▶ [2] 단어 학습 & 복습 시뮬레이션 (정답/오답 누적)");
        System.out.println(" - 'apple'  : 2회 정답 (외움)");
        repo.recordReview(deckName, "apple", true);
        repo.recordReview(deckName, "apple", true);

        System.out.println(" - 'banana' : 1회 틀림, 1회 맞춤");
        repo.recordReview(deckName, "banana", false);
        repo.recordReview(deckName, "banana", true);

        System.out.println(" - 'cherry' : 3회 연속 틀림 (취약 단어)");
        repo.recordReview(deckName, "cherry", false);
        repo.recordReview(deckName, "cherry", false);
        repo.recordReview(deckName, "cherry", false);

        System.out.println(" - 'durian' : 2회 틀림, 1회 맞춤");
        repo.recordReview(deckName, "durian", false);
        repo.recordReview(deckName, "durian", false);
        repo.recordReview(deckName, "durian", true);

        // 마지막 학습 위치 저장
        repo.saveLastIndex(deckName, 15);

        // 3. 기존 기본 기능 확인
        System.out.println("\n▶ [3] 기본 진도 데이터 로드");
        LearningProgress progress = repo.loadProgress(deckName);
        System.out.println(" - 외운 단어 목록: " + progress.getKnownWords());
        System.out.println(" - 마지막 학습 위치: " + progress.getLastIndex() + "번째 카드");
        System.out.println(" - easy.db (800단어) 기준 전체 암기율: " + progress.getProgressRate(800) + "%");

        // 4. [기능 1] 취약 단어 TOP N 추출 시연
        System.out.println("\n▶ [4] [기능 1] 가장 많이 틀린 '취약 단어 TOP 3' 분석 결과");
        List<WordReviewStat> difficultWords = repo.getDifficultWords(deckName, 3);
        int rank = 1;
        for (WordReviewStat stat : difficultWords) {
            System.out.printf("   %d위: %-8s | 오답: %d회, 정답: %d회 | 정답률: %.1f%%%n",
                    rank++, stat.getWord(), stat.getIncorrectCount(), stat.getCorrectCount(), stat.getAccuracyRate());
        }

        // 5. [기능 3] 일일 학습량 및 최근 7일 학습 통계 시연
        System.out.println("\n▶ [5] [기능 3] 일일 학습량 및 최근 7일 학습 현황");
        int todayCount = repo.getTodayLearnedCount(deckName);
        System.out.println(" - 오늘 복습/학습한 총 횟수: " + todayCount + "회");

        System.out.println(" - 최근 7일간 일별 학습 통계:");
        Map<String, Integer> weeklyStats = repo.getWeeklyStudyStats(deckName);
        for (Map.Entry<String, Integer> entry : weeklyStats.entrySet()) {
            String bar = "■".repeat(Math.min(20, entry.getValue()));
            System.out.printf("   %s : %2d회  %s%n", entry.getKey(), entry.getValue(), bar);
        }

        // 6. DB 재연결을 통한 영속성(Persistence) 최종 확인
        System.out.println("\n▶ [6] 앱 재실행 시뮬레이션 (새로운 Repository 인스턴스 생성)");
        ProgressRepository newRepo = new SqliteProgressRepository("data/progress.db");
        System.out.println(" - DB에서 다시 읽어온 오늘의 학습량: " + newRepo.getTodayLearnedCount(deckName) + "회");
        System.out.println(" - DB에서 다시 읽어온 1위 취약 단어: " + newRepo.getDifficultWords(deckName, 1).get(0));

        System.out.println("\n=====================================================");
        System.out.println("        1번과 3번 기능 테스트가 모두 완료되었습니다!  ");
        System.out.println("=====================================================");
    }
}
