import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

public class LoginApp {

    static final class PasswordHasher {
        private static final int ITERATIONS = 120_000;
        private static final int KEY_LENGTH = 256;
        private static final SecureRandom RANDOM = new SecureRandom();

        static byte[] newSalt() {
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            return salt;
        }

        static byte[] hash(char[] password, byte[] salt) {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec).getEncoded();
            } catch (Exception e) {
                throw new IllegalStateException("해시 알고리즘을 사용할 수 없습니다.", e);
            } finally {
                spec.clearPassword();
            }
        }

        static boolean matches(char[] password, byte[] salt, byte[] expected) {
            return MessageDigest.isEqual(hash(password, salt), expected);
        }
    }

    static final class User {
        final String username;
        final byte[] salt;
        final byte[] passwordHash;
        int failedAttempts = 0;
        Instant lockedUntil = null;

        User(String username, byte[] salt, byte[] passwordHash) {
            this.username = username;
            this.salt = salt;
            this.passwordHash = passwordHash;
        }

        boolean isLocked() {
            return lockedUntil != null && Instant.now().isBefore(lockedUntil);
        }
    }
    static final class UserStore {
        private final Path file;
        private final Map<String, User> users = new HashMap<>();

        UserStore(Path file) {
            this.file = file;
            load();
        }

        synchronized User find(String username) {
            return users.get(username.toLowerCase());
        }

        synchronized void add(User user) {
            users.put(user.username.toLowerCase(), user);
            save();
        }

        private void load() {
            if (!Files.exists(file)) return;
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] p = line.split("\\|");
                    if (p.length != 3) continue;
                    users.put(p[0].toLowerCase(), new User(p[0],
                            Base64.getDecoder().decode(p[1]),
                            Base64.getDecoder().decode(p[2])));
                }
            } catch (IOException | IllegalArgumentException e) {
                System.err.println("사용자 파일을 읽지 못했습니다: " + e.getMessage());
            }
        }

        private void save() {
            List<String> lines = new ArrayList<>();
            for (User u : users.values()) {
                lines.add(u.username + "|"
                        + Base64.getEncoder().encodeToString(u.salt) + "|"
                        + Base64.getEncoder().encodeToString(u.passwordHash));
            }
            try {
                Files.write(file, lines, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static final class AuthService {
        private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
        private static final int MAX_FAILS = 5;
        private static final Duration LOCK_TIME = Duration.ofMinutes(5);

        private final UserStore store;
        private String currentUser = null;

        AuthService(UserStore store) {
            this.store = store;
        }

        String register(String username, char[] password) {
            if (!USERNAME.matcher(username).matches())
                return "아이디는 영문/숫자/밑줄 4~20자여야 합니다.";
            if (!isStrong(password))
                return "비밀번호는 8자 이상, 영문자와 숫자를 모두 포함해야 합니다.";
            if (store.find(username) != null)
                return "이미 사용 중인 아이디입니다.";
            try {
                byte[] salt = PasswordHasher.newSalt();
                store.add(new User(username, salt, PasswordHasher.hash(password, salt)));
            } catch (UncheckedIOException e) {
                return "계정 저장 중 오류가 발생했습니다: " + e.getMessage();
            }
            return null;
        }

        String login(String username, char[] password) {
            User user = store.find(username);

            if (user == null) {
                PasswordHasher.hash(password, PasswordHasher.newSalt());
                return "아이디 또는 비밀번호가 올바르지 않습니다.";
            }
            if (user.isLocked()) {
                long sec = Duration.between(Instant.now(), user.lockedUntil).getSeconds();
                return "계정이 잠겨 있습니다. " + (sec + 1) + "초 후 다시 시도하세요.";
            }
            if (PasswordHasher.matches(password, user.salt, user.passwordHash)) {
                user.failedAttempts = 0;
                user.lockedUntil = null;
                currentUser = user.username;
                return null;
            }

            user.failedAttempts++;
            if (user.failedAttempts >= MAX_FAILS) {
                user.lockedUntil = Instant.now().plus(LOCK_TIME);
                user.failedAttempts = 0;
                return "로그인 " + MAX_FAILS + "회 실패로 계정이 " + LOCK_TIME.toMinutes() + "분간 잠깁니다.";
            }
            return "아이디 또는 비밀번호가 올바르지 않습니다. (남은 시도: "
                    + (MAX_FAILS - user.failedAttempts) + "회)";
        }

        void logout() {
            currentUser = null;
        }

        String getCurrentUser() {
            return currentUser;
        }

        private boolean isStrong(char[] pw) {
            if (pw.length < 8) return false;
            boolean letter = false, digit = false;
            for (char c : pw) {
                if (Character.isLetter(c)) letter = true;
                if (Character.isDigit(c)) digit = true;
            }
            return letter && digit;
        }
    }

    static final class App extends JFrame {
        private final AuthService auth;
        private final CardLayout cards = new CardLayout();
        private final JPanel root = new JPanel(cards);

        private final LoginPanel loginPanel;
        private final RegisterPanel registerPanel;
        private final HomePanel homePanel;

        App(AuthService auth) {
            super("로그인 시스템");
            this.auth = auth;

            loginPanel = new LoginPanel();
            registerPanel = new RegisterPanel();
            homePanel = new HomePanel();

            root.add(loginPanel, "login");
            root.add(registerPanel, "register");
            root.add(homePanel, "home");

            setDefaultCloseOperation(EXIT_ON_CLOSE);
            add(root);
            pack();
            setResizable(false);
            setLocationRelativeTo(null);
            showCard("login");
        }

        void showCard(String name) {
            cards.show(root, name);
            switch (name) {
                case "login":
                    getRootPane().setDefaultButton(loginPanel.loginButton);
                    loginPanel.idField.requestFocusInWindow();
                    break;
                case "register":
                    getRootPane().setDefaultButton(registerPanel.registerButton);
                    registerPanel.idField.requestFocusInWindow();
                    break;
                default:
                    getRootPane().setDefaultButton(homePanel.logoutButton);
                    break;
            }
        }

        abstract class FormPanel extends JPanel {
            private int row = 0;
            final JLabel message = new JLabel(" ", SwingConstants.CENTER);

            FormPanel(String title) {
                setLayout(new GridBagLayout());
                setBorder(BorderFactory.createEmptyBorder(24, 40, 24, 40));
                JLabel t = new JLabel(title, SwingConstants.CENTER);
                t.setFont(t.getFont().deriveFont(Font.BOLD, 22f));
                addFull(t);
            }

            void addField(String label, JComponent field) {
                GridBagConstraints g = new GridBagConstraints();
                g.gridx = 0;
                g.gridy = row;
                g.anchor = GridBagConstraints.WEST;
                g.insets = new Insets(6, 0, 6, 12);
                add(new JLabel(label), g);

                g.gridx = 1;
                g.weightx = 1;
                g.fill = GridBagConstraints.HORIZONTAL;
                g.insets = new Insets(6, 0, 6, 0);
                add(field, g);
                row++;
            }

            void addFull(JComponent c) {
                GridBagConstraints g = new GridBagConstraints();
                g.gridx = 0;
                g.gridy = row++;
                g.gridwidth = 2;
                g.fill = GridBagConstraints.HORIZONTAL;
                g.insets = new Insets(8, 0, 8, 0);
                add(c, g);
            }

            void info(String text, boolean ok) {
                message.setForeground(ok ? new Color(0, 130, 0) : new Color(200, 30, 30));
                message.setText(text);
            }
        }

        final class LoginPanel extends FormPanel {
            final JTextField idField = new JTextField(16);
            final JPasswordField pwField = new JPasswordField(16);
            final JButton loginButton = new JButton("로그인");
            final JButton toRegisterButton = new JButton("회원가입");

            LoginPanel() {
                super("로그인");
                addField("아이디", idField);
                addField("비밀번호", pwField);
                addFull(loginButton);
                addFull(toRegisterButton);
                addFull(message);

                loginButton.addActionListener(e -> doLogin());
                toRegisterButton.addActionListener(e -> {
                    registerPanel.clear();
                    showCard("register");
                });
            }

            private void doLogin() {
                char[] pw = pwField.getPassword();
                String err = auth.login(idField.getText().trim(), pw);
                Arrays.fill(pw, '\0');
                pwField.setText("");

                if (err == null) {
                    idField.setText("");
                    info(" ", true);
                    homePanel.setUser(auth.getCurrentUser());
                    showCard("home");
                } else {
                    info(err, false);
                }
            }
        }

        final class RegisterPanel extends FormPanel {
            final JTextField idField = new JTextField(16);
            final JPasswordField pwField = new JPasswordField(16);
            final JPasswordField pw2Field = new JPasswordField(16);
            final JButton registerButton = new JButton("가입하기");
            final JButton backButton = new JButton("돌아가기");

            RegisterPanel() {
                super("회원가입");
                addField("아이디", idField);
                addField("비밀번호", pwField);
                addField("비밀번호 확인", pw2Field);
                addFull(registerButton);
                addFull(backButton);
                addFull(message);

                registerButton.addActionListener(e -> doRegister());
                backButton.addActionListener(e -> showCard("login"));
            }

            void clear() {
                idField.setText("");
                pwField.setText("");
                pw2Field.setText("");
                info(" ", true);
            }

            private void doRegister() {
                char[] pw = pwField.getPassword();
                char[] pw2 = pw2Field.getPassword();
                String err;
                if (!Arrays.equals(pw, pw2)) {
                    err = "비밀번호 확인이 일치하지 않습니다.";
                } else {
                    err = auth.register(idField.getText().trim(), pw);
                }
                Arrays.fill(pw, '\0');
                Arrays.fill(pw2, '\0');

                if (err == null) {
                    clear();
                    loginPanel.info("회원가입이 완료되었습니다. 로그인해 주세요.", true);
                    showCard("login");
                } else {
                    info(err, false);
                }
            }
        }

        final class HomePanel extends FormPanel {
            final JLabel welcome = new JLabel(" ", SwingConstants.CENTER);
            final JButton logoutButton = new JButton("로그아웃");

            HomePanel() {
                super("환영합니다");
                welcome.setFont(welcome.getFont().deriveFont(Font.PLAIN, 16f));
                addFull(welcome);
                addFull(logoutButton);

                logoutButton.addActionListener(e -> {
                    auth.logout();
                    loginPanel.info("로그아웃되었습니다.", true);
                    showCard("login");
                });
            }

            void setUser(String username) {
                welcome.setText(username + " 님, 로그인되었습니다.");
            }
        }
    }
    private static void setGlobalFont(Font font) {
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(font));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                
            }
            setGlobalFont(new Font(Font.DIALOG, Font.PLAIN, 14));

            Path dbFile = Paths.get("users.db");
            new App(new AuthService(new UserStore(dbFile))).setVisible(true);
        });
    }
}
