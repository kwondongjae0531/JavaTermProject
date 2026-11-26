
package workout_management.view;

import workout_management.controller.AuthController;
import workout_management.domain.Member;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.util.Arrays;
import java.util.function.BiConsumer;

public class LoginFrame extends JFrame {

    private static final Color BG =
            new Color(23, 25, 29);

    private static final Color CARD =
            new Color(39, 42, 47);

    private static final Color TEXT =
            new Color(245, 245, 245);

    private static final Color MUTED =
            workout_management.util.AppTheme.MUTED;

    private final AuthController authController =
            new AuthController();

    private final BiConsumer<Member, Boolean> onLogin;

    private final JTextField idField =
            new JTextField();

    private final JPasswordField passwordField =
            new JPasswordField();

    private final JCheckBox rememberCheck =
            new JCheckBox("자동 로그인");

    public LoginFrame(
            BiConsumer<Member, Boolean> onLogin) {

        this.onLogin = onLogin;

        setTitle("LIFTLOG");

        setSize(440, 560);

        setMinimumSize(
                new Dimension(380, 500));

        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);

        // ========================================
        // 전체 화면
        // ========================================

        JPanel root =
                new JPanel(new GridBagLayout());

        root.setBackground(BG);

        root.setBorder(
                new EmptyBorder(28, 30, 28, 30));

        JPanel content = verticalPanel();

        // ========================================
        // 로고
        // ========================================

        JLabel logo =
                label("LIFTLOG.", 40, TEXT);

        logo.setFont(
                workout_management.util.AppTheme.font(Font.BOLD,
                        40));

        logo.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        content.add(logo);

        content.add(
                Box.createVerticalStrut(8));

        JLabel subtitle =
                label("나의 운동 기록", 13, MUTED);

        subtitle.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        content.add(subtitle);

        content.add(
                Box.createVerticalStrut(38));

        // ========================================
        // 아이디 입력창
        // ========================================

        content.add(
                label("아이디", 13, MUTED));

        content.add(
                Box.createVerticalStrut(7));

        styleField(idField);

        idField.putClientProperty(
                "JTextField.placeholderText",
                "아이디를 입력하세요");

        content.add(idField);

        content.add(
                Box.createVerticalStrut(16));

        // ========================================
        // 비밀번호 입력창
        // ========================================

        content.add(
                label("비밀번호", 13, MUTED));

        content.add(
                Box.createVerticalStrut(7));

        styleField(passwordField);

        passwordField.putClientProperty(
                "JTextField.placeholderText",
                "비밀번호를 입력하세요");

        content.add(passwordField);

        content.add(
                Box.createVerticalStrut(14));

        // ========================================
        // 자동 로그인
        // ========================================

        rememberCheck.setOpaque(false);

        rememberCheck.setForeground(MUTED);

        rememberCheck.setFont(
                workout_management.util.AppTheme.font(Font.PLAIN,
                        13));

        rememberCheck.setSelected(false);

        rememberCheck.setFocusPainted(false);

        rememberCheck.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        rememberCheck.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        content.add(rememberCheck);

        content.add(
                Box.createVerticalStrut(24));

        // ========================================
        // 로그인 버튼
        // ========================================

        JButton loginButton =
                button("로그인", true);

        loginButton.addActionListener(
                e -> login());

        content.add(loginButton);

        content.add(
                Box.createVerticalStrut(14));

        // ========================================
        // 회원가입 버튼
        // ========================================

        JButton registerButton =
                new JButton("회원가입");

        registerButton.setFont(
                workout_management.util.AppTheme.font(Font.PLAIN,
                        13));

        registerButton.setForeground(MUTED);

        registerButton.setContentAreaFilled(false);

        registerButton.setBorderPainted(false);

        registerButton.setFocusPainted(false);

        registerButton.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        registerButton.setAlignmentX(
                Component.CENTER_ALIGNMENT);

        registerButton.addActionListener(
                e -> register());

        content.add(registerButton);

        // ========================================
        // 화면 중앙 정렬
        // ========================================

        GridBagConstraints gbc =
                new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;

        gbc.weightx = 1;

        gbc.fill =
                GridBagConstraints.HORIZONTAL;

        gbc.anchor =
                GridBagConstraints.CENTER;

        root.add(content, gbc);

        setContentPane(root);

        // Enter 키 로그인

        getRootPane().setDefaultButton(loginButton);

        idField.addActionListener(
                e -> passwordField.requestFocusInWindow());

        passwordField.addActionListener(
                e -> login());

        SwingUtilities.invokeLater(
                idField::requestFocusInWindow);
    }

    // ========================================
    // 로그인
    // ========================================

    private void login() {

        String loginId =
                idField.getText().trim();

        char[] password =
                passwordField.getPassword();

        Member member;

        try {

            if (loginId.isEmpty()) {

                throw new IllegalArgumentException(
                        "아이디를 입력하세요.");
            }

            if (password.length == 0) {

                throw new IllegalArgumentException(
                        "비밀번호를 입력하세요.");
            }

            member = authController.login(
                    loginId, password);

            if (member == null) {

                throw new IllegalArgumentException(
                        "아이디 또는 비밀번호가 일치하지 않습니다.");
            }

        } catch (IllegalArgumentException e) {

            showWarning(e.getMessage());

            passwordField.setText("");

            passwordField.requestFocusInWindow();

            return;

        } catch (Exception e) {

            showError("로그인 처리 중 오류가 발생했습니다.");

            return;

        } finally {

            Arrays.fill(password, '\0');
        }

        boolean remember =
                rememberCheck.isSelected();

        passwordField.setText("");

        dispose();

        onLogin.accept(member, remember);
    }

    // ========================================
    // 회원가입
    // ========================================

    private void register() {

        JTextField newIdField =
                new JTextField(idField.getText().trim());

        JPasswordField newPasswordField =
                new JPasswordField();

        JPasswordField confirmPasswordField =
                new JPasswordField();

        styleField(newIdField);

        styleField(newPasswordField);

        styleField(confirmPasswordField);

        newPasswordField.putClientProperty(
                "JTextField.placeholderText",
                "8자 이상 입력");

        JPanel form = verticalPanel();

        form.setBorder(
                new EmptyBorder(8, 6, 8, 6));

        form.add(
                label("아이디", 13, MUTED));

        form.add(
                Box.createVerticalStrut(6));

        form.add(newIdField);

        form.add(
                Box.createVerticalStrut(14));

        form.add(
                label("비밀번호", 13, MUTED));

        form.add(
                Box.createVerticalStrut(6));

        form.add(newPasswordField);

        form.add(
                Box.createVerticalStrut(14));

        form.add(
                label("비밀번호 확인", 13, MUTED));

        form.add(
                Box.createVerticalStrut(6));

        form.add(confirmPasswordField);

        while (true) {

            int result =
                    JOptionPane.showConfirmDialog(

                            this,

                            form,

                            "회원가입",

                            JOptionPane.OK_CANCEL_OPTION,

                            JOptionPane.PLAIN_MESSAGE
                    );

            if (result != JOptionPane.OK_OPTION) {

                newPasswordField.setText("");

                confirmPasswordField.setText("");

                return;
            }

            String loginId =
                    newIdField.getText().trim();

            char[] password =
                    newPasswordField.getPassword();

            char[] confirm =
                    confirmPasswordField.getPassword();

            try {

                if (loginId.isEmpty()) {

                    throw new IllegalArgumentException(
                            "아이디를 입력하세요.");
                }

                if (password.length < 8) {

                    throw new IllegalArgumentException(
                            "비밀번호는 8자 이상 입력하세요.");
                }

                if (!Arrays.equals(password, confirm)) {

                    throw new IllegalArgumentException(
                            "비밀번호가 일치하지 않습니다.");
                }

                authController.register(
                        loginId, password);

                // 가입한 아이디를 로그인 화면에 표시

                idField.setText(loginId);

                passwordField.setText("");

                newPasswordField.setText("");

                confirmPasswordField.setText("");

                JOptionPane.showMessageDialog(

                        this,

                        "회원가입이 완료되었습니다.",

                        "회원가입",

                        JOptionPane.INFORMATION_MESSAGE
                );

                passwordField.requestFocusInWindow();

                return;

            } catch (IllegalArgumentException e) {

                showWarning(e.getMessage());

            } catch (java.sql.SQLException e) {

                if (e.getErrorCode() == 1) {

                    showWarning(
                            "이미 사용 중인 아이디입니다.");

                } else {

                    showError(
                            "회원가입 처리 중 DB 오류가 발생했습니다.");
                }

            } catch (Exception e) {

                showError(
                        "회원가입 처리 중 오류가 발생했습니다.");

            } finally {

                Arrays.fill(password, '\0');

                Arrays.fill(confirm, '\0');

                newPasswordField.setText("");

                confirmPasswordField.setText("");
            }
        }
    }

    // ========================================
    // 공통 둥근 입력창
    // ========================================

    private static void styleField(
            JTextField field) {

        field.setBackground(CARD);

        field.setForeground(TEXT);

        field.setCaretColor(TEXT);

        field.setFont(
                workout_management.util.AppTheme.font(Font.PLAIN,
                        14));

        field.setMargin(
                new Insets(10, 15, 10, 15));

        field.setPreferredSize(
                new Dimension(300, 46));

        field.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE, 46));

        field.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        field.putClientProperty(
                "JComponent.roundRect", true);
    }

    // ========================================
    // 공통 버튼
    // ========================================

    private static JButton button(
            String text, boolean primary) {

        JButton button = new JButton(text);

        button.setBackground(
                primary ? TEXT : CARD);

        button.setForeground(
                primary ? BG : TEXT);

        button.setFont(
                workout_management.util.AppTheme.font(Font.BOLD,
                        15));

        button.setFocusPainted(false);

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        button.setPreferredSize(
                new Dimension(300, 46));

        button.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE, 46));

        button.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        button.putClientProperty(
                "JButton.buttonType", "roundRect");

        return button;
    }

    // ========================================
    // 공통 패널
    // ========================================

    private static JPanel verticalPanel() {

        JPanel panel = new JPanel();

        panel.setOpaque(false);

        panel.setLayout(
                new BoxLayout(
                        panel, BoxLayout.Y_AXIS));

        panel.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        return panel;
    }

    // ========================================
    // 공통 라벨
    // ========================================

    private static JLabel label(
            String text, int size, Color color) {

        JLabel label = new JLabel(text);

        label.setFont(
                workout_management.util.AppTheme.font(Font.PLAIN,
                        size));

        label.setForeground(color);

        label.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        return label;
    }

    // ========================================
    // 오류 메시지
    // ========================================

    private void showWarning(String message) {

        JOptionPane.showMessageDialog(

                this,

                message,

                "입력 확인",

                JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(

                this,

                message,

                "오류",

                JOptionPane.ERROR_MESSAGE
        );
    }
}
