package flashcard.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import flashcard.data.WordRepository;
import flashcard.model.Word;
import flashcard.progress.LearningProgress;
import flashcard.progress.ProgressRepository;

/**
 * 영단어 플래시카드 메인 화면.
 * WordRepository 인터페이스를 통해서만 단어 데이터를 받아오므로,
 * 실제 데이터가 어디서 오는지와 무관하게 동작한다.
 */
public class FlashCardFrame extends JFrame {

    private static final Color FRONT_COLOR = new Color(255, 255, 255); // 영어 면 배경색
    private static final Color BACK_COLOR = new Color(200, 230, 201);  // 뜻 면 배경색
    private static final Color ACCENT_COLOR = new Color(70, 120, 220);

    private final WordRepository repository;
    private final ProgressRepository progressRepository;
    private final String deckName;
    private final Runnable onBackToMenu; // null이면 "메인으로" 버튼 없음

    private final List<Word> allWords = new ArrayList<>();   // repository에서 불러온 전체 단어
    private final List<Word> words = new ArrayList<>();      // 현재 화면에 보여줄(필터링된) 단어

    // 단어별 "외웠어요" 체크 상태. Word가 equals/hashCode를 재정의하지 않았으므로
    // 여기서는 객체 동일성(같은 인스턴스인지) 기준으로 구분된다.
    private final Map<Word, Boolean> knownMap = new HashMap<>();

    private int currentIndex = 0;
    private boolean showingMeaning = false;

    /** 화면에 보여줄 단어 범위 */
    private enum FilterMode { ALL, UNKNOWN_ONLY, KNOWN_ONLY }
    private FilterMode filterMode = FilterMode.ALL;

    private static final Color KNOWN_BORDER_COLOR = new Color(56, 142, 60); // 외운 단어 카드 테두리색

    // ----- UI 컴포넌트 -----
    private RoundedCardPanel cardPanel;
    private JLabel cardLabel;
    private JLabel exampleLabel;
    private JLabel knownBadgeLabel;
    private JLabel progressLabel;
    private JButton prevButton;
    private JButton nextButton;
    private JButton flipButton;
    private JButton shuffleButton;
    private JButton firstButton;
    private JButton knownToggleButton;
    private JCheckBox unknownOnlyCheckBox;
    private JCheckBox knownOnlyCheckBox;

    public FlashCardFrame(WordRepository repository) {
        this(repository, null, null);
    }

    public FlashCardFrame(WordRepository repository, ProgressRepository progressRepository, String deckName) {
        this(repository, progressRepository, deckName, null);
    }

    /**
     * @param onBackToMenu "메인으로" 버튼을 눌렀을 때 실행할 동작. null이면 버튼을 표시하지 않는다.
     */
    public FlashCardFrame(WordRepository repository, ProgressRepository progressRepository, String deckName,
                          Runnable onBackToMenu) {
        super("영단어 플래시카드");
        this.repository = Objects.requireNonNull(repository, "repository는 null일 수 없습니다.");
        this.progressRepository = progressRepository;
        this.deckName = deckName;
        this.onBackToMenu = onBackToMenu;
        if (progressRepository != null && (deckName == null || deckName.isBlank())) {
            throw new IllegalArgumentException("진도를 저장할 덱 이름이 필요합니다.");
        }

        loadWords();
        if (progressRepository != null) {
            LearningProgress saved = progressRepository.loadProgress(deckName);
            for (Word word : allWords) {
                knownMap.put(word, saved.isKnown(word.getEnglish()));
            }
            currentIndex = Math.min(saved.getLastIndex(), Math.max(0, words.size() - 1));
        }
        initComponents();
        installKeyboardShortcuts();
        showCurrentCard();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(440, 540));
        pack();
        setLocationRelativeTo(null);
    }

    /** repository에서 단어를 읽어와 allWords를 채우고 필터를 적용한다. */
    private void loadWords() {
        allWords.clear();
        allWords.addAll(repository.getAllWords());
        applyFilter();
    }

    /** filterMode 값에 따라 words 리스트를 다시 구성한다. */
    private void applyFilter() {
        words.clear();
        if (filterMode == FilterMode.ALL) {
            words.addAll(allWords);
        } else {
            boolean wantKnown = filterMode == FilterMode.KNOWN_ONLY;
            List<Word> filtered = allWords.stream()
                    .filter(w -> knownMap.getOrDefault(w, false) == wantKnown)
                    .collect(Collectors.toList());
            if (filtered.isEmpty()) {
                // 조건에 맞는 단어가 하나도 없으면 필터를 해제하고 전체를 보여준다.
                String message = wantKnown
                        ? "아직 외운 단어가 없습니다.\n전체 단어를 보여줍니다."
                        : "모든 단어를 외웠습니다!\n전체 단어를 보여줍니다.";
                setFilterMode(FilterMode.ALL);
                words.addAll(allWords);
                if (isShowing()) {
                    JOptionPane.showMessageDialog(this, message, "알림", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                words.addAll(filtered);
            }
        }
        if (currentIndex >= words.size()) {
            currentIndex = 0;
        }
    }

    /** 필터 모드를 바꾸고 두 체크박스 상태를 그에 맞춘다 (둘 중 하나만 켜질 수 있음). */
    private void setFilterMode(FilterMode mode) {
        filterMode = mode;
        if (unknownOnlyCheckBox != null) {
            unknownOnlyCheckBox.setSelected(mode == FilterMode.UNKNOWN_ONLY);
            knownOnlyCheckBox.setSelected(mode == FilterMode.KNOWN_ONLY);
        }
    }

    /** 체크박스를 눌렀을 때: 켜면 해당 모드로, 끄면 전체 보기로 전환한다. */
    private void onFilterCheckBox(JCheckBox source, FilterMode mode) {
        setFilterMode(source.isSelected() ? mode : FilterMode.ALL);
        currentIndex = 0;
        applyFilter();
        showCurrentCard();
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // 상단: "메인으로" 버튼 + 진행도 표시
        JPanel topPanel = new JPanel(new BorderLayout(8, 0));
        progressLabel = new JLabel("", SwingConstants.CENTER);
        progressLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        topPanel.add(progressLabel, BorderLayout.CENTER);

        // 메인 화면에서 열린 경우에만 "메인으로" 버튼을 보여준다.
        if (onBackToMenu != null) {
            JButton backButton = new JButton("◀ 메인으로");
            backButton.setFocusable(false);
            backButton.setToolTipText("난이도 선택 화면으로 돌아갑니다 (Esc)");
            backButton.addActionListener(e -> backToMenu());
            topPanel.add(backButton, BorderLayout.WEST);
        }
        add(topPanel, BorderLayout.NORTH);

        // 중앙: 카드 (둥근 모서리 + 그림자, 앞/뒤 면 배경색 구분)
        cardPanel = new RoundedCardPanel();
        cardPanel.setLayout(new GridBagLayout());
        cardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cardPanel.setPreferredSize(new Dimension(380, 240));
        cardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                flipCard();
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(8, 8, 8, 8);

        cardLabel = new JLabel("", SwingConstants.CENTER);
        cardLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        cardPanel.add(cardLabel, gbc);

        gbc.gridy = 1;
        exampleLabel = new JLabel("", SwingConstants.CENTER);
        exampleLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        exampleLabel.setForeground(new Color(100, 100, 100));
        cardPanel.add(exampleLabel, gbc);

        // 카드 위쪽에 "외운 단어" 여부를 보여주는 배지. 평소엔 숨겨져 있다가
        // 현재 단어가 외운 상태일 때만 텍스트가 나타난다.
        knownBadgeLabel = new JLabel("✔ 외운 단어");
        knownBadgeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        knownBadgeLabel.setForeground(KNOWN_BORDER_COLOR);
        knownBadgeLabel.setVisible(false);

        JPanel badgeRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(knownBadgeLabel);

        JPanel cardContainer = new JPanel(new BorderLayout());
        cardContainer.add(badgeRow, BorderLayout.NORTH);
        cardContainer.add(cardPanel, BorderLayout.CENTER);

        add(cardContainer, BorderLayout.CENTER);

        // 하단: 버튼들
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints bgc = new GridBagConstraints();
        bgc.insets = new Insets(4, 4, 4, 4);
        bgc.fill = GridBagConstraints.HORIZONTAL;

        prevButton = new JButton("◀ 이전");
        flipButton = new JButton("뒤집기 (Space)");
        nextButton = new JButton("다음 ▶");
        shuffleButton = new JButton("섞기 (S)");
        firstButton = new JButton("처음으로 (Home)");
        knownToggleButton = new JButton("✔ 외운 단어로 표시");
        knownToggleButton.setForeground(ACCENT_COLOR);

        // 보기 필터: 두 체크박스 중 하나만 켜질 수 있다. 둘 다 꺼져 있으면 전체 보기.
        unknownOnlyCheckBox = new JCheckBox("모르는 단어만 보기");
        knownOnlyCheckBox = new JCheckBox("외운 단어만 보기");
        unknownOnlyCheckBox.addActionListener(e -> onFilterCheckBox(unknownOnlyCheckBox, FilterMode.UNKNOWN_ONLY));
        knownOnlyCheckBox.addActionListener(e -> onFilterCheckBox(knownOnlyCheckBox, FilterMode.KNOWN_ONLY));
        JPanel filterRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 16, 0));
        filterRow.add(unknownOnlyCheckBox);
        filterRow.add(knownOnlyCheckBox);

        // 버튼이 포커스를 갖지 않게 해서, 스페이스바 등 단축키가 버튼 클릭이 아니라
        // installKeyboardShortcuts()에 등록한 동작으로만 처리되게 한다.
        prevButton.setFocusable(false);
        flipButton.setFocusable(false);
        nextButton.setFocusable(false);
        shuffleButton.setFocusable(false);
        firstButton.setFocusable(false);
        knownToggleButton.setFocusable(false);
        unknownOnlyCheckBox.setFocusable(false);
        knownOnlyCheckBox.setFocusable(false);

        // "섞기"와 "처음으로"는 한 줄에 반씩
        JPanel orderRow = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        orderRow.add(shuffleButton);
        orderRow.add(firstButton);

        bgc.gridx = 0; bgc.gridy = 0; bgc.gridwidth = 3;
        buttonPanel.add(filterRow, bgc);
        bgc.gridwidth = 1;
        bgc.gridx = 0; bgc.gridy = 1; buttonPanel.add(prevButton, bgc);
        bgc.gridx = 1; bgc.gridy = 1; buttonPanel.add(flipButton, bgc);
        bgc.gridx = 2; bgc.gridy = 1; buttonPanel.add(nextButton, bgc);
        bgc.gridx = 0; bgc.gridy = 2; bgc.gridwidth = 3;
        buttonPanel.add(orderRow, bgc);
        bgc.gridx = 0; bgc.gridy = 3; bgc.gridwidth = 3;
        buttonPanel.add(knownToggleButton, bgc);

        add(buttonPanel, BorderLayout.SOUTH);

        prevButton.addActionListener(e -> previousCard());
        nextButton.addActionListener(e -> nextCard());
        flipButton.addActionListener(e -> flipCard());
        shuffleButton.addActionListener(e -> shuffleCards());
        firstButton.addActionListener(e -> goToFirstCard());
        knownToggleButton.addActionListener(e -> toggleKnownForCurrentWord());
    }

    /** 방향키/스페이스바/S/K 키로도 조작할 수 있도록 단축키를 등록한다. */
    private void installKeyboardShortcuts() {
        JComponent root = getRootPane();

        bindKey(root, KeyEvent.VK_SPACE, "flip", e -> flipCard());
        bindKey(root, KeyEvent.VK_LEFT, "prev", e -> previousCard());
        bindKey(root, KeyEvent.VK_RIGHT, "next", e -> nextCard());
        bindKey(root, KeyEvent.VK_S, "shuffle", e -> shuffleCards());
        bindKey(root, KeyEvent.VK_K, "toggleKnown", e -> toggleKnownForCurrentWord());
        bindKey(root, KeyEvent.VK_HOME, "first", e -> goToFirstCard());
        if (onBackToMenu != null) {
            bindKey(root, KeyEvent.VK_ESCAPE, "backToMenu", e -> backToMenu());
        }
    }

    /** 현재 창을 닫고 메인 화면으로 돌아간다. 진도는 이미 DB에 저장되어 있다. */
    private void backToMenu() {
        dispose();
        onBackToMenu.run();
    }

    private void bindKey(JComponent component, int keyCode, String name, java.util.function.Consumer<ActionEvent> action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        });
    }

    /** 현재 인덱스의 단어를 카드 앞면(영단어) 상태로 화면에 표시한다. */
    private void showCurrentCard() {
        if (words.isEmpty()) {
            cardLabel.setText("표시할 단어가 없습니다");
            exampleLabel.setText(" ");
            progressLabel.setText("0 / 0");
            knownBadgeLabel.setVisible(false);
            cardPanel.setKnown(false);
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            flipButton.setEnabled(false);
            shuffleButton.setEnabled(false);
            firstButton.setEnabled(false);
            knownToggleButton.setEnabled(false);
            return;
        }
        prevButton.setEnabled(true);
        nextButton.setEnabled(true);
        flipButton.setEnabled(true);
        shuffleButton.setEnabled(true);
        firstButton.setEnabled(true);
        knownToggleButton.setEnabled(true);
        Word word = words.get(currentIndex);
        showingMeaning = false;

        cardLabel.setText(word.getEnglish());
        exampleLabel.setText(word.hasExample() ? word.getExample() : " ");
        cardPanel.setFaceColor(FRONT_COLOR);
        updateProgressLabel();
        updateKnownButtonLabel(word);
        if (progressRepository != null) {
            progressRepository.saveLastIndex(deckName, currentIndex);
        }
    }

    /** 카드를 뒤집어 영단어 면 ↔ 뜻 면을 전환한다. */
    private void flipCard() {
        if (words.isEmpty()) {
            return;
        }
        Word word = words.get(currentIndex);
        showingMeaning = !showingMeaning;

        if (showingMeaning) {
            cardLabel.setText(word.getMeaning());
            cardPanel.setFaceColor(BACK_COLOR);
        } else {
            cardLabel.setText(word.getEnglish());
            cardPanel.setFaceColor(FRONT_COLOR);
        }
        exampleLabel.setText(word.hasExample() ? word.getExample() : " ");
    }

    private void nextCard() {
        if (words.isEmpty()) {
            return;
        }
        currentIndex = (currentIndex + 1) % words.size();
        showCurrentCard();
    }

    private void previousCard() {
        if (words.isEmpty()) {
            return;
        }
        currentIndex = (currentIndex - 1 + words.size()) % words.size();
        showCurrentCard();
    }

    /** 현재 필터링된 단어 목록의 순서를 무작위로 섞는다. */
    private void shuffleCards() {
        Collections.shuffle(words);
        currentIndex = 0;
        showCurrentCard();
    }

    /** 현재 단어의 "외웠음" 상태를 토글한다. */
    private void toggleKnownForCurrentWord() {
        if (words.isEmpty()) {
            return;
        }
        Word word = words.get(currentIndex);
        boolean nowKnown = !knownMap.getOrDefault(word, false);
        if (progressRepository != null) {
            progressRepository.setWordKnown(deckName, word.getEnglish(), nowKnown);
        }
        knownMap.put(word, nowKnown);

        // 필터 조건에서 벗어난 단어(모르는 단어만 보기에서 외움 처리 / 외운 단어만 보기에서 취소)는
        // 목록에서 바로 뺀다. 섞은 순서를 유지하려고 applyFilter() 대신 현재 단어만 제거한다.
        boolean leavesFilter = (filterMode == FilterMode.UNKNOWN_ONLY && nowKnown)
                || (filterMode == FilterMode.KNOWN_ONLY && !nowKnown);
        if (leavesFilter) {
            words.remove(currentIndex);
            if (words.isEmpty()) {
                applyFilter(); // 남은 단어가 없으면 안내 후 전체 보기로 전환
            } else if (currentIndex >= words.size()) {
                currentIndex = 0;
            }
        }
        showCurrentCard();
    }

    /** 현재 목록의 1번 단어로 돌아간다. */
    private void goToFirstCard() {
        if (words.isEmpty()) {
            return;
        }
        currentIndex = 0;
        showCurrentCard();
    }

    /** "n / 전체  (외운 단어: k / 전체)" 형식으로 진행 상황을 갱신한다. */
    private void updateProgressLabel() {
        long knownCount = allWords.stream().filter(w -> knownMap.getOrDefault(w, false)).count();
        progressLabel.setText(String.format("%d / %d  (외운 단어: %d / %d)",
                currentIndex + 1, words.size(), knownCount, allWords.size()));
    }

    /** 현재 단어의 암기 상태에 맞게 버튼 텍스트, 카드 배지, 카드 테두리를 갱신한다. */
    private void updateKnownButtonLabel(Word word) {
        boolean known = knownMap.getOrDefault(word, false);
        knownToggleButton.setText(known ? "↺ 외운 단어 취소 (K)" : "✔ 외운 단어로 표시 (K)");
        knownBadgeLabel.setVisible(known);
        cardPanel.setKnown(known);
    }

    /**
     * 둥근 모서리와 그림자가 있는 카드 패널.
     * setFaceColor()로 앞/뒷면 배경색을 바꿔가며 사용한다.
     */
    private static class RoundedCardPanel extends JPanel {
        private Color faceColor = FRONT_COLOR;
        private boolean known = false; // true면 외운 단어 표시로 테두리를 굵은 초록색으로 그린다

        RoundedCardPanel() {
            setOpaque(false);
        }

        void setFaceColor(Color color) {
            this.faceColor = color;
            repaint();
        }

        void setKnown(boolean known) {
            this.known = known;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 28;
            int shadowOffset = 4;

            // 그림자
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fill(new RoundRectangle2D.Double(
                    shadowOffset, shadowOffset,
                    getWidth() - shadowOffset - 1, getHeight() - shadowOffset - 1, arc, arc));

            // 카드 본체
            g2.setColor(faceColor);
            g2.fill(new RoundRectangle2D.Double(
                    0, 0, getWidth() - shadowOffset - 1, getHeight() - shadowOffset - 1, arc, arc));

            // 테두리: 외운 단어면 굵은 초록색, 아니면 얇은 회색
            RoundRectangle2D.Double border = new RoundRectangle2D.Double(
                    0, 0, getWidth() - shadowOffset - 1, getHeight() - shadowOffset - 1, arc, arc);
            if (known) {
                g2.setStroke(new java.awt.BasicStroke(4f));
                g2.setColor(KNOWN_BORDER_COLOR);
            } else {
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.setColor(new Color(200, 200, 200));
            }
            g2.draw(border);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
