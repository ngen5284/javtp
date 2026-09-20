# GUI 통합 시 참고할 점

제가 만든 GUI 코드 합칠 때 미리 알아두면 좋을 것들 정리했습니다

## 현재 GUI 코드 생김새

```
flashcard.gui   - FlashCardFrame.java (화면 전체), Main.java (GUI 실행하는 곳)
flashcard.data  - WordRepository.java (단어 가져오는 규칙), SampleWordRepository.java (테스트용 가짜 단어 10개)
flashcard.model - Word.java (단어 하나의 정보 담는 곳)
```

## 합치기 전에 확인사항

**1. 패키지 위치가 서로 다릅니다**
제 코드는 `flashcard.gui`, `flashcard.data`, `flashcard.model`로 나눠져 있는데, 저장소엔 `main` 패키지(`src/main/flashcard.java`)로 되어 있더라고요. 최종적으로 어떤 구조로 갈지 정하고 옮기면 좋을 것 같습니다.

**2. `module-info.java`에 GUI용 모듈이 빠져있습니다**
지금은 `requires java.sql;`만 있는데, 제 GUI는 Swing(`javax.swing`)을 쓰니까 `requires java.desktop;`도 추가해야 빌드가 됩니다. 안 그러면 컴파일 에러가 납니다.

**3. `.classpath`에 있는 sqlite jar 경로가 개인 컴퓨터 경로입니다**
`G:/eclipse_java/flash_card/lib/...`처럼 되어 있어서 다른 사람 컴퓨터에서 열면 그 파일을 못 찾습니다. `lib/sqlite-jdbc-3.53.4.0.jar`처럼 상대경로로 바꾸는 게 안전할 것 같습니다.

**4. 아직 진짜 DB 연결은 안 돼 있습니다**
지금 GUI는 `SampleWordRepository`라는 임시 단어 10개짜리로만 돌아갑니다. `easy.db`/`hard.db`에서 실제 단어 읽어오는 부분은 연결이 안되어있습니다.

## GUI 실행해서 확인하는 법

1. `Main.java` 실행하면 창이 뜹니다
2. 방향키나 버튼으로 카드 넘기고, 스페이스바나 클릭으로 뒤집습니다
3. `K` 키(또는 "외운 단어로 표시" 버튼)를 누르면 카드에 초록 테두리랑 "✔ 외운 단어" 표시가 뜹니다
4. "모르는 단어만 보기"를 체크하면 필터링됩니다
