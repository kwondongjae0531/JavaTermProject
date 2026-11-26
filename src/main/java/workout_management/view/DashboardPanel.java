package workout_management.view;

import workout_management.controller.WorkoutController;
import workout_management.domain.*;
import workout_management.util.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static workout_management.util.AppTheme.*;

public class DashboardPanel extends JPanel {

    private final WorkoutController controller;
    private final Runnable onStart;
    private final Consumer<WorkoutRecord> onOpen;

    private final CalendarField dateField =
            new CalendarField(LocalDate.now(), false);

    private final JLabel dateTitle = bold("", 26, TEXT);
    private final JLabel dayLabel = label("", 12, MUTED);

    private final JLabel exerciseCount = bold("0", 23, TEXT);
    private final JLabel setCount = bold("0", 23, TEXT);
    private final JLabel volumeCount = bold("0", 23, TEXT);

    private final JLabel historyTitle =
            bold("오늘의 운동", 18, TEXT);

    private final JButton startButton =
            button("운동 기록하기", true);

    private final JPanel cards = vertical();

    private boolean refreshQueued;

    private record Entry(
            WorkoutRecord record,
            Exercise exercise,
            List<SetRecord> sets) {}

    public DashboardPanel(
            WorkoutController controller,
            Runnable onStart,
            Runnable onHistory,
            Runnable onAddExercise,
            Consumer<WorkoutRecord> onOpen) {

        super(new BorderLayout(0, 16));

        this.controller = controller;
        this.onStart = onStart;
        this.onOpen = onOpen;

        setBackground(BG);

        JPanel top = vertical();

        // 날짜 선택

        JPanel dateRow = new JPanel(new BorderLayout(8, 0));

        dateRow.setOpaque(false);

        JPanel dateText = vertical();

        dateText.add(dayLabel);
        dateText.add(Box.createVerticalStrut(5));
        dateText.add(dateTitle);

        JButton calendar = button("", false);

        calendar.setIcon(new NavigationIcon("HISTORY"));

        calendar.setPreferredSize(new Dimension(44, 44));

        calendar.setToolTipText("운동 날짜 선택");

        calendar.getAccessibleContext()
                .setAccessibleName("운동 날짜 선택");

        calendar.addActionListener(e -> openCalendar());

        JPanel dateActions = new JPanel(new BorderLayout());

        dateActions.setOpaque(false);

        // 달력 다이얼로그 연결 유지

        dateField.setVisible(false);

        dateActions.add(calendar, BorderLayout.CENTER);
        dateActions.add(dateField, BorderLayout.SOUTH);

        dateRow.add(dateText, BorderLayout.CENTER);
        dateRow.add(dateActions, BorderLayout.EAST);

        top.add(dateRow);
        top.add(Box.createVerticalStrut(14));

        // 운동 요약

        RoundedPanel summary = new RoundedPanel(CARD, CARD_ARC);

        summary.setLayout(new GridLayout(1, 3, 8, 0));

        summary.setBorder(
                new EmptyBorder(14, 16, 14, 16));

        summary.add(metric("종목", exerciseCount));
        summary.add(metric("세트", setCount));
        summary.add(metric("운동량(kg)", volumeCount));

        volumeCount.setToolTipText(
                "입력 중량 × 반복 횟수의 합계");

        top.add(summary);
        top.add(Box.createVerticalStrut(10));

        // 운동 시작 및 이어서 기록

        fullWidth(startButton, 48);

        startButton.addActionListener(e -> onStart.run());

        top.add(startButton);
        top.add(Box.createVerticalStrut(20));

        top.add(historyTitle);

        alignLeft(top);

        add(top, BorderLayout.NORTH);

        // 운동 목록

        JPanel scrollContent = new JPanel(new BorderLayout());

        scrollContent.setBackground(BG);

        scrollContent.add(cards, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(scrollContent);

        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.getVerticalScrollBar().setUnitIncrement(20);

        add(scroll, BorderLayout.CENTER);

        // 날짜 변경 시 자동 갱신

        dateField.getDocument().addDocumentListener(
                new DocumentListener() {

                    public void insertUpdate(DocumentEvent e) {
                        refreshLater();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        refreshLater();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        refreshLater();
                    }
                });

        runAction(this::refresh);
    }

    private void openCalendar() {

        Action action = dateField.getActionMap().get("calendar");

        if (action != null) {

            action.actionPerformed(new ActionEvent(
                    dateField,
                    ActionEvent.ACTION_PERFORMED,
                    "calendar"));
        }
    }

    public LocalDate getDate() {

        LocalDate date = dateField.getDate();

        return date == null ? LocalDate.now() : date;
    }

    public void showToday() {

        if (!getDate().equals(LocalDate.now()))
            dateField.setDate(LocalDate.now());
    }

    private void refreshLater() {

        if (refreshQueued) return;

        refreshQueued = true;

        SwingUtilities.invokeLater(() -> {

            refreshQueued = false;

            runAction(this::refresh);
        });
    }

    // 운동 기록 조회

    public void refresh() throws SQLException {

        LocalDate date = getDate();

        dayLabel.setText(date.format(
                DateTimeFormatter.ofPattern(
                        "yyyy년 · EEEE", Locale.KOREAN)));

        dateTitle.setText(date.format(
                DateTimeFormatter.ofPattern("M월 d일")));

        historyTitle.setText(
                date.equals(LocalDate.now())
                        ? "오늘의 운동" : "운동 기록");

        Map<Long, Exercise> exerciseMap = new HashMap<>();

        for (Exercise exercise : controller.getExercises()) {

            exerciseMap.put(
                    exercise.getExerciseId(), exercise);
        }

        List<WorkoutRecord> records = controller.getRecords()
                .stream()
                .filter(record ->
                        date.equals(record.getWorkoutDate()))
                .sorted(Comparator.comparingLong(
                        WorkoutRecord::getRecordId))
                .toList();

        Map<String, List<Entry>> groups = new LinkedHashMap<>();

        int totalExercises = 0;
        int totalSets = 0;

        BigDecimal totalVolume = BigDecimal.ZERO;

        for (WorkoutRecord record : records) {

            Exercise exercise =
                    exerciseMap.get(record.getExerciseId());

            if (exercise == null) continue;

            List<SetRecord> sets =
                    controller.getSets(record.getRecordId());

            if (!sets.isEmpty()) {

                totalExercises++;
                totalSets += sets.size();
            }

            for (SetRecord set : sets) {

                totalVolume = totalVolume.add(
                        set.getWeightKg().multiply(
                                BigDecimal.valueOf(set.getReps())));
            }

            groups.computeIfAbsent(
                            exercise.getBodyPart(),
                            key -> new ArrayList<>())
                    .add(new Entry(record, exercise, sets));
        }

        setMetric(exerciseCount,
                String.valueOf(totalExercises));

        setMetric(setCount,
                String.valueOf(totalSets));

        setMetric(volumeCount,
                formatNumber(totalVolume));

        if (totalSets > 0) {

            startButton.setText(
                    date.equals(LocalDate.now())
                            ? "운동 이어서 기록"
                            : "운동 기록 수정");

        } else {

            startButton.setText("운동 기록하기");
        }

        cards.removeAll();

        if (groups.isEmpty()) {

            cards.add(createEmptyCard());

        } else {

            for (var group : groups.entrySet()) {

                cards.add(createGroupCard(
                        group.getKey(), group.getValue()));

                cards.add(Box.createVerticalStrut(10));
            }
        }

        alignLeft(cards);

        cards.revalidate();
        cards.repaint();

        revalidate();
        repaint();
    }

    // 부위별 운동 카드

    private JPanel createGroupCard(
            String part, List<Entry> entries) {

        RoundedPanel card = new RoundedPanel(CARD, CARD_ARC);

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.setBorder(new EmptyBorder(12, 14, 8, 14));

        int totalSets = entries.stream()
                .mapToInt(entry -> entry.sets().size())
                .sum();

        JPanel heading = new JPanel(new BorderLayout());

        heading.setOpaque(false);

        heading.add(bold(part, 15, TEXT),
                BorderLayout.WEST);

        heading.add(label(totalSets + "세트", 12, MUTED),
                BorderLayout.EAST);

        fullWidth(heading, 26);

        card.add(heading);
        card.add(Box.createVerticalStrut(5));
        card.add(divider());

        for (int i = 0; i < entries.size(); i++) {

            card.add(createExerciseRow(entries.get(i)));

            if (i < entries.size() - 1)
                card.add(divider());
        }

        alignLeft(card);

        return card;
    }

    private JButton createExerciseRow(Entry entry) {

        JButton row = new JButton();

        row.setLayout(new BorderLayout(8, 0));

        row.setOpaque(false);
        row.setContentAreaFilled(false);
        row.setBorderPainted(false);

        row.setBorder(new EmptyBorder(0, 0, 0, 0));

        row.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        JLabel name = bold(
                entry.exercise().getExerciseName(),
                14, TEXT);

        name.setToolTipText(
                entry.exercise().getExerciseName());

        JPanel right = new JPanel();

        right.setLayout(
                new BoxLayout(right, BoxLayout.X_AXIS));

        right.setOpaque(false);

        JLabel result = label(
                representativeSet(entry), 12, MUTED);

        JLabel arrow = label("›", 20, MUTED);

        result.setAlignmentY(Component.CENTER_ALIGNMENT);
        arrow.setAlignmentY(Component.CENTER_ALIGNMENT);

        right.add(result);
        right.add(Box.createHorizontalStrut(8));
        right.add(arrow);

        row.add(name, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);

        row.getAccessibleContext().setAccessibleName(
                entry.exercise().getExerciseName()
                        + " 상세 기록 보기");

        row.addActionListener(e ->
                onOpen.accept(entry.record()));

        fullWidth(row, 44);

        return row;
    }

    // 대표 세트: 최고 중량, 동일 중량이면 가장 많은 횟수

    private String representativeSet(Entry entry) {

        if (entry.sets().isEmpty()) return "미기록";

        boolean bodyweight = WorkoutDayDialog.isBodyweight(
                entry.exercise().getExerciseName());

        SetRecord best = entry.sets().stream()
                .max(Comparator
                        .comparing(SetRecord::getWeightKg)
                        .thenComparingInt(SetRecord::getReps))
                .orElseThrow();

        String weight = WorkoutDayDialog.formatWeight(
                best.getWeightKg(), bodyweight);

        String unit = bodyweight
                && best.getWeightKg().signum() == 0
                ? "" : "kg";

        return weight + unit + " × "
                + best.getReps() + "회";
    }

    private JPanel createEmptyCard() {

        RoundedPanel empty =
                new RoundedPanel(CARD, CARD_ARC);

        empty.setLayout(
                new BoxLayout(empty, BoxLayout.Y_AXIS));

        empty.setBorder(
                new EmptyBorder(30, 12, 30, 12));

        JLabel icon = new JLabel(
                new NavigationIcon("WORKOUT"));

        icon.setForeground(MUTED);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = bold(
                "아직 운동 기록이 없어요.", 14, TEXT);

        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        empty.add(icon);
        empty.add(Box.createVerticalStrut(12));
        empty.add(title);

        return empty;
    }

    // 공통 디자인

    private static JPanel metric(String name, JLabel value) {

        JPanel panel = vertical();

        panel.add(label(name, 12, MUTED));
        panel.add(Box.createVerticalStrut(7));
        panel.add(value);

        alignLeft(panel);

        return panel;
    }

    private static void setMetric(JLabel label, String value) {

        label.setText(value);

        label.setFont(AppTheme.bold(
                value.length() > 7 ? 16 :
                        value.length() > 5 ? 20 : 23));
    }

    private static String formatNumber(BigDecimal value) {

        return new DecimalFormat("#,##0.##").format(value);
    }

    private static JPanel vertical() {

        JPanel panel = new JPanel();

        panel.setOpaque(false);

        panel.setLayout(
                new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        return panel;
    }

    private static JLabel label(
            String text, int size, Color color) {

        JLabel label = new JLabel(text);

        label.setFont(AppTheme.regular(size));
        label.setForeground(color);

        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    private static JLabel bold(
            String text, int size, Color color) {

        JLabel label = label(text, size, color);

        label.setFont(AppTheme.bold(size));

        return label;
    }

    private static JButton button(
            String text, boolean primary) {

        JButton button = new JButton(text);

        button.setFont(AppTheme.bold(14));

        button.setBackground(primary ? TEXT : CARD);
        button.setForeground(primary ? BG : TEXT);

        button.setBorderPainted(false);

        button.putClientProperty(
                "JButton.buttonType", "roundRect");

        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        return button;
    }

    private static JPanel divider() {

        JPanel line = new JPanel();

        line.setBackground(LINE);

        fullWidth(line, 1);

        return line;
    }

    private static void fullWidth(
            JComponent component, int height) {

        component.setPreferredSize(
                new Dimension(300, height));

        component.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, height));

        component.setAlignmentX(
                Component.LEFT_ALIGNMENT);
    }

    private static void alignLeft(JPanel panel) {

        for (Component component : panel.getComponents()) {

            if (component instanceof JComponent jc) {

                jc.setAlignmentX(
                        Component.LEFT_ALIGNMENT);
            }
        }
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

    @FunctionalInterface
    private interface SqlAction {

        void run() throws SQLException;
    }

    private void runAction(SqlAction action) {

        try {

            action.run();

        } catch (SQLException | IllegalArgumentException e) {

            JOptionPane.showMessageDialog(
                    this, e.getMessage(),
                    "운동 기록 확인",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}