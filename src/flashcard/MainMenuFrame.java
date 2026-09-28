package flashcard.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import flashcard.data.WordRepository;
import flashcard.model.Word;
import flashcard.progress.LearningProgress;
import flashcard.progress.ProgressRepository;

/**
 * 프로그램 시작 시 보여주는 메인 화면.
 * 쉬움(easy) / 어려움(hard) 덱 중 하나를 고르면 해당 덱으로 FlashCardFrame을 연다.
 */
public class MainMenuFrame extends JFrame {

    private static final Color ACCENT_COLOR = new Color(70, 120, 220);
    private static final Color EASY_COLOR = new Color(200, 230, 201);   // 연두색 (카드 뒷면과 같은 톤)
    private static final Color HARD_COLOR = new Color(255, 224, 178);   // 연주황색
    private static final Color DISABLED_COLOR = new Color(230, 230, 230);

    private final Function<String, WordRepository> repositoryFactory;
    private final ProgressRepository progressRepository;

    /**
     * @param repositoryFactory  덱 이름("easy", "hard")을 받아 그 덱의 WordRepository를 만들어 주는 함수
     * @param progressRepository 학습 진도 저장소 (진도율 표시 및 FlashCardFrame에 전달)
     */
    public MainMenuFrame(Function<String, WordRepository> repositoryFactory,
                         ProgressRepository progressRepository) {
        super("영단어 플래시카드");
        this.repositoryFactory = Objects.requireNonNull(repositoryFactory, "repositoryFactory");
        this.progressRepository = progressRepository;

        initComponents();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(440, 540));
        pack();
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setBorder(BorderFactory.createEmptyBorder(40, 32, 32, 32));
        setContentPane(content);

        // 상단: 제목
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("영단어 플래시카드");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("학습할 난이도를 선택하세요");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        subtitleLabel.setForeground(new Color(100, 100, 100));
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        content.add(titlePanel, BorderLayout.NORTH);

        // 중앙: 난이도 선택 카드 2개
        JPanel deckPanel = new JPanel(new GridLayout(2, 1, 0, 16));
        deckPanel.add(createDeckCard("easy", "쉬움", "기초 필수 영단어", EASY_COLOR));
        deckPanel.add(createDeckCard("hard", "어려움", "심화 영단어", HARD_COLOR));
        content.add(deckPanel, BorderLayout.CENTER);

        // 하단: 조작 안내
        JLabel hintLabel = new JLabel("<html><center>단축키: Space 뒤집기 · ←/→ 이동 · S 섞기<br>"
                + "Home 처음으로 · K 외운 단어 · Esc 메인으로</center></html>",
                SwingConstants.CENTER);
        hintLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hintLabel.setForeground(new Color(140, 140, 140));
        content.add(hintLabel, BorderLayout.SOUTH);
    }

    /** 덱 하나를 나타내는 클릭 가능한 카드를 만든다. 단어 수와 진도율도 함께 보여준다. */
    private JPanel createDeckCard(String deckName, String title, String description, Color color) {
        DeckCardPanel card = new DeckCardPanel(color);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        descLabel.setForeground(new Color(90, 90, 90));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(descLabel);
        card.add(textPanel, BorderLayout.CENTER);

        JLabel statsLabel = new JLabel();
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JProgressBar progressBar = new JProgressBar(0, 1000);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(10, 8));

        // 진도 초기화 버튼 (카드 오른쪽 위). 버튼 클릭은 카드 클릭(덱 열기)으로 이어지지 않는다.
        JButton resetButton = new JButton("초기화");
        resetButton.setFocusable(false);
        resetButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resetButton.setToolTipText(title + " 덱의 외운 단어와 마지막 위치를 모두 지웁니다");
        resetButton.setVisible(progressRepository != null);
        JPanel resetPanel = new JPanel(new BorderLayout());
        resetPanel.setOpaque(false);
        resetPanel.add(resetButton, BorderLayout.NORTH);
        card.add(resetPanel, BorderLayout.EAST);

        JPanel statsPanel = new JPanel(new BorderLayout(0, 6));
        statsPanel.setOpaque(false);
        statsPanel.add(statsLabel, BorderLayout.NORTH);
        statsPanel.add(progressBar, BorderLayout.SOUTH);
        card.add(statsPanel, BorderLayout.SOUTH);

        // 단어 수와 진도율을 미리 읽어 온다. DB가 없거나 읽을 수 없으면 카드를 비활성화한다.
        try {
            List<Word> words = repositoryFactory.apply(deckName).getAllWords();
            updateStats(deckName, words, statsLabel, progressBar);

            resetButton.addActionListener(e -> {
                int answer = JOptionPane.showConfirmDialog(this,
                        "'" + title + "' 덱의 학습 진도를 초기화할까요?\n외운 단어 표시와 마지막 학습 위치가 모두 지워집니다.",
                        "진도 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (answer == JOptionPane.YES_OPTION) {
                    progressRepository.resetProgress(deckName);
                    updateStats(deckName, words, statsLabel, progressBar);
                }
            });

            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    card.setHovered(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    card.setHovered(false);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    openDeck(deckName);
                }
            });
        } catch (RuntimeException e) {
            card.setFaceColor(DISABLED_COLOR);
            titleLabel.setForeground(new Color(150, 150, 150));
            statsLabel.setText("단어 DB를 불러올 수 없습니다");
            statsLabel.setForeground(new Color(200, 60, 60));
            progressBar.setVisible(false);
            resetButton.setVisible(false);
            card.setToolTipText(e.getMessage());
        }

        return card;
    }

    /** 덱의 단어 수, 외운 단어 수, 진도율을 계산해 라벨과 진행 막대에 표시한다. */
    private void updateStats(String deckName, List<Word> words, JLabel statsLabel, JProgressBar progressBar) {
        int total = words.size();
        int known = 0;
        if (progressRepository != null) {
            // 외운 단어 목록을 한 번에 읽어 온 뒤 비교한다 (단어마다 DB 조회하지 않도록).
            LearningProgress saved = progressRepository.loadProgress(deckName);
            known = (int) words.stream()
                    .filter(w -> saved.isKnown(w.getEnglish()))
                    .count();
        }
        double rate = total == 0 ? 0.0 : Math.round(known * 1000.0 / total) / 10.0;
        statsLabel.setText(String.format("단어 %d개 · 외운 단어 %d개 (%.1f%%)", total, known, rate));
        progressBar.setValue(total == 0 ? 0 : (int) Math.round(known * 1000.0 / total));
    }

    /** 선택한 덱으로 플래시카드 화면을 열고 메인 화면은 닫는다. */
    private void openDeck(String deckName) {
        try {
            WordRepository words = repositoryFactory.apply(deckName);
            // "메인으로"를 누르면 메인 화면을 새로 만들어 띄운다 (진도율을 다시 계산하기 위해).
            Runnable backToMenu = () ->
                    new MainMenuFrame(repositoryFactory, progressRepository).setVisible(true);
            FlashCardFrame frame = new FlashCardFrame(words, progressRepository, deckName, backToMenu);
            frame.setVisible(true);
            dispose();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "덱을 열 수 없습니다",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /** 둥근 모서리 + 그림자가 있는 덱 선택 카드. 마우스를 올리면 테두리가 강조된다. */
    private static class DeckCardPanel extends JPanel {
        private Color faceColor;
        private boolean hovered = false;

        DeckCardPanel(Color faceColor) {
            this.faceColor = faceColor;
            setOpaque(false);
        }

        void setFaceColor(Color color) {
            this.faceColor = color;
            repaint();
        }

        void setHovered(boolean hovered) {
            this.hovered = hovered;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 24;
            int shadowOffset = 4;
            double w = getWidth() - shadowOffset - 1;
            double h = getHeight() - shadowOffset - 1;

            g2.setColor(new Color(0, 0, 0, 30));
            g2.fill(new RoundRectangle2D.Double(shadowOffset, shadowOffset, w, h, arc, arc));

            g2.setColor(faceColor);
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, arc, arc));

            if (hovered) {
                g2.setStroke(new java.awt.BasicStroke(3f));
                g2.setColor(ACCENT_COLOR);
            } else {
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.setColor(new Color(200, 200, 200));
            }
            g2.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, arc, arc));

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
