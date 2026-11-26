package workout_management.view;

import workout_management.domain.WorkoutRecord;
import workout_management.domain.SetRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class ComparisonDialog extends JDialog {

    private static final Color BG = new Color(23, 25, 29);
    private static final Color CARD = new Color(39, 42, 47);
    private static final Color FIELD = new Color(51, 54, 59);
    private static final Color TEXT = new Color(245, 245, 245);
    private static final Color MUTED = workout_management.util.AppTheme.MUTED;
    private static final Color GREEN = new Color(145, 214, 172);
    private static final Color RED = new Color(230, 160, 160);

    private final boolean bodyweight;

    public ComparisonDialog(
            JFrame owner,
            String exerciseName,
            WorkoutRecord previousRecord,
            List<SetRecord> previousSets,
            WorkoutRecord currentRecord,
            List<SetRecord> currentSets) {

        super(owner, "운동 비교", true);

        bodyweight = WorkoutDayDialog.isBodyweight(exerciseName);

        setSize(460, 700);
        setMinimumSize(new Dimension(400, 480));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(20, 18, 18, 18));

        JPanel top = verticalPanel();

        top.add(label("WORKOUT COMPARISON", 11, MUTED));
        top.add(Box.createVerticalStrut(8));
        top.add(bold(exerciseName, 26, TEXT));
        top.add(Box.createVerticalStrut(8));

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yy.MM.dd");

        top.add(label(
                previousRecord.getWorkoutDate().format(formatter)
                        + "  →  "
                        + currentRecord.getWorkoutDate().format(formatter),
                13, MUTED));

        if (currentSets.isEmpty()) {
            top.add(Box.createVerticalStrut(10));
            top.add(label("아직 저장된 세트가 없습니다.", 12, MUTED));
        }

        root.add(top, BorderLayout.NORTH);

        JPanel content = verticalPanel();

        content.add(createSummary(previousSets, currentSets));
        content.add(Box.createVerticalStrut(22));

        content.add(bold("세트별 비교", 18, TEXT));
        content.add(Box.createVerticalStrut(12));

        JPanel header = new JPanel(new GridLayout(1, 3, 8, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(new EmptyBorder(0, 12, 0, 12));

        header.add(centered("SET", 11, MUTED));
        header.add(centered("이전 운동", 12, MUTED));
        header.add(centered("이번 운동", 12, MUTED));

        content.add(header);
        content.add(Box.createVerticalStrut(8));

        Map<Integer, SetRecord> previousMap =
                new TreeMap<>();

        Map<Integer, SetRecord> currentMap =
                new TreeMap<>();

        for (SetRecord set : previousSets) {
            previousMap.put(set.getSetNo(), set);
        }

        for (SetRecord set : currentSets) {
            currentMap.put(set.getSetNo(), set);
        }

        TreeSet<Integer> numbers = new TreeSet<>();

        numbers.addAll(previousMap.keySet());
        numbers.addAll(currentMap.keySet());

        for (Integer number : numbers) {

            content.add(createSetCard(
                    number,
                    previousMap.get(number),
                    currentMap.get(number)));

            content.add(Box.createVerticalStrut(8));
        }

        if (numbers.isEmpty()) {

            content.add(label(
                    "비교할 세트 기록이 없습니다.", 13, MUTED));
        }

        content.add(Box.createVerticalStrut(8));

        JLabel note = label(
                "<html>중량과 횟수를 각각 비교합니다.<br>"
                        + "운동량 증가는 실제 근력 향상과 다를 수 있습니다."
                        + "</html>",
                11, MUTED);

        content.add(note);

        JPanel scrollContent = new JPanel(new BorderLayout());
        scrollContent.setBackground(BG);
        scrollContent.setBorder(new EmptyBorder(0, 0, 0, 4));
        scrollContent.add(content, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.getVerticalScrollBar().setUnitIncrement(20);

        scroll.getVerticalScrollBar().setPreferredSize(
                new Dimension(6, 0));

        root.add(scroll, BorderLayout.CENTER);

        JButton close = button("닫기", true);
        close.setPreferredSize(new Dimension(300, 48));
        close.addActionListener(e -> dispose());

        root.add(close, BorderLayout.SOUTH);

        setContentPane(root);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // 운동 요약

    private JPanel createSummary(
            List<SetRecord> previousSets,
            List<SetRecord> currentSets) {

        JPanel summary = verticalPanel();

        BigDecimal previousMax = maxWeight(previousSets);
        BigDecimal currentMax = maxWeight(currentSets);

        long previousReps = totalReps(previousSets);
        long currentReps = totalReps(currentSets);

        boolean comparable =
                !previousSets.isEmpty() && !currentSets.isEmpty();

        JPanel metrics = new JPanel(new GridLayout(1, 2, 8, 0));
        metrics.setOpaque(false);
        metrics.setAlignmentX(Component.LEFT_ALIGNMENT);

        String weightTitle = bodyweight
                ? "최고 추가 중량" : "최고 중량";

        metrics.add(createMetric(
                weightTitle,
                currentSets.isEmpty()
                        ? "—" : displayWeight(currentMax),
                previousSets.isEmpty()
                        ? "—" : displayWeight(previousMax),
                currentMax.subtract(previousMax),
                " kg",
                comparable));

        metrics.add(createMetric(
                "총 반복 횟수",
                currentSets.isEmpty()
                        ? "—" : currentReps + "회",
                previousSets.isEmpty()
                        ? "—" : previousReps + "회",
                BigDecimal.valueOf(currentReps - previousReps),
                "회",
                comparable));

        summary.add(metrics);
        summary.add(Box.createVerticalStrut(8));

        RoundedPanel details = new RoundedPanel(CARD, workout_management.util.AppTheme.CARD_ARC);

        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setBorder(new EmptyBorder(14, 16, 14, 16));

        details.add(summaryRow(
                "완료 세트",
                previousSets.size() + " → " + currentSets.size() + "세트"));

        if (!bodyweight) {

            BigDecimal previousVolume =
                    totalVolume(previousSets);

            BigDecimal currentVolume =
                    totalVolume(currentSets);

            details.add(Box.createVerticalStrut(12));

            details.add(summaryRow(
                    "이전 운동량",
                    previousSets.isEmpty()
                            ? "—"
                            : formatNumber(previousVolume) + " kg"));

            details.add(Box.createVerticalStrut(8));

            details.add(summaryRow(
                    "이번 운동량",
                    currentSets.isEmpty()
                            ? "—"
                            : formatNumber(currentVolume) + " kg"));

            if (comparable) {

                BigDecimal difference =
                        currentVolume.subtract(previousVolume);

                details.add(Box.createVerticalStrut(8));

                JPanel changeRow =
                        new JPanel(new BorderLayout());

                changeRow.setOpaque(false);
                changeRow.setAlignmentX(Component.LEFT_ALIGNMENT);

                changeRow.add(
                        label("운동량 변화", 12, MUTED),
                        BorderLayout.WEST);

                changeRow.add(
                        bold(formatChange(difference, " kg"),
                                13, changeColor(difference)),
                        BorderLayout.EAST);

                details.add(changeRow);
            }
        }

        summary.add(details);

        return summary;
    }

    private JPanel createMetric(
            String title,
            String current,
            String previous,
            BigDecimal difference,
            String unit,
            boolean comparable) {

        RoundedPanel card = new RoundedPanel(CARD, workout_management.util.AppTheme.CARD_ARC);

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        card.add(label(title, 12, MUTED));
        card.add(Box.createVerticalStrut(10));

        card.add(bold(
                current,
                current.length() > 8 ? 17 : 25,
                TEXT));

        card.add(Box.createVerticalStrut(6));
        card.add(label("이전 " + previous, 11, MUTED));
        card.add(Box.createVerticalStrut(6));

        String change = comparable
                ? formatChange(difference, unit)
                : "비교할 기록 부족";

        card.add(bold(
                change,
                13,
                comparable ? changeColor(difference) : MUTED));

        return card;
    }

    private JPanel summaryRow(String title, String value) {

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(label(title, 12, MUTED), BorderLayout.WEST);
        row.add(bold(value, 13, TEXT), BorderLayout.EAST);

        return row;
    }

    // 세트별 비교 카드

    private JPanel createSetCard(
            int number,
            SetRecord previous,
            SetRecord current) {

        RoundedPanel card = new RoundedPanel(CARD, workout_management.util.AppTheme.CARD_ARC);

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 10, 12, 10));

        JPanel values = new JPanel(new GridBagLayout());

        values.setOpaque(false);
        values.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints c = new GridBagConstraints();

        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 4, 0, 4);

        c.gridx = 0;
        c.weightx = 0;

        JLabel setNumber =
                centered(String.valueOf(number), 14, MUTED);

        setNumber.setPreferredSize(new Dimension(42, 45));

        values.add(setNumber, c);

        c.gridx = 1;
        c.weightx = 1;

        values.add(createSetValue(previous, false), c);

        c.gridx = 2;

        values.add(createSetValue(current, true), c);

        card.add(values);

        if (previous != null && current != null) {

            BigDecimal weightDifference =
                    current.getWeightKg()
                            .subtract(previous.getWeightKg());

            int repsDifference =
                    current.getReps() - previous.getReps();

            JPanel changes =
                    new JPanel(new GridLayout(1, 2, 6, 0));

            changes.setOpaque(false);
            changes.setAlignmentX(Component.LEFT_ALIGNMENT);

            String weightText = bodyweight
                    ? "추가 중량 " : "중량 ";

            changes.add(centered(
                    weightText + formatChange(weightDifference, " kg"),
                    11, changeColor(weightDifference)));

            BigDecimal repChange =
                    BigDecimal.valueOf(repsDifference);

            changes.add(centered(
                    "횟수 " + formatChange(repChange, "회"),
                    11, changeColor(repChange)));

            card.add(Box.createVerticalStrut(8));
            card.add(changes);

        } else {

            card.add(Box.createVerticalStrut(6));

            JLabel status = centered(
                    previous == null
                            ? "새로 추가한 세트"
                            : "이번 운동에 저장되지 않은 세트",
                    11, MUTED);

            status.setAlignmentX(Component.LEFT_ALIGNMENT);

            card.add(status);
        }

        return card;
    }

    private JPanel createSetValue(
            SetRecord set, boolean current) {

        RoundedPanel panel =
                new RoundedPanel(current ? FIELD : CARD, 16);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.setBorder(
                new EmptyBorder(10, 4, 10, 4));

        if (set == null) {

            JLabel empty = label("—", 16, MUTED);

            empty.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(empty);

            return panel;
        }

        JLabel weight = bold(
                displayWeight(set.getWeightKg()), 15, TEXT);

        weight.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel reps = label(
                set.getReps() + "회", 13, TEXT);

        reps.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(weight);
        panel.add(Box.createVerticalStrut(4));
        panel.add(reps);

        if (set.getRestSeconds() != null) {

            int rest = set.getRestSeconds();

            JLabel restLabel = label(
                    "휴식 " + String.format(
                            "%d:%02d", rest / 60, rest % 60),
                    11, MUTED);

            restLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(Box.createVerticalStrut(5));
            panel.add(restLabel);
        }

        return panel;
    }

    // 운동 기록 계산

    private BigDecimal maxWeight(List<SetRecord> sets) {

        BigDecimal max = BigDecimal.ZERO;

        for (SetRecord set : sets) {
            max = max.max(set.getWeightKg());
        }

        return max;
    }

    private long totalReps(List<SetRecord> sets) {

        long total = 0;

        for (SetRecord set : sets) {
            total += set.getReps();
        }

        return total;
    }

    private BigDecimal totalVolume(List<SetRecord> sets) {

        BigDecimal total = BigDecimal.ZERO;

        for (SetRecord set : sets) {

            total = total.add(
                    set.getWeightKg().multiply(
                            BigDecimal.valueOf(set.getReps())));
        }

        return total;
    }

    private String displayWeight(BigDecimal value) {

        if (bodyweight) {

            if (value.signum() == 0) return "맨몸";

            return "+" + formatNumber(value) + " kg";
        }

        return formatNumber(value) + " kg";
    }

    private static String formatNumber(BigDecimal number) {

        return new DecimalFormat("#,##0.##").format(number);
    }

    private static String formatChange(
            BigDecimal difference, String unit) {

        if (difference.signum() == 0) return "변화 없음";

        return (difference.signum() > 0 ? "+" : "")
                + formatNumber(difference) + unit;
    }

    private static Color changeColor(BigDecimal difference) {

        if (difference.signum() > 0) return GREEN;
        if (difference.signum() < 0) return RED;

        return MUTED;
    }

    // 공통 디자인

    private static JPanel verticalPanel() {

        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return panel;
    }

    private static JLabel label(
            String text, int size, Color color) {

        JLabel label = new JLabel(text);

        label.setForeground(color);
        label.setFont(workout_management.util.AppTheme.font(Font.PLAIN, size));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private static JLabel bold(
            String text, int size, Color color) {

        JLabel label = label(text, size, color);

        label.setFont(workout_management.util.AppTheme.font(Font.BOLD, size));

        return label;
    }

    private static JLabel centered(
            String text, int size, Color color) {

        JLabel label = label(text, size, color);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        return label;
    }

    private static JButton button(String text, boolean primary) {

        JButton button = new JButton(text);

        button.setBackground(primary ? TEXT : FIELD);
        button.setForeground(primary ? BG : TEXT);
        button.setFont(workout_management.util.AppTheme.font(Font.BOLD, 14));
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.putClientProperty(
                "JButton.buttonType", "roundRect");

        return button;
    }

    private static class RoundedPanel extends JPanel {

        private final Color color;
        private final int arc;

        RoundedPanel(Color color, int arc) {

            this.color = color;
            this.arc = arc;

            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(color);

            g.fillRoundRect(
                    0, 0, getWidth(), getHeight(), arc, arc);

            g.dispose();

            super.paintComponent(graphics);
        }

        @Override
        public Dimension getMaximumSize() {

            return new Dimension(
                    Integer.MAX_VALUE,
                    getPreferredSize().height);
        }
    }
}