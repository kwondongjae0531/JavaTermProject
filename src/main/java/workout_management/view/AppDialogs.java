
package workout_management.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public final class AppDialogs {

    private static final Color CARD = new Color(39, 42, 47);
    private static final Color FIELD = new Color(51, 54, 59);
    private static final Color TEXT = new Color(245, 245, 245);
    private static final Color MUTED = workout_management.util.AppTheme.MUTED;
    private static final Color RED = new Color(240, 167, 167);
    private static final Color RED_BG = new Color(73, 50, 55);

    private AppDialogs() {
    }

    public static boolean confirm(
            Component parent,
            String title,
            String message,
            String cancelText,
            String confirmText,
            boolean destructive) {

        Window owner = parent instanceof Window
                ? (Window) parent
                : SwingUtilities.getWindowAncestor(parent);

        JDialog dialog = new JDialog(
                owner,
                title,
                Dialog.ModalityType.APPLICATION_MODAL);

        dialog.setUndecorated(true);
        dialog.setResizable(false);

        dialog.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE);

        boolean transparent = dialog
                .getGraphicsConfiguration()
                .getDevice()
                .isWindowTranslucencySupported(
                        GraphicsDevice.WindowTranslucency
                                .PERPIXEL_TRANSLUCENT);

        if (transparent) {
            dialog.setBackground(new Color(0, 0, 0, 0));
        }

        boolean[] confirmed = {false};

        RoundedPanel root = new RoundedPanel();

        root.setOpaque(!transparent);
        root.setBackground(CARD);

        root.setLayout(
                new BoxLayout(root, BoxLayout.Y_AXIS));

        root.setBorder(
                new EmptyBorder(24, 20, 20, 20));

        // 제목

        JLabel titleLabel = new JLabel(title);

        titleLabel.setForeground(TEXT);

        titleLabel.setFont(
                workout_management.util.AppTheme.font(Font.BOLD, 20));

        titleLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(12));

        // 설명

        String safeMessage = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");

        JLabel messageLabel = new JLabel(
                "<html><body style='width:260px;'>"
                        + safeMessage
                        + "</body></html>");

        messageLabel.setForeground(MUTED);

        messageLabel.setFont(
                workout_management.util.AppTheme.font(Font.PLAIN, 13));

        messageLabel.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        root.add(messageLabel);
        root.add(Box.createVerticalStrut(24));

        // 하단 버튼

        JPanel actions = new JPanel(
                new GridLayout(1, 2, 8, 0));

        actions.setOpaque(false);

        actions.setAlignmentX(
                Component.LEFT_ALIGNMENT);

        actions.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 44));

        JButton cancelButton =
                button(cancelText, FIELD, TEXT);

        JButton confirmButton = button(
                confirmText,
                destructive ? RED_BG : TEXT,
                destructive ? RED : CARD);

        cancelButton.addActionListener(
                e -> dialog.dispose());

        confirmButton.addActionListener(e -> {

            confirmed[0] = true;
            dialog.dispose();
        });

        actions.add(cancelButton);
        actions.add(confirmButton);

        root.add(actions);

        dialog.setContentPane(root);

        // ESC 키로 취소

        root.getInputMap(
                        JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(
                        KeyStroke.getKeyStroke("ESCAPE"),
                        "cancel");

        root.getActionMap().put(
                "cancel",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                });

        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        SwingUtilities.invokeLater(
                cancelButton::requestFocusInWindow);

        dialog.setVisible(true);

        return confirmed[0];
    }

    private static JButton button(
            String text,
            Color background,
            Color foreground) {

        JButton button = new JButton(text);

        button.setBackground(background);
        button.setForeground(foreground);

        button.setFont(
                workout_management.util.AppTheme.font(Font.BOLD, 13));

        button.setPreferredSize(
                new Dimension(130, 44));

        button.setBorderPainted(false);
        button.setFocusPainted(false);

        button.setCursor(
                new Cursor(Cursor.HAND_CURSOR));

        button.putClientProperty(
                "JButton.buttonType", "roundRect");

        return button;
    }

    private static class RoundedPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g =
                    (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(CARD);

            g.fillRoundRect(
                    0, 0,
                    getWidth(), getHeight(),
                    30, 30);

            g.dispose();
        }
    }
}
