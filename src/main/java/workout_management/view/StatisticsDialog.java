package workout_management.view;

import workout_management.controller.WorkoutController;
import workout_management.domain.*;
import workout_management.util.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

import static workout_management.util.AppTheme.*;

public class StatisticsDialog extends JPanel {

    private final List<Session> sessions = new ArrayList<>();
    private final Map<Long, Exercise> exercises = new HashMap<>();

    private final JComboBox<String> partCombo = new JComboBox<>();
    private final JComboBox<Exercise> exerciseCombo = new JComboBox<>();

    private final JLabel daysValue = bold("0", 23, TEXT);
    private final JLabel setsValue = bold("0", 23, TEXT);
    private final JLabel exercisesValue = bold("0", 23, TEXT);

    private final JLabel graphTitle = label("", 12, MUTED);
    private final JLabel currentValue = bold("—", 28, TEXT);
    private final JLabel difference = label("", 12, MUTED);
    private final JLabel graphCaption = label("", 11, MUTED);

    private final JLabel bestTitle =
            label("역대 최고 중량", 13, MUTED);

    private final JLabel bestWeight = bold("—", 15, TEXT);
    private final JLabel bestReps = bold("—", 15, TEXT);

    private final JButton weekButton = button("1주");
    private final JButton monthButton = button("1개월");
    private final JButton allButton = button("전체");

    private final JButton weightButton = button("최고 중량");
    private final JButton volumeButton = button("운동량");

    private final Graph graph = new Graph();

    private int period = 30;
    private boolean volumeMode;
    private boolean updating;

    private record Session(
            LocalDate date,
            Exercise exercise,
            List<SetRecord> sets) {}

    private record Point(
            LocalDate date,
            double value) {}

    public StatisticsDialog(
            Window owner,
            WorkoutController controller) throws java.sql.SQLException {

        super(new BorderLayout(0, 10));

        setBackground(BG);

        loadData(controller);

        JLabel title = bold("통계", PAGE_TITLE, TEXT);

        add(title, BorderLayout.NORTH);

        JPanel content = new ScrollContent();

        content.add(createPeriodSelector());
        content.add(Box.createVerticalStrut(8));

        content.add(createSummary());
        content.add(Box.createVerticalStrut(10));

        content.add(createExerciseSelector());
        content.add(Box.createVerticalStrut(10));

        content.add(createGraphCard());

        JScrollPane scroll = new JScrollPane(content);

        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.getVerticalScrollBar().setUnitIncrement(20);

        add(scroll, BorderLayout.CENTER);

        alignLeft(content);

        updateExerciseCombo();
        refresh();
    }

    // 운동 데이터 불러오기

    private void loadData(
            WorkoutController controller)
            throws java.sql.SQLException {

        for (Exercise exercise : controller.getExercises()) {

            exercises.put(exercise.getExerciseId(), exercise);
        }

        for (WorkoutRecord record : controller.getRecords()) {

            if (record.getWorkoutDate().isAfter(LocalDate.now()))
                continue;

            Exercise exercise =
                    exercises.get(record.getExerciseId());

            if (exercise == null) continue;

            List<SetRecord> sets =
                    controller.getSets(record.getRecordId());

            if (sets.isEmpty()) continue;

            sessions.add(new Session(
                    record.getWorkoutDate(), exercise, sets));
        }

        sessions.sort(Comparator.comparing(Session::date));
    }

    // 기간 선택

    private JPanel createPeriodSelector() {

        RoundedPanel panel = new RoundedPanel(CARD, 18);

        panel.setLayout(new GridLayout(1, 3, 4, 0));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        weekButton.addActionListener(e -> changePeriod(7));
        monthButton.addActionListener(e -> changePeriod(30));
        allButton.addActionListener(e -> changePeriod(0));

        panel.add(weekButton);
        panel.add(monthButton);
        panel.add(allButton);

        fullWidth(panel, 40);

        return panel;
    }

    private void changePeriod(int value) {

        period = value;

        refresh();
    }

    // 전체 운동 요약

    private JPanel createSummary() {

        RoundedPanel panel = new RoundedPanel(CARD, CARD_ARC);

        panel.setLayout(new GridLayout(1, 3, 6, 0));

        panel.setBorder(new EmptyBorder(12, 8, 12, 8));

        panel.add(metric("운동 일수", daysValue));
        panel.add(metric("총 세트", setsValue));
        panel.add(metric("종목", exercisesValue));

        fullWidth(panel, 76);

        return panel;
    }

    private JPanel metric(String name, JLabel value) {

        JPanel panel = vertical();

        JLabel title = label(name, 12, MUTED);

        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        value.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(5));
        panel.add(value);

        return panel;
    }

    // 부위와 운동 종목을 한 줄에 배치

    private JPanel createExerciseSelector() {

        JPanel panel = new JPanel(new BorderLayout(8, 0));

        panel.setOpaque(false);

        partCombo.addItem("전체 부위");

        String[] parts = {
                "가슴", "등", "하체", "어깨", "팔", "복근"
        };

        Set<String> availableParts = new HashSet<>();

        for (Session session : sessions) {

            availableParts.add(
                    session.exercise().getBodyPart());
        }

        for (String part : parts) {

            if (availableParts.contains(part))
                partCombo.addItem(part);
        }

        styleCombo(partCombo);
        styleCombo(exerciseCombo);

        partCombo.setPreferredSize(new Dimension(116, 38));

        partCombo.addActionListener(e -> {

            if (!updating) updateExerciseCombo();
        });

        exerciseCombo.addActionListener(e -> {

            if (!updating) refresh();
        });

        exerciseCombo.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focused) {

                Object text = value instanceof Exercise exercise
                        ? exercise.getExerciseName() : "운동 선택";

                return super.getListCellRendererComponent(
                        list, text, index, selected, focused);
            }
        });

        panel.add(partCombo, BorderLayout.WEST);
        panel.add(exerciseCombo, BorderLayout.CENTER);

        fullWidth(panel, 38);

        return panel;
    }

    private void updateExerciseCombo() {

        updating = true;

        Exercise previous =
                (Exercise) exerciseCombo.getSelectedItem();

        String part = (String) partCombo.getSelectedItem();

        LinkedHashMap<Long, Exercise> available =
                new LinkedHashMap<>();

        // 최근 운동한 종목부터 표시

        for (int i = sessions.size() - 1; i >= 0; i--) {

            Exercise exercise = sessions.get(i).exercise();

            if (!"전체 부위".equals(part)
                    && !exercise.getBodyPart().equals(part))
                continue;

            available.putIfAbsent(
                    exercise.getExerciseId(), exercise);
        }

        exerciseCombo.removeAllItems();

        Exercise restore = null;

        for (Exercise exercise : available.values()) {

            exerciseCombo.addItem(exercise);

            if (previous != null
                    && previous.getExerciseId()
                    == exercise.getExerciseId()) {

                restore = exercise;
            }
        }

        if (restore != null) {

            exerciseCombo.setSelectedItem(restore);

        } else if (exerciseCombo.getItemCount() > 0) {

            exerciseCombo.setSelectedIndex(0);
        }

        exerciseCombo.setEnabled(
                exerciseCombo.getItemCount() > 0);

        updating = false;

        refresh();
    }

    // 그래프와 최고 기록 통합 카드

    private JPanel createGraphCard() {

        RoundedPanel card = new RoundedPanel(CARD, CARD_ARC);

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        RoundedPanel selector =
                new RoundedPanel(FIELD, 16);

        selector.setLayout(new GridLayout(1, 2, 4, 0));
        selector.setBorder(new EmptyBorder(3, 3, 3, 3));

        weightButton.addActionListener(e -> {

            volumeMode = false;
            refresh();
        });

        volumeButton.addActionListener(e -> {

            volumeMode = true;
            refresh();
        });

        selector.add(weightButton);
        selector.add(volumeButton);

        fullWidth(selector, 36);

        card.add(selector);
        card.add(Box.createVerticalStrut(12));

        card.add(graphTitle);
        card.add(Box.createVerticalStrut(4));

        JPanel valueRow = new JPanel(new BorderLayout(8, 0));

        valueRow.setOpaque(false);

        valueRow.add(currentValue, BorderLayout.WEST);
        valueRow.add(difference, BorderLayout.EAST);

        fullWidth(valueRow, 36);

        card.add(valueRow);
        card.add(Box.createVerticalStrut(4));

        card.add(graph);

        card.add(Box.createVerticalStrut(4));
        card.add(graphCaption);

        card.add(Box.createVerticalStrut(12));
        card.add(divider());
        card.add(Box.createVerticalStrut(10));

        card.add(recordRow(bestTitle, bestWeight));

        card.add(Box.createVerticalStrut(6));

        card.add(recordRow(
                label("한 세트 최다 반복", 13, MUTED),
                bestReps));

        alignLeft(card);

        return card;
    }

    private JPanel recordRow(JLabel name, JLabel value) {

        JPanel panel = new JPanel(new BorderLayout(8, 0));

        panel.setOpaque(false);

        panel.add(name, BorderLayout.WEST);
        panel.add(value, BorderLayout.EAST);

        fullWidth(panel, 26);

        return panel;
    }

    // 통계 갱신

    private void refresh() {

        List<Session> filtered = sessions.stream()
                .filter(session -> inPeriod(session.date()))
                .toList();

        long days = filtered.stream()
                .map(Session::date)
                .distinct()
                .count();

        long totalSets = filtered.stream()
                .mapToLong(session -> session.sets().size())
                .sum();

        long totalExercises = filtered.stream()
                .map(session ->
                        session.exercise().getExerciseId())
                .distinct()
                .count();

        daysValue.setText(days + "일");
        setsValue.setText(totalSets + "세트");
        exercisesValue.setText(totalExercises + "개");

        styleActive(weekButton, period == 7);
        styleActive(monthButton, period == 30);
        styleActive(allButton, period == 0);

        styleActive(weightButton, !volumeMode);
        styleActive(volumeButton, volumeMode);

        Exercise exercise =
                (Exercise) exerciseCombo.getSelectedItem();

        if (exercise == null) {

            graphTitle.setText("운동 기록");
            currentValue.setText("—");
            difference.setText("");
            graphCaption.setText("운동을 기록하면 그래프가 표시됩니다.");

            bestTitle.setText("역대 최고 중량");
            bestWeight.setText("—");
            bestReps.setText("—");

            weightButton.setEnabled(false);
            volumeButton.setEnabled(false);

            graph.setData(List.of(), "kg");

            return;
        }

        weightButton.setEnabled(true);
        volumeButton.setEnabled(true);

        boolean bodyweight = WorkoutDayDialog.isBodyweight(
                exercise.getExerciseName());

        weightButton.setText(
                bodyweight ? "최다 반복" : "최고 중량");

        volumeButton.setText(
                bodyweight ? "총 반복" : "운동량");

        List<Session> exerciseSessions = sessions.stream()
                .filter(session ->
                        session.exercise().getExerciseId()
                                == exercise.getExerciseId())
                .toList();

        List<Session> periodSessions =
                exerciseSessions.stream()
                        .filter(session -> inPeriod(session.date()))
                        .toList();

        List<Point> points = new ArrayList<>();

        for (Session session : periodSessions) {

            points.add(new Point(
                    session.date(),
                    metricValue(session, bodyweight)));
        }

        String unit = bodyweight ? "회" : "kg";

        String metricName = bodyweight
                ? volumeMode ? "총 반복" : "최다 반복"
                : volumeMode ? "운동량" : "최고 중량";

        if (points.isEmpty()) {

            graphTitle.setText(metricName);
            currentValue.setText("—");
            difference.setText("");

            graphCaption.setText("선택한 기간에 운동 기록이 없습니다.");

        } else {

            Point first = points.get(0);
            Point last = points.get(points.size() - 1);

            graphTitle.setText("최근 " + metricName + " · "
                    + last.date().format(
                    DateTimeFormatter.ofPattern("M/d")));

            currentValue.setText(
                    number(last.value()) + " " + unit);

            double change = last.value() - first.value();

            if (points.size() == 1) {

                difference.setText("첫 기록");
                difference.setForeground(MUTED);

            } else if (Math.abs(change) < 0.000001) {

                difference.setText("변화 없음");
                difference.setForeground(MUTED);

            } else {

                difference.setText(
                        (change > 0 ? "+" : "")
                                + number(change) + " " + unit);

                difference.setForeground(
                        change > 0 ? GREEN : RED);
            }

            graphCaption.setText(
                    "기간 첫 기록 대비 · "
                            + points.size() + "회 운동");
        }

        graph.setData(points, unit);

        // 역대 최고 기록은 기간 선택과 무관하게 계산

        BigDecimal maxWeight = BigDecimal.ZERO;
        int maxReps = 0;

        for (Session session : exerciseSessions) {

            for (SetRecord set : session.sets()) {

                maxWeight = maxWeight.max(set.getWeightKg());

                maxReps = Math.max(maxReps, set.getReps());
            }
        }

        if (bodyweight) {

            bestTitle.setText("역대 최고 추가 중량");

            bestWeight.setText(
                    maxWeight.signum() == 0
                            ? "맨몸"
                            : "+" + number(maxWeight.doubleValue())
                              + " kg");

        } else {

            bestTitle.setText("역대 최고 중량");

            bestWeight.setText(
                    number(maxWeight.doubleValue()) + " kg");
        }

        bestReps.setText(maxReps + "회");

        revalidate();
        repaint();
    }

    private boolean inPeriod(LocalDate date) {

        if (period == 0) return true;

        LocalDate start =
                LocalDate.now().minusDays(period - 1);

        return !date.isBefore(start);
    }

    private double metricValue(
            Session session, boolean bodyweight) {

        double value = 0;

        for (SetRecord set : session.sets()) {

            double weight = set.getWeightKg().doubleValue();
            int reps = set.getReps();

            if (bodyweight) {

                if (volumeMode)
                    value += reps;
                else
                    value = Math.max(value, reps);

            } else {

                if (volumeMode)
                    value += weight * reps;
                else
                    value = Math.max(value, weight);
            }
        }

        return value;
    }

    // 성장 그래프

    private static class Graph extends JPanel {

        private List<Point> points = List.of();
        private String unit = "kg";

        Graph() {

            setOpaque(false);

            setPreferredSize(new Dimension(300, 150));
            setMaximumSize(
                    new Dimension(Integer.MAX_VALUE, 150));

            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setData(List<Point> points, String unit) {

            this.points = List.copyOf(points);
            this.unit = unit;

            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();

            try {

                g.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                g.setFont(AppTheme.regular(11));

                if (points.isEmpty()) {

                    g.setColor(MUTED);

                    String message = "표시할 기록이 없습니다.";

                    FontMetrics fm = g.getFontMetrics();

                    g.drawString(message,
                            (getWidth() - fm.stringWidth(message)) / 2,
                            getHeight() / 2);

                    return;
                }

                double min = points.stream()
                        .mapToDouble(Point::value)
                        .min().orElse(0);

                double max = points.stream()
                        .mapToDouble(Point::value)
                        .max().orElse(0);

                double rawStep;

                if (max == min) {

                    rawStep = Math.max(1, max * 0.1);

                } else {

                    rawStep = (max - min) / 3.0;
                }

                double step = niceStep(rawStep);

                if (unit.equals("회"))
                    step = Math.max(1, step);

                double lower = Math.max(
                        0, Math.floor(min / step) * step);

                if (lower == min && lower >= step)
                    lower -= step;

                while (lower + step * 4 < max) {

                    step = niceStep(step * 1.01);

                    lower = Math.max(
                            0, Math.floor(min / step) * step);
                }

                double upper = lower + step * 4;

                FontMetrics fm = g.getFontMetrics();

                int left = Math.max(34,
                        fm.stringWidth(number(upper)) + 10);

                int right = getWidth() - 12;

                int top = 18;
                int bottom = getHeight() - 28;

                if (right <= left || bottom <= top)
                    return;

                g.setColor(MUTED);
                g.drawString(unit, 2, 11);

                // Y축 및 점선

                for (int i = 0; i <= 4; i++) {

                    int y = top
                            + (bottom - top) * i / 4;

                    double value = upper - step * i;

                    String text = number(value);

                    g.setColor(MUTED);

                    g.drawString(text,
                            left - fm.stringWidth(text) - 7,
                            y + 4);

                    g.setColor(LINE);

                    g.setStroke(new BasicStroke(
                            1f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10f,
                            new float[]{3f, 4f},
                            0f));

                    g.drawLine(left, y, right, y);
                }

                LocalDate first = points.get(0).date();

                LocalDate last =
                        points.get(points.size() - 1).date();

                long span = ChronoUnit.DAYS.between(first, last);

                int[] xs = new int[points.size()];
                int[] ys = new int[points.size()];

                for (int i = 0; i < points.size(); i++) {

                    Point point = points.get(i);

                    double fraction = span == 0
                            ? 0.5
                            : (double) ChronoUnit.DAYS.between(
                            first, point.date()) / span;

                    xs[i] = left + (int) Math.round(
                            fraction * (right - left));

                    ys[i] = bottom - (int) Math.round(
                            (point.value() - lower)
                                    / (upper - lower)
                                    * (bottom - top));
                }

                // 그래프 선

                g.setColor(TEXT);

                g.setStroke(new BasicStroke(
                        2f,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));

                for (int i = 1; i < points.size(); i++) {

                    g.drawLine(
                            xs[i - 1], ys[i - 1],
                            xs[i], ys[i]);
                }

                // 데이터 표시

                for (int i = 0; i < points.size(); i++) {

                    boolean latest =
                            i == points.size() - 1;

                    g.setColor(latest ? GREEN : TEXT);

                    int radius = latest ? 4 : 2;

                    g.fillOval(
                            xs[i] - radius,
                            ys[i] - radius,
                            radius * 2,
                            radius * 2);
                }

                // X축 날짜

                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("M/d");

                g.setColor(MUTED);

                if (points.size() == 1) {

                    drawDate(g,
                            first.format(formatter),
                            xs[0], bottom + 20);

                } else {

                    drawDate(g,
                            first.format(formatter),
                            xs[0], bottom + 20);

                    if (points.size() >= 3) {

                        int middle = points.size() / 2;

                        if (xs[middle] - xs[0] > 45
                                && xs[points.size() - 1]
                                - xs[middle] > 45) {

                            drawDate(g,
                                    points.get(middle)
                                            .date().format(formatter),
                                    xs[middle],
                                    bottom + 20);
                        }
                    }

                    drawDate(g,
                            last.format(formatter),
                            xs[points.size() - 1],
                            bottom + 20);
                }

            } finally {

                g.dispose();
            }
        }

        private void drawDate(
                Graphics2D g, String text, int x, int y) {

            FontMetrics fm = g.getFontMetrics();

            int position = x - fm.stringWidth(text) / 2;

            position = Math.max(0,
                    Math.min(position,
                            getWidth() - fm.stringWidth(text)));

            g.drawString(text, position, y);
        }

        private static double niceStep(double value) {

            if (value <= 0) return 1;

            double power = Math.pow(
                    10, Math.floor(Math.log10(value)));

            double normalized = value / power;

            if (normalized <= 1) return power;
            if (normalized <= 2) return 2 * power;
            if (normalized <= 5) return 5 * power;

            return 10 * power;
        }
    }

    // 공통 디자인

    private static String number(double value) {

        return new DecimalFormat("#,##0.##").format(value);
    }

    private static JPanel vertical() {

        JPanel panel = new JPanel();

        panel.setOpaque(false);

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

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

    private static JButton button(String text) {

        JButton button = new JButton(text);

        button.setFont(AppTheme.regular(12));

        button.setBackground(CARD);
        button.setForeground(MUTED);

        button.setBorderPainted(false);

        button.setMargin(new Insets(0, 0, 0, 0));

        button.putClientProperty(
                "JButton.buttonType", "roundRect");

        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        return button;
    }

    private static void styleActive(
            JButton button, boolean active) {

        button.setBackground(active ? TEXT : CARD);

        button.setForeground(active ? BG : MUTED);

        button.setFont(active
                ? AppTheme.bold(12)
                : AppTheme.regular(12));
    }

    private static void styleCombo(JComboBox<?> combo) {

        combo.setFont(AppTheme.regular(14));
        combo.setBackground(CARD);
        combo.setForeground(TEXT);

        combo.putClientProperty(
                "JComponent.roundRect", null);

        combo.putClientProperty(
                "FlatLaf.style",
                "arc: 28; " +
                        "buttonStyle: none; " +
                        "buttonBackground: #272A2F; " +
                        "buttonArrowColor: #B4B4B4; " +
                        "borderColor: #272A2F; " +
                        "padding: 0,10,0,8; " +
                        "selectionArc: 12"
        );
    }

    private static void fullWidth(
            JComponent component, int height) {

        component.setPreferredSize(
                new Dimension(300, height));

        component.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, height));

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void alignLeft(JPanel panel) {

        for (Component component : panel.getComponents()) {

            if (component instanceof JComponent jc) {

                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
    }

    private static JComponent divider() {

        JPanel line = new JPanel();

        line.setBackground(LINE);

        fullWidth(line, 1);

        return line;
    }

    // 둥근 카드

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
                    0, 0,
                    getWidth(), getHeight(),
                    arc, arc);

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

    // 가로 공간을 모두 사용하는 스크롤 화면

    private static class ScrollContent extends JPanel
            implements Scrollable {

        ScrollContent() {

            setBackground(BG);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            setBorder(new EmptyBorder(0, 0, 0, 3));
        }

        public Dimension getPreferredScrollableViewportSize() {

            return new Dimension(400, 520);
        }

        public int getScrollableUnitIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction) {

            return 20;
        }

        public int getScrollableBlockIncrement(
                Rectangle visibleRect,
                int orientation,
                int direction) {

            return Math.max(20, visibleRect.height - 40);
        }

        public boolean getScrollableTracksViewportWidth() {

            return true;
        }

        public boolean getScrollableTracksViewportHeight() {

            return false;
        }
    }
}