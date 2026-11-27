package workout_management.view;

import workout_management.controller.WorkoutController;
import workout_management.domain.*;
import workout_management.util.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static workout_management.util.AppTheme.*;

public class HistoryDialog extends JPanel {

    private final WorkoutController controller;
    private final Consumer<Long> onOpen;

    private final ArrayList<WorkoutRecord> records;
    private final Map<Long, Exercise> exercises = new HashMap<>();
    private final Map<Long, Integer> setCounts = new HashMap<>();

    private final Set<Long> selected = new HashSet<>();
    private final List<Long> visibleIds = new ArrayList<>();
    private final Map<Row, List<Long>> dayIds = new HashMap<>();

    private final JTextField search = new JTextField();

    private final CalendarField dateField =
            new CalendarField(null, true);

    private final JComboBox<String> partCombo =
            new JComboBox<>(new String[]{
                    "전체 부위", "가슴", "등",
                    "하체", "어깨", "팔", "복근"
            });

    private final JLabel count = label("", 12, MUTED);

    private final JButton selectButton = textButton("선택");
    private final JButton allButton = textButton("전체 선택");
    private final JButton filterButton = button("필터", false);
    private final JButton deleteButton = button("삭제", false);

    private final JPanel filterPanel = new RoundedPanel(CARD);
    private final JPanel deletePanel = new JPanel(new BorderLayout());

    private final DefaultListModel<Row> model =
            new DefaultListModel<>();

    private final JList<Row> list = new JList<>(model);

    private boolean selectionMode;
    private boolean shown;
    private boolean queued;

    private Long selectedRecordId;

    private record Row(
            int type,
            String name,
            String meta,
            WorkoutRecord record,
            boolean first,
            boolean last) {

        @Override
        public String toString() {
            return name + " " + meta;
        }
    }

    public HistoryDialog(
            JFrame owner,
            ArrayList<WorkoutRecord> records,
            ArrayList<Exercise> exercises,
            Consumer<Long> onOpen,
            WorkoutController controller) throws SQLException {

        super(new BorderLayout(0, 10));

        this.controller = controller;
        this.onOpen = onOpen;
        this.records = new ArrayList<>(records);

        for (Exercise exercise : exercises) {

            this.exercises.put(
                    exercise.getExerciseId(), exercise);
        }

        loadSetCounts();

        setBackground(BG);

        JPanel top = vertical();

        // 상단 제목

        JPanel title = new JPanel(new BorderLayout());
        title.setOpaque(false);

        title.add(bold("운동 기록", PAGE_TITLE, TEXT),
                BorderLayout.WEST);

        selectButton.setPreferredSize(new Dimension(65, 44));

        selectButton.addActionListener(e ->
                setSelectionMode(!selectionMode));

        title.add(selectButton, BorderLayout.EAST);

        top.add(title);
        top.add(Box.createVerticalStrut(10));

        // 검색창

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));

        searchRow.setOpaque(false);

        search.setFont(AppTheme.body());
        search.setBackground(CARD);
        search.setForeground(TEXT);
        search.setCaretColor(TEXT);

        search.setMargin(new Insets(8, 14, 8, 14));

        search.putClientProperty(
                "JTextField.placeholderText", "운동 검색");

        search.putClientProperty(
                "JTextField.showClearButton", true);

        search.putClientProperty(
                "JComponent.roundRect", true);

        search.getAccessibleContext()
                .setAccessibleName("운동 검색");

        filterButton.setPreferredSize(new Dimension(76, 44));

        filterButton.addActionListener(e -> {

            filterPanel.setVisible(!filterPanel.isVisible());

            updateFilterButton();

            revalidate();
            repaint();
        });

        searchRow.add(search, BorderLayout.CENTER);
        searchRow.add(filterButton, BorderLayout.EAST);

        fullWidth(searchRow, 44);

        top.add(searchRow);
        top.add(Box.createVerticalStrut(8));

        // 필터

        setupFilters();

        top.add(filterPanel);

        filterPanel.setVisible(false);

        // 기록 개수

        JPanel countRow = new JPanel(new BorderLayout());

        countRow.setOpaque(false);

        countRow.add(count, BorderLayout.WEST);

        allButton.setVisible(false);

        allButton.addActionListener(e -> toggleAll());

        countRow.add(allButton, BorderLayout.EAST);

        fullWidth(countRow, 32);

        top.add(countRow);

        // 기존에 발생했던 좌우 정렬 문제 방지

        alignLeft(top);

        add(top, BorderLayout.NORTH);

        // 운동 기록 목록

        setupList();

        JScrollPane scroll = new JScrollPane(list);

        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.getVerticalScrollBar().setUnitIncrement(20);

        add(scroll, BorderLayout.CENTER);

        // 다중 선택 삭제

        deletePanel.setOpaque(false);

        deleteButton.setBackground(RED_BG);
        deleteButton.setForeground(RED);

        deleteButton.setPreferredSize(new Dimension(300, 48));

        deleteButton.addActionListener(e -> deleteSelected());

        deletePanel.add(deleteButton);

        deletePanel.setVisible(false);

        add(deletePanel, BorderLayout.SOUTH);

        watch(search, this::refresh);

        watch(dateField, this::refreshLater);

        partCombo.addActionListener(e -> refresh());

        // 다른 화면에서 돌아오면 최신 기록 반영

        addHierarchyListener(e -> {

            if ((e.getChangeFlags()
                    & HierarchyEvent.SHOWING_CHANGED) == 0
                    || !isShowing()) return;

            if (!shown) {

                shown = true;
                return;
            }

            try {

                reloadData();
                refresh();

            } catch (SQLException ex) {

                error(ex.getMessage());
            }
        });

        refresh();
    }

    // 최신 운동 정보 조회

    private void reloadData() throws SQLException {

        ArrayList<WorkoutRecord> updatedRecords =
                controller.getRecords();

        ArrayList<Exercise> updatedExercises =
                controller.getExercises();

        Map<Long, Integer> updatedCounts = new HashMap<>();

        for (WorkoutRecord record : updatedRecords) {

            updatedCounts.put(
                    record.getRecordId(),
                    controller.getSets(record.getRecordId()).size());
        }

        records.clear();
        records.addAll(updatedRecords);

        exercises.clear();

        for (Exercise exercise : updatedExercises) {

            exercises.put(
                    exercise.getExerciseId(), exercise);
        }

        setCounts.clear();
        setCounts.putAll(updatedCounts);
    }

    private void loadSetCounts() throws SQLException {

        setCounts.clear();

        for (WorkoutRecord record : records) {

            setCounts.put(
                    record.getRecordId(),
                    controller.getSets(record.getRecordId()).size());
        }
    }

    // 필터 화면

    private void setupFilters() {

        filterPanel.setLayout(
                new BoxLayout(filterPanel, BoxLayout.Y_AXIS));

        filterPanel.setBorder(
                new EmptyBorder(14, 14, 14, 14));

        filterPanel.add(label("운동 날짜", 12, MUTED));
        filterPanel.add(Box.createVerticalStrut(6));

        dateField.setBackground(FIELD);

        dateField.putClientProperty(
                "JTextField.placeholderText", "전체 날짜");

        fullWidth(dateField, 44);

        filterPanel.add(dateField);
        filterPanel.add(Box.createVerticalStrut(12));

        filterPanel.add(label("운동 부위", 12, MUTED));
        filterPanel.add(Box.createVerticalStrut(6));

        partCombo.setBackground(FIELD);
        partCombo.setFont(AppTheme.body());

        fullWidth(partCombo, 44);

        filterPanel.add(partCombo);
        filterPanel.add(Box.createVerticalStrut(12));

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));

        actions.setOpaque(false);

        fullWidth(actions, 40);

        JButton reset = button("초기화", false);

        reset.setBackground(FIELD);

        reset.addActionListener(e -> {

            dateField.setDate(null);
            partCombo.setSelectedIndex(0);

            refreshLater();
        });

        JButton done = button("완료", true);

        done.addActionListener(e -> {

            filterPanel.setVisible(false);

            updateFilterButton();

            revalidate();
            repaint();
        });

        actions.add(reset);
        actions.add(done);

        filterPanel.add(actions);

        alignLeft(filterPanel);
    }

    // 날짜별 운동 카드 렌더링

    private void setupList() {

        list.setBackground(BG);
        list.setForeground(TEXT);

        list.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        list.getAccessibleContext().setAccessibleName(
                "날짜별 운동 기록");

        list.setCellRenderer(
                (view, row, index, active, focused) ->
                        renderRow(row, active, focused));

        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (!SwingUtilities.isLeftMouseButton(e)
                        || e.getClickCount() != 1) return;

                int index = list.locationToIndex(e.getPoint());

                if (index < 0) return;

                Rectangle bounds = list.getCellBounds(index, index);

                if (bounds != null && bounds.contains(e.getPoint())) {

                    activate(model.get(index));
                }
            }
        });

        list.getInputMap().put(
                KeyStroke.getKeyStroke("ENTER"), "open");

        list.getActionMap().put("open", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {

                activate(list.getSelectedValue());
            }
        });

        list.getInputMap().put(
                KeyStroke.getKeyStroke("SPACE"), "toggle");

        list.getActionMap().put("toggle", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {

                if (selectionMode)
                    activate(list.getSelectedValue());
            }
        });

        moveKey("UP", -1);
        moveKey("DOWN", 1);
    }

    private Component renderRow(
            Row row, boolean active, boolean focused) {

        // 카드 사이 여백

        if (row.type() == 3) {

            JPanel gap = new JPanel();

            gap.setBackground(BG);
            gap.setPreferredSize(new Dimension(300, 12));

            return gap;
        }

        // 기록이 없는 화면

        if (row.type() == 4) {

            JPanel empty = new JPanel(new GridBagLayout());

            empty.setBackground(BG);
            empty.setPreferredSize(new Dimension(300, 180));

            JPanel content = vertical();

            JLabel icon = new JLabel(
                    new NavigationIcon("HISTORY"));

            icon.setForeground(MUTED);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel message = label(row.name(), 14, MUTED);

            message.setAlignmentX(Component.CENTER_ALIGNMENT);

            content.add(icon);
            content.add(Box.createVerticalStrut(14));
            content.add(message);

            empty.add(content);

            return empty;
        }

        // 날짜 제목

        if (row.type() == 0) {

            GroupRow panel = new GroupRow(
                    CARD, true, false);

            panel.setLayout(new BorderLayout(8, 0));

            panel.setBorder(
                    new EmptyBorder(14, 14, 10, 14));

            panel.setPreferredSize(new Dimension(300, 52));

            panel.add(bold(row.name(), 15, TEXT),
                    BorderLayout.CENTER);

            if (selectionMode) {
                List<Long> ids = dayIds.getOrDefault(row, List.of());
                boolean all = !ids.isEmpty() && selected.containsAll(ids);
                boolean partial = !all && ids.stream().anyMatch(selected::contains);
                JLabel check = new JLabel(new SelectionIcon(all, partial));
                check.setPreferredSize(new Dimension(22, 22));
                panel.add(check, BorderLayout.WEST);
            }

            panel.add(label(row.meta(), 12, MUTED),
                    BorderLayout.EAST);

            return panel;
        }

        // 운동 부위

        if (row.type() == 1) {

            GroupRow panel = new GroupRow(
                    CARD, false, false);

            panel.setLayout(new BorderLayout());

            panel.setBorder(
                    new EmptyBorder(7, 14, 5, 14));

            panel.setPreferredSize(new Dimension(300, 32));

            panel.add(bold(row.name(), 13, MUTED),
                    BorderLayout.WEST);

            panel.add(label(row.meta(), 11, MUTED),
                    BorderLayout.EAST);

            return panel;
        }

        // 운동 종목

        boolean checked = selected.contains(
                row.record().getRecordId());

        GroupRow panel = new GroupRow(
                checked || (active && focused)
                        ? FIELD : CARD,
                false, row.last());

        panel.setLayout(new BorderLayout(8, 0));

        panel.setBorder(
                new EmptyBorder(8, 14,
                        row.last() ? 13 : 8, 14));

        panel.setPreferredSize(new Dimension(
                300, row.last() ? 49 : 44));

        JLabel name = label(row.name(), 15, TEXT);

        panel.add(name, BorderLayout.CENTER);

        if (selectionMode) {

            JLabel check = new JLabel(
                    new SelectionIcon(checked));

            check.setPreferredSize(new Dimension(22, 22));

            panel.add(check, BorderLayout.WEST);
        }

        JPanel right = new JPanel(
                new FlowLayout(FlowLayout.RIGHT, 8, 0));

        right.setOpaque(false);

        right.add(label(row.meta(), 12, MUTED));

        if (!selectionMode)
            right.add(label("›", 20, MUTED));

        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    // 키보드로 운동 종목 선택

    private void moveKey(String key, int direction) {

        String action = "move-" + key;

        list.getInputMap().put(
                KeyStroke.getKeyStroke(key), action);

        list.getActionMap().put(action, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {

                int index = list.getSelectedIndex();

                if (index < 0)
                    index = direction > 0 ? -1 : model.size();

                for (index += direction;
                     index >= 0 && index < model.size();
                     index += direction) {

                    Row row = model.get(index);
                    if (row.record() != null
                            || (selectionMode && row.type() == 0)) {

                        list.setSelectedIndex(index);
                        list.ensureIndexIsVisible(index);

                        break;
                    }
                }
            }
        });
    }

    private void activate(Row row) {

        if (row == null) return;

        if (selectionMode && row.type() == 0) {
            List<Long> ids = dayIds.getOrDefault(row, List.of());
            if (ids.isEmpty()) return;
            if (selected.containsAll(ids)) selected.removeAll(ids);
            else selected.addAll(ids);
            updateSelection();
            return;
        }

        if (row.record() == null) return;

        long id = row.record().getRecordId();

        if (!selectionMode) {

            selectedRecordId = id;

            onOpen.accept(id);

            return;
        }

        if (!selected.add(id))
            selected.remove(id);

        updateSelection();
    }

    public Long getSelectedRecordId() {

        return selectedRecordId;
    }

    // 검색 및 날짜별 그룹화

    private void refreshLater() {

        if (queued) return;

        queued = true;

        SwingUtilities.invokeLater(() -> {

            queued = false;

            refresh();
        });
    }

    private void refresh() {

        String keyword = normalize(search.getText());

        String part = (String) partCombo.getSelectedItem();

        LocalDate date = dateField.getDate();

        Map<LocalDate, Map<String, List<WorkoutRecord>>> groups =
                new TreeMap<>(Comparator.reverseOrder());

        List<WorkoutRecord> sorted = records.stream()
                .sorted(Comparator.comparingLong(
                        WorkoutRecord::getRecordId))
                .toList();

        selected.clear();
        visibleIds.clear();
        dayIds.clear();
        model.clear();

        for (WorkoutRecord record : sorted) {

            Exercise exercise =
                    exercises.get(record.getExerciseId());

            if (exercise == null) continue;

            if (date != null
                    && !date.equals(record.getWorkoutDate()))
                continue;

            if (!"전체 부위".equals(part)
                    && !exercise.getBodyPart().equals(part))
                continue;

            boolean matches =
                    normalize(exercise.getExerciseName())
                            .contains(keyword)
                            || normalize(exercise.getBodyPart())
                            .contains(keyword);

            if (!matches) continue;

            groups.computeIfAbsent(
                            record.getWorkoutDate(),
                            key -> new LinkedHashMap<>())
                    .computeIfAbsent(
                            exercise.getBodyPart(),
                            key -> new ArrayList<>())
                    .add(record);

            visibleIds.add(record.getRecordId());
        }

        int dayCount = 0;

        for (var day : groups.entrySet()) {

            if (dayCount > 0) {

                model.addElement(new Row(
                        3, "", "", null, false, false));
            }

            dayCount++;

            LocalDate workoutDate = day.getKey();

            String title = workoutDate.format(
                    DateTimeFormatter.ofPattern(
                            workoutDate.getYear()
                                    == LocalDate.now().getYear()
                                    ? "M월 d일 EEEE"
                                    : "yyyy년 M월 d일 EEEE",
                            Locale.KOREAN));

            int totalExercises = day.getValue()
                    .values()
                    .stream()
                    .mapToInt(List::size)
                    .sum();

            int totalSets = day.getValue()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .mapToInt(record -> setCounts.getOrDefault(
                            record.getRecordId(), 0))
                    .sum();

            Row dayRow = new Row(
                    0, title,
                    totalSets + "세트",
                    null, true, false);
            dayIds.put(dayRow, day.getValue().values().stream()
                    .flatMap(Collection::stream)
                    .map(WorkoutRecord::getRecordId)
                    .toList());
            model.addElement(dayRow);

            int current = 0;

            for (var group : day.getValue().entrySet()) {

                List<WorkoutRecord> items = group.getValue();

                model.addElement(new Row(
                        1, group.getKey(),
                        items.size() + "종목",
                        null, false, false));

                for (WorkoutRecord record : items) {

                    current++;

                    Exercise exercise = exercises.get(
                            record.getExerciseId());

                    int sets = setCounts.getOrDefault(
                            record.getRecordId(), 0);

                    model.addElement(new Row(
                            2,
                            exercise.getExerciseName(),
                            sets == 0 ? "미기록" : sets + "세트",
                            record,
                            false,
                            current == totalExercises));
                }
            }
        }

        if (visibleIds.isEmpty()) {

            model.addElement(new Row(
                    4,
                    records.isEmpty()
                            ? "아직 운동 기록이 없어요."
                            : "일치하는 기록이 없어요.",
                    "", null, false, false));
        }

        list.clearSelection();

        updateFilterButton();
        updateSelection();
    }

    private void updateFilterButton() {

        int filters = 0;

        if (dateField.getDate() != null) filters++;

        if (partCombo.getSelectedIndex() > 0) filters++;

        filterButton.setText(
                filters == 0 ? "필터" : "필터 " + filters);

        boolean active =
                filters > 0 || filterPanel.isVisible();

        filterButton.setBackground(active ? TEXT : CARD);

        filterButton.setForeground(active ? BG : TEXT);
    }

    // 다중 선택 및 삭제

    private void setSelectionMode(boolean enabled) {

        selectionMode = enabled;

        selected.clear();
        list.clearSelection();

        selectButton.setText(enabled ? "취소" : "선택");

        allButton.setVisible(enabled);

        deletePanel.setVisible(enabled);

        updateSelection();
    }

    private void toggleAll() {

        if (visibleIds.isEmpty()) return;

        boolean all = selected.size() == visibleIds.size();

        selected.clear();

        if (!all)
            selected.addAll(visibleIds);

        updateSelection();
    }

    private void updateSelection() {

        long days = 0;

        for (int i = 0; i < model.size(); i++) {

            if (model.get(i).type() == 0) days++;
        }

        count.setText(selectionMode
                ? (selected.isEmpty() ? "날짜 또는 종목을 선택하세요"
                   : selected.size() + "개 선택됨")
                : days + "일 · " + visibleIds.size() + "종목");
        count.setToolTipText(selectionMode
                ? "날짜를 선택하면 현재 표시된 해당 날짜의 종목이 모두 선택됩니다."
                : null);

        allButton.setText(
                !selected.isEmpty()
                        && selected.size() == visibleIds.size()
                        ? "선택 해제" : "전체 선택");

        allButton.setEnabled(!visibleIds.isEmpty());

        deleteButton.setText(selected.isEmpty()
                ? "삭제할 기록 선택"
                : selected.size() + "개 기록 삭제");

        deleteButton.setEnabled(!selected.isEmpty());

        list.repaint();

        revalidate();
        repaint();
    }

    private void deleteSelected() {

        if (selected.isEmpty()) return;

        long selectedDays = records.stream()
                .filter(record -> selected.contains(record.getRecordId()))
                .map(WorkoutRecord::getWorkoutDate)
                .distinct().count();

        if (!AppDialogs.confirm(
                this,
                "운동 기록 삭제",
                selectedDays + "일의 선택된 운동 기록 " + selected.size()
                        + "개와 포함된 모든 세트를 삭제할까요?",
                "취소", "삭제", true)) return;

        int deleted = 0;

        try {

            for (long id : new ArrayList<>(selected)) {

                controller.deleteRecord(id);

                records.removeIf(
                        record -> record.getRecordId() == id);

                setCounts.remove(id);

                deleted++;
            }

        } catch (SQLException ex) {

            error(deleted + "개 기록 삭제 완료.\n"
                    + "나머지 삭제 중 오류가 발생했습니다.\n"
                    + ex.getMessage());
        }

        selectedRecordId = null;

        setSelectionMode(false);

        refresh();
    }

    // 공통 UI

    private static String normalize(String value) {

        return value == null ? "" : value.trim()
                                    .toLowerCase(Locale.ROOT)
                                    .replaceAll("\\s+", "");
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

        button.setFont(AppTheme.bold(13));

        button.setBackground(primary ? TEXT : CARD);

        button.setForeground(primary ? BG : TEXT);

        button.setBorderPainted(false);

        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        button.putClientProperty(
                "JButton.buttonType", "roundRect");

        return button;
    }

    private static JButton textButton(String text) {

        JButton button = new JButton(text);

        button.setFont(AppTheme.regular(13));

        button.setForeground(MUTED);

        button.setContentAreaFilled(false);

        button.setBorderPainted(false);

        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        return button;
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

            if (component instanceof JComponent jc)
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    private static void watch(
            JTextField field, Runnable action) {

        field.getDocument().addDocumentListener(
                new DocumentListener() {

                    public void insertUpdate(DocumentEvent e) {
                        action.run();
                    }

                    public void removeUpdate(DocumentEvent e) {
                        action.run();
                    }

                    public void changedUpdate(DocumentEvent e) {
                        action.run();
                    }
                });
    }

    private void error(String message) {

        JOptionPane.showMessageDialog(
                this, message,
                "운동 기록 확인",
                JOptionPane.WARNING_MESSAGE);
    }

    // 선택 아이콘

    private static class SelectionIcon implements Icon {

        private final boolean checked;
        private final boolean partial;

        SelectionIcon(boolean checked) {
            this(checked, false);
        }

        SelectionIcon(boolean checked, boolean partial) {
            this.checked = checked;
            this.partial = partial;
        }

        public int getIconWidth() { return 20; }

        public int getIconHeight() { return 20; }

        public void paintIcon(
                Component c, Graphics graphics, int x, int y) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.translate(x, y);

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setStroke(new BasicStroke(
                    1.6f,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));

            if (partial) {
                g.setColor(TEXT);
                g.fillOval(1, 1, 18, 18);
                g.setColor(BG);
                g.drawLine(5, 10, 15, 10);
            } else if (checked) {

                g.setColor(TEXT);
                g.fillOval(1, 1, 18, 18);

                g.setColor(BG);

                g.drawLine(5, 10, 9, 14);
                g.drawLine(9, 14, 15, 7);

            } else {

                g.setColor(MUTED);
                g.drawOval(1, 1, 18, 18);
            }

            g.dispose();
        }
    }

    // 둥근 필터 카드

    private static class RoundedPanel extends JPanel {

        final Color color;

        RoundedPanel(Color color) {

            this.color = color;

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

            g.fillRoundRect(0, 0,
                    getWidth(), getHeight(),
                    CARD_ARC, CARD_ARC);

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

    // 하루 전체를 하나의 카드처럼 표시

    private static class GroupRow extends JPanel {

        final Color color;
        final boolean first;
        final boolean last;

        GroupRow(Color color, boolean first, boolean last) {

            this.color = color;
            this.first = first;
            this.last = last;

            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            g.setColor(color);

            g.fillRoundRect(
                    0, 0, width, height,
                    CARD_ARC, CARD_ARC);

            if (!first)
                g.fillRect(0, 0, width, height / 2);

            if (!last)
                g.fillRect(
                        0, height / 2,
                        width, height - height / 2);

            if (first) {

                g.setColor(LINE);

                g.drawLine(
                        14, height - 1,
                        width - 14, height - 1);
            }

            g.dispose();

            super.paintComponent(graphics);
        }
    }
}