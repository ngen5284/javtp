package flashcard.progress;

import java.util.Set;

/**
 * 학습 진도 저장 모듈이 정상적으로 동작하는지 독립적으로 테스트하고 시연하는 클래스.
 */
public class ProgressDemo {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   학습 진도 저장 모듈(Progress) 테스트   ");
        System.out.println("===========================================\n");

        String deckName = "easy";
        ProgressRepository repo = new SqliteProgressRepository("data/progress.db");

        // 1. 기존 테스트 데이터 초기화
        System.out.println("[1] 기존 '" + deckName + "' 덱 진도 초기화");
        repo.resetProgress(deckName);

        // 2. 단어 암기 체크 저장
        System.out.println("\n[2] 단어 암기 상태 저장");
        System.out.println(" - 'apple'  -> 외움(true)");
        System.out.println(" - 'banana' -> 외움(true)");
        System.out.println(" - 'cherry' -> 안 외움(false)");
        repo.setWordKnown(deckName, "apple", true);
        repo.setWordKnown(deckName, "banana", true);
        repo.setWordKnown(deckName, "cherry", false);

        // 3. 마지막 학습 위치(카드 인덱스) 저장
        int lastCardIndex = 7;
        System.out.println("\n[3] 마지막 학습 위치 저장: " + lastCardIndex + "번째 카드");
        repo.saveLastIndex(deckName, lastCardIndex);

        // 4. 데이터 조회 및 검증
        System.out.println("\n[4] 저장된 데이터 조회 검증");
        System.out.println(" - apple 외움 여부: " + repo.isWordKnown(deckName, "apple"));
        System.out.println(" - cherry 외움 여부: " + repo.isWordKnown(deckName, "cherry"));
        System.out.println(" - 저장된 마지막 인덱스: " + repo.getLastIndex(deckName));

        Set<String> knownWords = repo.getKnownWords(deckName);
        System.out.println(" - 외운 단어 전체 목록: " + knownWords);

        // 5. LearningProgress 모델을 통한 전체 진도 정보 확인
        System.out.println("\n[5] LearningProgress 모델 객체로 로드");
        LearningProgress progress = repo.loadProgress(deckName);
        System.out.println(" - " + progress);
        System.out.println(" - 전체 10개 단어 기준 진행률: " + progress.getProgressRate(10) + "%");
        System.out.println(" - 전체 800개 단어(easy.db) 기준 진행률: " + progress.getProgressRate(800) + "%");

        // 6. 새로운 인스턴스로 영속성(Persistence) 확인
        System.out.println("\n[6] 앱 재실행 상황 시뮬레이션 (새 Repository 인스턴스 생성)");
        ProgressRepository newRepoInstance = new SqliteProgressRepository("data/progress.db");
        LearningProgress restored = newRepoInstance.loadProgress(deckName);
        System.out.println(" - DB에서 다시 읽어온 데이터: " + restored);
        System.out.println(" - 외운 단어 수: " + restored.getKnownCount());
        System.out.println(" - 마지막 학습 위치: " + restored.getLastIndex());

        System.out.println("\n===========================================");
        System.out.println("       모든 테스트가 성공했습니다!         ");
        System.out.println("===========================================");
    }
}

