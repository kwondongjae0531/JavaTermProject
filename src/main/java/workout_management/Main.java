package workout_management;

import com.formdev.flatlaf.FlatDarkLaf;

import workout_management.domain.Member;
import workout_management.util.AppTheme;
import workout_management.util.SessionUtil;
import workout_management.view.LoginFrame;
import workout_management.view.MainFrame;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            setupTheme();

            try {

                Member member = SessionUtil.restore();

                if (member != null) {
                    showMain(member);
                } else {
                    showLogin();
                }

            } catch (Exception e) {

                System.err.println(
                        "자동 로그인 확인 실패: " + e.getMessage());

                showLogin();
            }
        });
    }

    // 전체 프로그램 디자인 설정

    private static void setupTheme() {

        FlatDarkLaf.setup();

        // 공통 색상

        UIManager.put("Panel.background", AppTheme.BG);

        UIManager.put("Label.foreground", AppTheme.TEXT);

        UIManager.put("Button.background", AppTheme.CARD);
        UIManager.put("Button.foreground", AppTheme.TEXT);

        UIManager.put("TextField.background", AppTheme.CARD);
        UIManager.put("TextField.foreground", AppTheme.TEXT);
        UIManager.put("TextField.caretForeground", AppTheme.TEXT);

        UIManager.put("PasswordField.background", AppTheme.CARD);
        UIManager.put("PasswordField.foreground", AppTheme.TEXT);

        UIManager.put("ComboBox.background", AppTheme.CARD);
        UIManager.put("ComboBox.foreground", AppTheme.TEXT);

        UIManager.put("PopupMenu.background", AppTheme.CARD);
        UIManager.put("MenuItem.background", AppTheme.CARD);
        UIManager.put("MenuItem.foreground", AppTheme.TEXT);

        // 모서리 곡률

        UIManager.put("Button.arc", 24);
        UIManager.put("Component.arc", 24);
        UIManager.put("TextComponent.arc", 24);
        UIManager.put("CheckBox.arc", 8);
        UIManager.put("ProgressBar.arc", 16);

        // 콤보박스

        UIManager.put("ComboBox.buttonStyle", "none");
        UIManager.put("ComboBox.selectionArc", 12);

        UIManager.put(
                "ComboBox.selectionInsets",
                new Insets(2, 4, 2, 4));

        // 스크롤바

        UIManager.put("ScrollBar.width", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.showButtons", false);

        UIManager.put(
                "ScrollBar.thumbInsets",
                new Insets(1, 1, 1, 1));

        // 테두리

        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 0);

        UIManager.put(
                "Component.borderColor",
                AppTheme.LINE);

        UIManager.put(
                "Component.focusedBorderColor",
                new Color(100, 103, 109));

        // Pretendard 공통 폰트 적용

        AppTheme.install();
    }

    // 로그인 화면

    private static void showLogin() {

        LoginFrame[] login = new LoginFrame[1];

        login[0] = new LoginFrame((member, remember) -> {

            try {

                SessionUtil.forget();

                if (remember) {
                    SessionUtil.remember(member);
                }

            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                        login[0],
                        "자동 로그인 설정을 저장하지 못했습니다.",
                        "로그인 확인",
                        JOptionPane.WARNING_MESSAGE);
            }

            if (login[0] != null) {
                login[0].dispose();
            }

            showMain(member);
        });

        login[0].setVisible(true);
    }

    // 메인 화면

    private static void showMain(Member member) {

        MainFrame frame = new MainFrame(member, () -> {

            try {

                SessionUtil.forget();

            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                        null,
                        "자동 로그인 정보를 삭제하지 못했습니다.",
                        "로그아웃 확인",
                        JOptionPane.WARNING_MESSAGE);
            }

            showLogin();
        });

        frame.setVisible(true);
    }
}