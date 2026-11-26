
package workout_management.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class NavigationIcon implements Icon {

    private final String type;

    public NavigationIcon(String type) {
        this.type = type;
    }

    @Override
    public int getIconWidth() {
        return 24;
    }

    @Override
    public int getIconHeight() {
        return 24;
    }

    @Override
    public void paintIcon(
            Component c, Graphics graphics,
            int x, int y) {

        Graphics2D g =
                (Graphics2D) graphics.create();

        g.translate(x, y);

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(c.getForeground());

        g.setStroke(new BasicStroke(
                1.7f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        switch (type) {

            // ========================================
            // 홈 아이콘
            // ========================================

            case "HOME" -> {

                Path2D roof = new Path2D.Double();

                roof.moveTo(3, 10);
                roof.lineTo(12, 3);
                roof.lineTo(21, 10);

                g.draw(roof);

                g.drawLine(5, 9, 5, 21);
                g.drawLine(19, 9, 19, 21);

                g.drawLine(5, 21, 10, 21);
                g.drawLine(14, 21, 19, 21);

                g.drawRoundRect(
                        10, 14, 4, 7, 1, 1);
            }

            // ========================================
            // 운동 아이콘
            // ========================================

            case "WORKOUT" -> {

                g.drawRoundRect(
                        3, 6, 4, 12, 2, 2);

                g.drawRoundRect(
                        17, 6, 4, 12, 2, 2);

                g.drawLine(7, 10, 17, 10);
                g.drawLine(7, 14, 17, 14);

                g.drawLine(1, 10, 1, 14);
                g.drawLine(23, 10, 23, 14);
            }

            // ========================================
            // 기록 아이콘
            // ========================================

            case "HISTORY" -> {

                g.drawRoundRect(
                        3, 5, 18, 16, 4, 4);

                g.drawLine(7, 2, 7, 8);
                g.drawLine(17, 2, 17, 8);

                g.drawLine(3, 10, 21, 10);

                g.fillOval(7, 13, 2, 2);
                g.fillOval(11, 13, 2, 2);
                g.fillOval(15, 13, 2, 2);

                g.fillOval(7, 17, 2, 2);
                g.fillOval(11, 17, 2, 2);
            }

            // ========================================
            // 통계 아이콘
            // ========================================

            case "STATS" -> {

                g.drawLine(3, 3, 3, 21);
                g.drawLine(3, 21, 22, 21);

                Path2D graph = new Path2D.Double();

                graph.moveTo(7, 16);
                graph.lineTo(11, 11);
                graph.lineTo(15, 14);
                graph.lineTo(21, 5);

                g.draw(graph);
            }

            // ========================================
            // 프로필 아이콘
            // ========================================

            case "PROFILE" -> {

                g.drawOval(8, 3, 8, 8);

                g.drawRoundRect(
                        4, 14, 16, 8, 8, 8);
            }
        }

        g.dispose();
    }
}
