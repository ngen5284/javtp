# 영단어 플래시카드

Swing으로 만든 영단어 학습 앱입니다. `data/easy.db`(800개)와 `data/hard.db`(1,800개)의 단어를 읽고, 암기 여부와 마지막 카드 위치를 `data/progress.db`에 저장합니다.

## 실행

프로젝트 루트에서 실행하세요. 프로젝트에 설정된 Java 25와 포함된 SQLite JDBC JAR가 필요합니다.

```powershell
$sources = Get-ChildItem src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -cp "lib/sqlite-jdbc-3.53.4.0.jar" -d bin $sources
java --enable-native-access=ALL-UNNAMED -cp "bin;lib/sqlite-jdbc-3.53.4.0.jar" flashcard.Main
```

기본 덱은 `easy`입니다. 마지막 명령에 `hard`를 붙이면 어려운 덱을, `sample`을 붙이면 GUI용 예시 단어 10개를 엽니다. Eclipse에서는 `flashcard.Main`을 실행 클래스로 선택하고 작업 디렉터리를 프로젝트 루트로 지정하세요.

## 조작

- 카드 클릭 또는 Space: 앞뒤 뒤집기
- ← / →: 이전 / 다음 카드
- S: 순서 섞기
- K: 외운 단어 표시 또는 취소
- **모르는 단어만 보기**: 아직 외우지 않은 단어만 표시

학습 기록은 덱별로 저장되며, `data/progress.db`는 Git에서 제외됩니다. `flashcard.progress.ProgressDemo`는 임시 DB를 사용하므로 실제 학습 기록에 영향을 주지 않습니다.
