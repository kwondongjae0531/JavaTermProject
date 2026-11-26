package workout_management.view;

import workout_management.controller.WorkoutController;
import workout_management.domain.*;
import workout_management.util.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static workout_management.util.AppTheme.*;

public class MainFrame extends JFrame {

    private final Member member;
    private final Runnable onLogout;
    private final WorkoutController controller;

    private final CardLayout layout = new CardLayout();

    private final JPanel pages = new JPanel(layout);

    private final Map<String, JPanel> pageMap =
            new HashMap<>();

    private final Map<String, JButton> navButtons =
            new LinkedHashMap<>();

    private final JPanel header =
            new JPanel(new BorderLayout());

    private final JPanel navigation =
            new JPanel(new GridLayout(1, 3));

    private final DashboardPanel homePanel;

    private HistoryDialog historyPanel;
    private WorkoutDayDialog workoutPage;

    private String currentPage = "HOME";
    private String detailOrigin = "HISTORY";

    private Long returnRecordId;

    public MainFrame(Member member, Runnable onLogout) {

        this.member = member;
        this.onLogout = onLogout;

        this.controller = new WorkoutController(member);

        setTitle("LIFTLOG");

        setSize(460, 800);

        setMinimumSize(new Dimension(420, 700));

        setLocationRelativeTo(null);

        setDefaultCloseOperation(
                WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {

                if (canLeaveWorkout())
                    dispose();
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 14));

        root.setBackground(BG);

        root.setBorder(
                new EmptyBorder(18, 18, 14, 18));

        setContentPane(root);

        // 공통 헤더

        createHeader();

        root.add(header, BorderLayout.NORTH);

        // 메인 화면

        pages.setBackground(BG);

        homePanel = new DashboardPanel(
                controller,

                () -> runAction(this::startFromHome),

                () -> runAction(() -> showHistory(false)),

                () -> runAction(this::startFromHome),

                record -> runAction(() ->
                        openDetail(record.getRecordId(), "HOME"))
        );

        replacePage("HOME", homePanel);

        root.add(pages, BorderLayout.CENTER);

        // 하단 내비게이션

        createNavigation();

        root.add(navigation, BorderLayout.SOUTH);

        display("HOME");
    }

    // 상단 로고 및 프로필

    private void createHeader() {

        header.setOpaque(false);

        JLabel logo = bold("LIFTLOG.", LOGO, TEXT);

        header.add(logo, BorderLayout.WEST);

        JButton profile = textButton("");

        profile.setIcon(new NavigationIcon("PROFILE"));

        profile.setForeground(TEXT);

        profile.setPreferredSize(new Dimension(44, 44));

        profile.setToolTipText("내 계정");

        profile.getAccessibleContext()
                .setAccessibleName("내 계정");

        profile.addActionListener(e -> {

            JPopupMenu menu = new JPopupMenu();

            JMenuItem account = new JMenuItem(
                    member.getLoginId());

            account.setFont(AppTheme.bold(14));
            account.setEnabled(false);

            JMenuItem logout = new JMenuItem("로그아웃");

            logout.setFont(AppTheme.body());

            logout.addActionListener(event -> logout());

            menu.add(account);
            menu.addSeparator();
            menu.add(logout);

            menu.show(profile,
                    Math.min(0, profile.getWidth()
                            - menu.getPreferredSize().width),
                    profile.getHeight());
        });

        header.add(profile, BorderLayout.EAST);
    }

    private void logout() {

        if (!canLeaveWorkout()) return;

        dispose();

        onLogout.run();
    }

    // 하단 내비게이션

    private void createNavigation() {

        navigation.setOpaque(false);

        navigation.setBorder(
                BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 0, 0, 0, LINE),
                        new EmptyBorder(8, 0, 0, 0)));

        navigation.setPreferredSize(
                new Dimension(300, 62));

        addNavigation("HOME", "홈");
        addNavigation("HISTORY", "기록");
        addNavigation("STATS", "통계");
    }

    private void addNavigation(String key, String title) {

        JButton button = textButton(title);

        button.setIcon(new NavigationIcon(key));

        button.setFont(AppTheme.regular(11));

        button.setHorizontalAlignment(
                SwingConstants.CENTER);

        button.setHorizontalTextPosition(
                SwingConstants.CENTER);

        button.setVerticalTextPosition(
                SwingConstants.BOTTOM);

        button.setIconTextGap(5);

        button.addActionListener(e -> {

            if (currentPage.equals(key)) return;

            if (!canLeaveWorkout()) return;

            runAction(() -> {

                switch (key) {

                    case "HOME" -> showHome(true);

                    case "HISTORY" -> showHistory(false);

                    case "STATS" -> showStats();
                }
            });
        });

        navButtons.put(key, button);

        navigation.add(button);
    }

    // 화면 전환

    private void replacePage(String key, JPanel panel) {

        JPanel old = pageMap.put(key, panel);

        if (old != null) pages.remove(old);

        pages.add(panel, key);
    }

    private void display(String key) {

        currentPage = key;

        layout.show(pages, key);

        boolean workout = key.equals("WORKOUT");

        header.setVisible(!workout);

        navigation.setVisible(!workout);

        String selectedTab = key.equals("DETAIL")
                ? "HISTORY" : key;

        for (var entry : navButtons.entrySet()) {

            boolean selected =
                    entry.getKey().equals(selectedTab);

            entry.getValue().setForeground(
                    selected ? TEXT : MUTED);

            entry.getValue().setFont(
                    selected
                            ? AppTheme.bold(11)
                            : AppTheme.regular(11));
        }

        if (!workout && workoutPage != null) {

            JPanel old = pageMap.remove("WORKOUT");

            if (old != null) pages.remove(old);

            workoutPage = null;
        }

        pages.revalidate();
        pages.repaint();

        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void showHome(boolean today) throws SQLException {

        if (today) homePanel.showToday();

        homePanel.refresh();

        display("HOME");
    }

    private void showHistory(boolean preserve)
            throws SQLException {

        if (!preserve || historyPanel == null) {

            historyPanel = new HistoryDialog(
                    this,
                    controller.getRecords(),
                    controller.getExercises(),
                    id -> runAction(() ->
                            openDetail(id, "HISTORY")),
                    controller);

            replacePage("HISTORY", historyPanel);
        }

        display("HISTORY");
    }

    private void showStats() throws SQLException {

        StatisticsDialog stats =
                new StatisticsDialog(this, controller);

        replacePage("STATS", stats);

        display("STATS");
    }

    // 운동 기록 시작

    private void startFromHome() throws SQLException {

        openWorkout(homePanel.getDate(), null);
    }

    private void openWorkout(
            LocalDate date, Long recordId)
            throws SQLException {

        WorkoutDayDialog newPage = new WorkoutDayDialog(
                this,
                controller,
                date,
                () -> runAction(this::finishWorkout));

        returnRecordId = recordId;

        workoutPage = newPage;

        replacePage("WORKOUT", workoutPage);

        display("WORKOUT");

        if (recordId != null) {

            SwingUtilities.invokeLater(() -> {

                if (workoutPage == newPage)
                    newPage.focusRecord(recordId);
            });
        }
    }

    private boolean canLeaveWorkout() {

        return !"WORKOUT".equals(currentPage)
                || workoutPage == null
                || workoutPage.prepareToLeave();
    }

    private void finishWorkout() throws SQLException {

        if (!canLeaveWorkout()) return;

        Long id = returnRecordId;

        if (id == null) {

            showHome(false);

        } else {

            boolean exists = controller.getRecords()
                    .stream()
                    .anyMatch(record ->
                            record.getRecordId() == id);

            if (exists) {

                openDetail(id, detailOrigin);

            } else {

                returnFromDetail();
            }
        }

        returnRecordId = null;
    }

    // 운동 기록 상세

    private void openDetail(long id, String origin)
            throws SQLException {

        ArrayList<WorkoutRecord> records =
                controller.getRecords();

        WorkoutRecord record = records.stream()
                .filter(item -> item.getRecordId() == id)
                .findFirst()
                .orElseThrow(() -> new SQLException(
                        "운동 기록을 찾을 수 없습니다."));

        Exercise exercise = controller.getExercises()
                .stream()
                .filter(item ->
                        item.getExerciseId()
                                == record.getExerciseId())
                .findFirst()
                .orElseThrow(() -> new SQLException(
                        "운동 종목을 찾을 수 없습니다."));

        List<SetRecord> sets =
                controller.getSets(record.getRecordId());

        JPanel detail = createDetailPage(
                record, exercise, sets, records);

        detailOrigin = origin;

        replacePage("DETAIL", detail);

        display("DETAIL");
    }

    private void returnFromDetail() throws SQLException {

        if ("HOME".equals(detailOrigin)) {

            showHome(false);

        } else {

            showHistory(true);
        }
    }

    private JPanel createDetailPage(
            WorkoutRecord record,
            Exercise exercise,
            List<SetRecord> sets,
            List<WorkoutRecord> records)
            throws SQLException {

        JPanel page = new JPanel(new BorderLayout(0, 12));

        page.setBackground(BG);

        boolean bodyweight = WorkoutDayDialog.isBodyweight(
                exercise.getExerciseName());

        // 뒤로가기 및 운동 이름

        JPanel top = vertical();

        JPanel titleRow = new JPanel(new BorderLayout(8, 0));

        titleRow.setOpaque(false);

        JButton back = textButton("←");

        back.setFont(AppTheme.regular(24));

        back.setForeground(TEXT);

        back.setPreferredSize(new Dimension(32, 44));

        back.setToolTipText("돌아가기");

        back.getAccessibleContext()
                .setAccessibleName("돌아가기");

        back.addActionListener(e ->
                runAction(this::returnFromDetail));

        JLabel name = bold(
                exercise.getExerciseName(), PAGE_TITLE, TEXT);

        name.setToolTipText(exercise.getExerciseName());

        JButton menu = textButton("···");

        menu.setFont(AppTheme.bold(23));

        menu.setPreferredSize(new Dimension(40, 44));

        menu.setToolTipText("운동 기록 관리");

        menu.getAccessibleContext()
                .setAccessibleName("운동 기록 관리");

        menu.addActionListener(e ->
                showRecordMenu(menu, record));

        titleRow.add(back, BorderLayout.WEST);
        titleRow.add(name, BorderLayout.CENTER);
        titleRow.add(menu, BorderLayout.EAST);

        fullWidth(titleRow, 44);

        top.add(titleRow);

        JLabel date = label(
                record.getWorkoutDate().format(
                        DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                        + " · " + exercise.getBodyPart(),
                12, MUTED);

        top.add(date);

        alignLeft(top);

        page.add(top, BorderLayout.NORTH);

        // 스크롤 가능한 상세 내용

        JPanel content = new ScrollContent();

        content.add(createDetailSummary(sets, bodyweight));

        // 이전 운동이 존재할 때만 비교 버튼 표시

        WorkoutRecord previous = null;

        List<SetRecord> previousSets = List.of();

        List<WorkoutRecord> candidates = records.stream()
                .filter(item ->
                        item.getExerciseId() == record.getExerciseId()
                                && item.getWorkoutDate().isBefore(
                                record.getWorkoutDate()))
                .sorted(Comparator
                        .comparing(WorkoutRecord::getWorkoutDate)
                        .thenComparingLong(WorkoutRecord::getRecordId)
                        .reversed())
                .toList();

        for (WorkoutRecord candidate : candidates) {

            List<SetRecord> history =
                    controller.getSets(candidate.getRecordId());

            if (!history.isEmpty()) {

                previous = candidate;

                previousSets = history;

                break;
            }
        }

        if (previous != null) {

            WorkoutRecord previousRecord = previous;

            List<SetRecord> comparisonSets = previousSets;

            JButton compare = button("이전 운동 비교", false);

            fullWidth(compare, 42);

            compare.addActionListener(e -> {

                ComparisonDialog dialog = new ComparisonDialog(
                        this,
                        exercise.getExerciseName(),
                        previousRecord,
                        comparisonSets,
                        record,
                        sets);

                dialog.setVisible(true);
            });

            content.add(Box.createVerticalStrut(8));
            content.add(compare);
        }

        content.add(Box.createVerticalStrut(18));

        content.add(bold("세트 기록", 18, TEXT));

        content.add(Box.createVerticalStrut(10));

        content.add(createSetTable(sets, bodyweight));

        alignLeft(content);

        JScrollPane scroll = new JScrollPane(content);

        scroll.setBorder(null);

        scroll.getViewport().setBackground(BG);

        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.getVerticalScrollBar().setUnitIncrement(20);

        page.add(scroll, BorderLayout.CENTER);

        // 하단 기록 수정 버튼

        JButton edit = button("기록 수정", true);

        edit.setPreferredSize(new Dimension(300, 48));

        edit.addActionListener(e -> runAction(() ->
                openWorkout(
                        record.getWorkoutDate(),
                        record.getRecordId())));

        page.add(edit, BorderLayout.SOUTH);

        return page;
    }

    // 상세 요약 카드

    private JPanel createDetailSummary(
            List<SetRecord> sets, boolean bodyweight) {

        BigDecimal maxWeight = BigDecimal.ZERO;
        BigDecimal volume = BigDecimal.ZERO;

        long totalReps = 0;

        for (SetRecord set : sets) {

            maxWeight = maxWeight.max(set.getWeightKg());

            totalReps += set.getReps();

            volume = volume.add(
                    set.getWeightKg().multiply(
                            BigDecimal.valueOf(set.getReps())));
        }

        RoundedPanel summary =
                new RoundedPanel(CARD, CARD_ARC);

        summary.setLayout(new GridLayout(1, 3, 8, 0));

        summary.setBorder(
                new EmptyBorder(14, 14, 14, 14));

        summary.add(metric(
                "세트", String.valueOf(sets.size())));

        String maxText = sets.isEmpty()
                ? "—"
                : WorkoutDayDialog.formatWeight(
                maxWeight, bodyweight);

        if (!sets.isEmpty()
                && (!bodyweight || maxWeight.signum() != 0)) {

            maxText += "kg";
        }

        summary.add(metric(
                bodyweight ? "최고 추가 중량" : "최고 중량",
                maxText));

        summary.add(metric(
                bodyweight ? "총 반복" : "운동량(kg)",
                bodyweight
                        ? totalReps + "회"
                        : formatNumber(volume)));

        return summary;
    }

    private JPanel metric(String title, String value) {

        JPanel panel = vertical();

        panel.add(label(title, 11, MUTED));

        panel.add(Box.createVerticalStrut(8));

        int size = value.length() > 7 ? 16 :
                value.length() > 5 ? 19 : 23;

        panel.add(bold(value, size, TEXT));

        alignLeft(panel);

        return panel;
    }

    // 세트 기록 통합 표

    private JPanel createSetTable(
            List<SetRecord> sets, boolean bodyweight) {

        RoundedPanel table =
                new RoundedPanel(CARD, CARD_ARC);

        table.setLayout(
                new BoxLayout(table, BoxLayout.Y_AXIS));

        table.setBorder(
                new EmptyBorder(8, 12, 8, 12));

        JPanel headings = new JPanel(new GridLayout(1, 4));

        headings.setOpaque(false);

        headings.add(centered("세트", 12, MUTED, false));

        headings.add(centered(
                bodyweight ? "추가 kg" : "kg",
                12, MUTED, false));

        headings.add(centered("횟수", 12, MUTED, false));

        headings.add(centered("휴식", 12, MUTED, false));

        fullWidth(headings, 34);

        table.add(headings);
        table.add(divider());

        List<SetRecord> sorted = sets.stream()
                .sorted(Comparator.comparingInt(
                        SetRecord::getSetNo))
                .toList();

        for (int i = 0; i < sorted.size(); i++) {

            SetRecord set = sorted.get(i);

            JPanel row = new JPanel(new GridLayout(1, 4));

            row.setOpaque(false);

            row.add(centered(
                    String.valueOf(set.getSetNo()),
                    14, MUTED, false));

            row.add(centered(
                    WorkoutDayDialog.formatWeight(
                            set.getWeightKg(), bodyweight),
                    15, TEXT, true));

            row.add(centered(
                    String.valueOf(set.getReps()),
                    15, TEXT, true));

            row.add(centered(
                    formatRest(set.getRestSeconds()),
                    13, MUTED, false));

            fullWidth(row, 42);

            table.add(row);

            if (i < sorted.size() - 1)
                table.add(divider());
        }

        if (sorted.isEmpty()) {

            JLabel empty = centered(
                    "저장된 세트가 없어요.",
                    13, MUTED, false);

            fullWidth(empty, 60);

            table.add(empty);
        }

        alignLeft(table);

        return table;
    }

    // 운동 기록 관리 메뉴

    private void showRecordMenu(
            JButton button, WorkoutRecord record) {

        JPopupMenu menu = new JPopupMenu();

        JMenuItem edit = new JMenuItem("날짜 및 종목 수정");

        edit.setFont(AppTheme.body());

        edit.addActionListener(e ->
                runAction(() -> editRecordInfo(record)));

        JMenuItem delete = new JMenuItem("기록 삭제");

        delete.setFont(AppTheme.body());

        delete.setForeground(RED);

        delete.addActionListener(e ->
                runAction(() -> deleteRecord(record)));

        menu.add(edit);
        menu.addSeparator();
        menu.add(delete);

        menu.show(
                button,
                Math.min(0, button.getWidth()
                        - menu.getPreferredSize().width),
                button.getHeight());
    }

    // 운동 날짜 및 종목 수정

    private void editRecordInfo(WorkoutRecord record)
            throws SQLException {

        CalendarField editDate =
                new CalendarField(
                        record.getWorkoutDate(), false);

        JComboBox<Exercise> combo = new JComboBox<>();

        for (Exercise exercise : controller.getExercises()) {

            combo.addItem(exercise);

            if (exercise.getExerciseId()
                    == record.getExerciseId()) {

                combo.setSelectedItem(exercise);
            }
        }

        combo.setFont(AppTheme.body());

        combo.setBackground(CARD);

        combo.setForeground(TEXT);

        combo.putClientProperty(
                "FlatLaf.style",
                "buttonStyle: none; buttonBackground: #272A2F; arc: 28");

        JPanel form = vertical();

        form.add(label("운동 날짜", 13, MUTED));

        form.add(Box.createVerticalStrut(6));

        fullWidth(editDate, 42);

        form.add(editDate);

        form.add(Box.createVerticalStrut(14));

        form.add(label("운동 종목", 13, MUTED));

        form.add(Box.createVerticalStrut(6));

        fullWidth(combo, 42);

        form.add(combo);

        alignLeft(form);

        int result = JOptionPane.showConfirmDialog(
                this,
                form,
                "운동 기록 수정",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        Exercise selected =
                (Exercise) combo.getSelectedItem();

        LocalDate date = editDate.getDate();

        if (selected == null || date == null) {

            throw new IllegalArgumentException(
                    "운동 날짜와 종목을 확인하세요.");
        }

        if (selected.getExerciseId() == record.getExerciseId()
                && date.equals(record.getWorkoutDate())) {

            return;
        }

        controller.updateRecord(
                record.getRecordId(), selected, date);

        openDetail(record.getRecordId(), detailOrigin);
    }

    private void deleteRecord(WorkoutRecord record)
            throws SQLException {

        if (!AppDialogs.confirm(
                this,
                "운동 기록 삭제",
                "이 운동의 기록과 모든 세트를 삭제할까요?",
                "취소", "삭제", true)) return;

        controller.deleteRecord(record.getRecordId());

        if ("HOME".equals(detailOrigin)) {

            showHome(false);

        } else {

            showHistory(false);
        }
    }

    // 공통 UI

    private static String formatNumber(BigDecimal value) {

        return new DecimalFormat("#,##0.##").format(value);
    }

    private static String formatRest(Integer seconds) {

        return seconds == null
                ? "—"
                : String.format(
                "%d:%02d", seconds / 60, seconds % 60);
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

    private static JLabel centered(
            String text,
            int size,
            Color color,
            boolean bold) {

        JLabel label = bold
                ? bold(text, size, color)
                : label(text, size, color);

        label.setHorizontalAlignment(
                SwingConstants.CENTER);

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

    private static JButton textButton(String text) {

        JButton button = new JButton(text);

        button.setFont(AppTheme.regular(13));

        button.setForeground(MUTED);

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);

        button.setMargin(new Insets(0, 0, 0, 0));

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

    // 스크롤 콘텐츠

    private static class ScrollContent
            extends JPanel implements Scrollable {

        ScrollContent() {

            setBackground(BG);

            setLayout(
                    new BoxLayout(this, BoxLayout.Y_AXIS));

            setBorder(new EmptyBorder(0, 0, 0, 3));
        }

        public Dimension getPreferredScrollableViewportSize() {

            return new Dimension(400, 500);
        }

        public int getScrollableUnitIncrement(
                Rectangle r, int orientation, int direction) {

            return 20;
        }

        public int getScrollableBlockIncrement(
                Rectangle r, int orientation, int direction) {

            return Math.max(20, r.height - 40);
        }

        public boolean getScrollableTracksViewportWidth() {

            return true;
        }

        public boolean getScrollableTracksViewportHeight() {

            return false;
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
                    this,
                    e.getMessage(),
                    "운동 기록 확인",
                    JOptionPane.WARNING_MESSAGE);
        }
    }
}