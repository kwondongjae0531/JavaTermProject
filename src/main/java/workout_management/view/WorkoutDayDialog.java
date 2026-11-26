package workout_management.view;

import workout_management.controller.WorkoutController;
import workout_management.domain.*;
import workout_management.util.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static workout_management.util.AppTheme.*;

public class WorkoutDayDialog extends JPanel {

    private static final String[] PARTS = {
            "전체", "가슴", "등", "하체", "어깨", "팔", "복근"
    };

    private static final Set<String> BODYWEIGHT = Set.of(
            "풀업", "턱걸이", "친업", "딥스",
            "푸시업", "푸쉬업", "팔굽혀펴기",
            "행잉레그레이즈", "레그레이즈",
            "ab슬라이드", "ab롤아웃",
            "맨몸스쿼트", "맨몸딥스", "맨몸풀업",
            "중량딥스", "중량풀업", "중량턱걸이"
    );

    private static final Color SAVED_FIELD =
            new Color(44, 47, 52);

    private static final Color ACTIVE_FIELD =
            new Color(63, 67, 73);

    private static final Color SAVED_GREEN =
            new Color(38, 92, 70);

    private static String lastSelectedPart = "전체";

    private final JFrame frameOwner;
    private final WorkoutController controller;
    private final LocalDate date;
    private final Runnable onFinish;

    private final ArrayList<Exercise> exercises;
    private final ArrayList<WorkoutRecord> records;
    private final ArrayList<ExerciseCard> exerciseCards =
            new ArrayList<>();

    private final CardLayout layout = new CardLayout();
    private final JPanel screens = new JPanel(layout);
    private final ScrollableCards cards = new ScrollableCards();

    private final JLabel exerciseCount = bold("0", 23, TEXT);
    private final JLabel setCount = bold("0", 23, TEXT);
    private final JLabel volumeCount = bold("0", 23, TEXT);

    private final JTextField search = new JTextField();
    private final JLabel resultCount = label("", 12, MUTED);

    private final DefaultListModel<PickerRow> model =
            new DefaultListModel<>();

    private final JList<PickerRow> exerciseList =
            new JList<>(model);

    private final Map<String, JButton> partButtons =
            new LinkedHashMap<>();

    private final LinkedHashSet<Long> recentIds =
            new LinkedHashSet<>();

    private final Map<String, Routine> routines =
            new HashMap<>();

    private final JButton routineButton =
            button("이전 운동 불러오기", false);

    private final JButton missingButton =
            button("+ 새 운동 만들기", false);

    private final JTextField newName = new JTextField();

    private final JComboBox<String> newPart =
            new JComboBox<>(new String[]{
                    "가슴", "등", "하체", "어깨", "팔", "복근"
            });

    private final JLabel newError = label(" ", 12, RED);

    private String selectedPart = lastSelectedPart;
    private boolean registering;

    private record PickerRow(Exercise exercise, String section) {

        @Override
        public String toString() {
            return exercise == null
                    ? section : exercise.getExerciseName();
        }
    }

    private record Input(
            BigDecimal weight, int reps, Integer rest) {}

    private record Routine(
            LocalDate date, List<Exercise> exercises) {}

    public WorkoutDayDialog(
            JFrame owner,
            WorkoutController controller,
            LocalDate date,
            Runnable onFinish) throws SQLException {

        super(new BorderLayout());

        this.frameOwner = owner;
        this.controller = controller;
        this.date = date;
        this.onFinish = onFinish;

        setBackground(BG);

        exercises = controller.getExercises();
        records = controller.getRecords();

        screens.setBackground(BG);

        screens.add(createWorkoutPage(), "WORKOUT");
        screens.add(createPickerPage(), "EXERCISES");
        screens.add(createNewExercisePage(), "NEW");

        add(screens, BorderLayout.CENTER);

        showScreen("WORKOUT");
    }

    public static boolean isBodyweight(String name) {
        return BODYWEIGHT.contains(normalize(name));
    }

    public static String formatWeight(
            BigDecimal weight, boolean bodyweight) {

        String value = weight.stripTrailingZeros().toPlainString();

        if (bodyweight)
            return weight.signum() == 0 ? "맨몸" : "+" + value;

        return value;
    }

    private static String normalize(String text) {

        return text == null ? "" : text.trim()
                                   .toLowerCase(Locale.ROOT)
                                   .replaceAll("\\s+", "");
    }

    // 운동 입력 화면

    private JPanel createWorkoutPage() throws SQLException {

        JPanel page = new JPanel(new BorderLayout(0, 12));
        page.setBackground(BG);

        JPanel top = vertical();

        top.add(header(
                date.equals(LocalDate.now())
                        ? "오늘의 운동" : "운동 기록",
                date.format(DateTimeFormatter.ofPattern("yy.MM.dd")),
                onFinish));

        top.add(Box.createVerticalStrut(12));

        RoundedPanel summary = new RoundedPanel(CARD, CARD_ARC);

        summary.setLayout(new GridLayout(1, 3, 8, 0));
        summary.setBorder(new EmptyBorder(14, 14, 14, 14));

        summary.add(metric("종목", exerciseCount));
        summary.add(metric("세트", setCount));
        summary.add(metric("운동량", volumeCount));

        volumeCount.setToolTipText("입력 중량 × 반복 횟수의 합계");

        top.add(summary);
        alignLeft(top);

        page.add(top, BorderLayout.NORTH);

        for (WorkoutRecord record : records) {

            if (!record.getWorkoutDate().equals(date)) continue;

            Exercise exercise = findExercise(record.getExerciseId());

            if (exercise != null)
                exerciseCards.add(new ExerciseCard(exercise, record));
        }

        rebuildCards();
        refreshSummary();

        page.add(scroll(cards), BorderLayout.CENTER);

        JPanel bottom = vertical();

        JButton add = button("+ 운동 추가", false);
        fullWidth(add, 44);

        add.addActionListener(e ->
                runAction(this::openPicker));

        JButton finish = button("운동 완료", true);
        fullWidth(finish, 48);

        finish.addActionListener(e -> runAction(() -> {

            savePending();
            onFinish.run();
        }));

        bottom.add(add);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(finish);

        page.add(bottom, BorderLayout.SOUTH);

        return page;
    }

    // 운동 선택 화면

    private JPanel createPickerPage() {

        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.setBackground(BG);

        JPanel top = vertical();

        JPanel heading = header(
                "운동 선택", "",
                () -> showScreen("WORKOUT"));

        JButton create = textButton("+ 새 운동");

        create.setPreferredSize(new Dimension(90, 44));
        create.addActionListener(e -> openNewExercise());

        heading.add(create, BorderLayout.EAST);

        top.add(heading);
        top.add(Box.createVerticalStrut(10));

        styleField(search);

        search.putClientProperty(
                "JTextField.placeholderText", "운동 검색");

        search.putClientProperty(
                "JTextField.showClearButton", true);

        fullWidth(search, 44);

        top.add(search);
        top.add(Box.createVerticalStrut(10));

        JPanel filters = new JPanel(new GridLayout(1, 7, 4, 0));

        filters.setOpaque(false);

        fullWidth(filters, 38);

        for (String part : PARTS) {

            JButton chip = button(part, false);

            chip.setFont(AppTheme.regular(12));
            chip.setMargin(new Insets(0, 0, 0, 0));

            chip.addActionListener(e -> {

                selectedPart = part;
                lastSelectedPart = part;

                filterExercises();
            });

            partButtons.put(part, chip);
            filters.add(chip);
        }

        top.add(filters);
        top.add(Box.createVerticalStrut(8));

        // 이전 운동 종목을 한 번에 불러오기

        fullWidth(routineButton, 42);

        routineButton.setBackground(FIELD);
        routineButton.setFont(AppTheme.bold(13));

        routineButton.addActionListener(e ->
                runAction(this::loadPreviousRoutine));

        top.add(routineButton);

        JPanel countRow = new JPanel(new BorderLayout());

        countRow.setOpaque(false);

        JButton manage = textButton("내 운동 관리 ›");

        manage.setPreferredSize(new Dimension(110, 36));

        manage.addActionListener(e ->
                runAction(this::manageExercises));

        countRow.add(resultCount, BorderLayout.WEST);
        countRow.add(manage, BorderLayout.EAST);

        top.add(countRow);

        alignLeft(top);

        page.add(top, BorderLayout.NORTH);

        exerciseList.setBackground(BG);
        exerciseList.setForeground(TEXT);

        exerciseList.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        exerciseList.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        exerciseList.setCellRenderer(
                (list, item, index, selected, focused) -> {

                    if (item.exercise() == null) {

                        JLabel title = bold(item.section(), 12, MUTED);

                        title.setBorder(new EmptyBorder(10, 4, 6, 4));

                        title.setPreferredSize(new Dimension(300, 32));

                        return title;
                    }

                    Exercise exercise = item.exercise();

                    JPanel wrapper = new JPanel(new BorderLayout());

                    wrapper.setBackground(BG);
                    wrapper.setBorder(new EmptyBorder(2, 0, 2, 0));
                    wrapper.setPreferredSize(new Dimension(300, 58));

                    RoundedPanel card = new RoundedPanel(
                            selected ? FIELD : CARD, 20);

                    card.setLayout(new BorderLayout(8, 0));
                    card.setBorder(new EmptyBorder(9, 14, 9, 14));

                    JPanel info = vertical();

                    info.add(bold(exercise.getExerciseName(), 15, TEXT));
                    info.add(Box.createVerticalStrut(3));

                    String description = exercise.getBodyPart();

                    if (exercise.getRegisteredMemberId() != null)
                        description += " · 내 운동";

                    boolean added = findCard(exercise.getExerciseId()) != null;

                    if (added) description += " · 추가됨";

                    info.add(label(description, 12, MUTED));

                    card.add(info, BorderLayout.CENTER);

                    card.add(centered(added ? "✓" : "+", 20,
                            added ? GREEN : MUTED), BorderLayout.EAST);

                    wrapper.add(card);

                    return wrapper;
                });

        exerciseList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (!SwingUtilities.isLeftMouseButton(e)
                        || e.getClickCount() != 1) return;

                int index = exerciseList.locationToIndex(e.getPoint());

                if (index < 0) return;

                Rectangle bounds =
                        exerciseList.getCellBounds(index, index);

                if (bounds != null && bounds.contains(e.getPoint()))
                    activatePickerRow(model.get(index));
            }
        });

        exerciseList.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), "selectExercise");

        exerciseList.getActionMap().put(
                "selectExercise", new AbstractAction() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        activatePickerRow(exerciseList.getSelectedValue());
                    }
                });

        search.addActionListener(e -> {

            List<Exercise> matches = Collections.list(model.elements())
                    .stream()
                    .map(PickerRow::exercise)
                    .filter(Objects::nonNull)
                    .toList();

            if (matches.size() == 1) {

                runAction(() -> addExercise(matches.get(0)));

            } else if (matches.isEmpty()
                    && !search.getText().isBlank()) {

                openNewExercise();

            } else {

                for (int i = 0; i < model.size(); i++) {

                    if (model.get(i).exercise() != null) {

                        exerciseList.setSelectedIndex(i);
                        exerciseList.requestFocusInWindow();

                        break;
                    }
                }
            }
        });

        watch(search, this::filterExercises);

        page.add(scroll(exerciseList), BorderLayout.CENTER);

        missingButton.setPreferredSize(new Dimension(300, 44));

        missingButton.addActionListener(e -> openNewExercise());

        page.add(missingButton, BorderLayout.SOUTH);

        filterExercises();

        return page;
    }

    private void activatePickerRow(PickerRow item) {

        if (item != null && item.exercise() != null)
            runAction(() -> addExercise(item.exercise()));
    }

    // 최근 운동과 이전 루틴 조회

    private void loadPickerData() throws SQLException {

        recentIds.clear();
        routines.clear();

        ArrayList<WorkoutRecord> sorted =
                new ArrayList<>(records);

        sorted.sort(Comparator
                .comparing(WorkoutRecord::getWorkoutDate)
                .reversed()
                .thenComparingLong(WorkoutRecord::getRecordId));

        for (WorkoutRecord record : sorted) {

            LocalDate workoutDate = record.getWorkoutDate();

            if (record.getRecordId() == 0
                    || workoutDate.isAfter(date)) continue;

            Exercise exercise = findExercise(record.getExerciseId());

            if (exercise == null) continue;

            boolean recentNeeded = recentIds.size() < 5
                    && !recentIds.contains(exercise.getExerciseId());

            boolean earlier = workoutDate.isBefore(date);

            boolean allNeeded = earlier
                    && needsRoutine("전체", workoutDate);

            boolean partNeeded = earlier
                    && needsRoutine(exercise.getBodyPart(), workoutDate);

            if (!recentNeeded && !allNeeded && !partNeeded)
                continue;

            if (controller.getSets(record.getRecordId()).isEmpty())
                continue;

            if (recentNeeded)
                recentIds.add(exercise.getExerciseId());

            if (allNeeded)
                addRoutineExercise("전체", workoutDate, exercise);

            if (partNeeded)
                addRoutineExercise(
                        exercise.getBodyPart(), workoutDate, exercise);
        }
    }

    private boolean needsRoutine(String part, LocalDate workoutDate) {

        Routine routine = routines.get(part);

        return routine == null || routine.date().equals(workoutDate);
    }

    private void addRoutineExercise(
            String part, LocalDate date, Exercise exercise) {

        Routine routine = routines.computeIfAbsent(part,
                key -> new Routine(date, new ArrayList<>()));

        boolean exists = routine.exercises().stream().anyMatch(
                e -> e.getExerciseId() == exercise.getExerciseId());

        if (!exists) routine.exercises().add(exercise);
    }

    private void updateRoutineButton() {

        Routine routine = routines.get(selectedPart);

        if (routine == null || !search.getText().isBlank()) {

            routineButton.setVisible(false);
            return;
        }

        long count = routine.exercises().stream()
                .filter(e -> findCard(e.getExerciseId()) == null)
                .count();

        if (count == 0) {

            routineButton.setVisible(false);
            return;
        }

        String name = selectedPart.equals("전체")
                ? "이전 운동" : "이전 " + selectedPart;

        String day = routine.date().format(
                DateTimeFormatter.ofPattern("M/d"));

        routineButton.setText(
                name + " 불러오기 · " + day + " · " + count + "종목");

        routineButton.setVisible(true);
    }

    private void loadPreviousRoutine() throws SQLException {

        Routine routine = routines.get(selectedPart);

        if (routine == null) return;

        ArrayList<ExerciseCard> additions = new ArrayList<>();

        for (Exercise exercise : routine.exercises()) {

            if (findCard(exercise.getExerciseId()) != null)
                continue;

            WorkoutRecord record =
                    controller.findDailyRecord(exercise, date);

            if (record == null) {

                record = new WorkoutRecord(
                        0, 0, exercise.getExerciseId(), date);
            }

            additions.add(new ExerciseCard(exercise, record));
        }

        if (additions.isEmpty()) return;

        // 기존에 추가된 운동은 유지

        for (ExerciseCard card : additions) {

            exerciseCards.add(card);

            if (card.record.getRecordId() != 0
                    && records.stream().noneMatch(r ->
                    r.getRecordId() == card.record.getRecordId())) {

                records.add(card.record);
            }
        }

        rebuildCards();
        refreshSummary();

        showScreen("WORKOUT");

        SwingUtilities.invokeLater(() ->
                focusCard(additions.get(0)));
    }

    // 운동 목록 검색

    private void filterExercises() {

        String keyword = normalize(search.getText());

        ArrayList<Exercise> matches = new ArrayList<>();

        for (Exercise exercise : exercises) {

            boolean partMatch = selectedPart.equals("전체")
                    || selectedPart.equals(exercise.getBodyPart());

            boolean nameMatch =
                    normalize(exercise.getExerciseName())
                            .contains(keyword);

            if (partMatch && nameMatch) matches.add(exercise);
        }

        matches.sort(Comparator.comparing(
                Exercise::getExerciseName));

        model.clear();

        Set<Long> shown = new HashSet<>();

        ArrayList<Exercise> recent = new ArrayList<>();

        for (long id : recentIds) {

            for (Exercise exercise : matches) {

                if (exercise.getExerciseId() == id) {

                    recent.add(exercise);
                    shown.add(id);
                    break;
                }
            }
        }

        if (!recent.isEmpty()) {

            model.addElement(new PickerRow(null, "최근 운동"));

            for (Exercise exercise : recent)
                model.addElement(new PickerRow(exercise, ""));
        }

        boolean headingAdded = false;

        for (Exercise exercise : matches) {

            if (shown.contains(exercise.getExerciseId())) continue;

            if (!headingAdded) {

                model.addElement(new PickerRow(null,
                        recent.isEmpty() ? "운동 목록" : "다른 운동"));

                headingAdded = true;
            }

            model.addElement(new PickerRow(exercise, ""));
        }

        resultCount.setText(matches.isEmpty()
                ? "일치하는 운동이 없어요."
                : matches.size() + "개 운동");

        missingButton.setVisible(
                matches.isEmpty() && !keyword.isEmpty());

        for (var entry : partButtons.entrySet()) {

            boolean active =
                    entry.getKey().equals(selectedPart);

            entry.getValue().setBackground(active ? TEXT : CARD);

            entry.getValue().setForeground(active ? BG : MUTED);
        }

        updateRoutineButton();

        revalidate();
        repaint();
    }

    private void reloadExercises() throws SQLException {

        ArrayList<Exercise> updated = controller.getExercises();

        exercises.clear();
        exercises.addAll(updated);
    }

    private void openPicker() throws SQLException {

        reloadExercises();
        loadPickerData();

        selectedPart = lastSelectedPart;

        search.setText("");

        filterExercises();
        exerciseList.clearSelection();

        showScreen("EXERCISES");

        if (!model.isEmpty())
            exerciseList.ensureIndexIsVisible(0);
    }

    private void manageExercises() throws SQLException {

        Set<Long> activeIds = new HashSet<>();

        for (ExerciseCard card : exerciseCards)
            activeIds.add(card.exercise.getExerciseId());

        ExerciseManagementDialog dialog =
                new ExerciseManagementDialog(
                        frameOwner, controller, activeIds);

        dialog.setVisible(true);

        reloadExercises();
        loadPickerData();
        filterExercises();
    }

    // 개인 운동 등록

    private JPanel createNewExercisePage() {

        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BG);

        page.add(header("새 운동", "",
                () -> showScreen("EXERCISES")), BorderLayout.NORTH);

        JPanel form = vertical();

        form.add(label("운동 이름", 13, MUTED));
        form.add(Box.createVerticalStrut(8));

        styleField(newName);

        newName.putClientProperty(
                "JTextField.placeholderText", "예: 해머 하이로우");

        fullWidth(newName, 46);

        form.add(newName);
        form.add(Box.createVerticalStrut(18));

        form.add(label("운동 부위", 13, MUTED));
        form.add(Box.createVerticalStrut(8));

        newPart.setBackground(CARD);
        newPart.setForeground(TEXT);
        newPart.setFont(AppTheme.body());

        newPart.putClientProperty("JComponent.roundRect", true);

        fullWidth(newPart, 44);

        form.add(newPart);
        form.add(Box.createVerticalStrut(8));
        form.add(newError);

        alignLeft(form);

        page.add(form, BorderLayout.CENTER);

        JButton create = button("운동 만들기", true);

        create.setPreferredSize(new Dimension(300, 48));

        create.addActionListener(e -> registerExercise());
        newName.addActionListener(e -> registerExercise());

        page.add(create, BorderLayout.SOUTH);

        return page;
    }

    private void openNewExercise() {

        newName.setText(search.getText().trim());
        newError.setText(" ");

        if (selectedPart.equals("전체"))
            newPart.setSelectedIndex(-1);
        else
            newPart.setSelectedItem(selectedPart);

        showScreen("NEW");

        SwingUtilities.invokeLater(
                newName::requestFocusInWindow);
    }

    private void registerExercise() {

        if (registering) return;

        registering = true;

        try {

            String name = newName.getText().trim();
            String part = (String) newPart.getSelectedItem();

            if (name.isBlank())
                throw new IllegalArgumentException("운동 이름을 입력하세요.");

            if (part == null)
                throw new IllegalArgumentException("운동 부위를 선택하세요.");

            controller.insertExercise(name, part);

            reloadExercises();

            Exercise created = exercises.stream()
                    .filter(e -> e.getExerciseName()
                            .equalsIgnoreCase(name)
                            && e.getBodyPart().equals(part))
                    .findFirst()
                    .orElseThrow(() -> new SQLException(
                            "등록한 운동을 불러오지 못했습니다."));

            selectedPart = part;
            lastSelectedPart = part;

            search.setText("");

            filterExercises();

            showScreen("EXERCISES");

            addExercise(created);

        } catch (IllegalArgumentException e) {

            newError.setText(e.getMessage());

        } catch (SQLException e) {

            showError(e.getMessage());

        } finally {

            registering = false;
        }
    }

    private void showScreen(String name) {

        layout.show(screens, name);

        screens.revalidate();
        screens.repaint();
    }

    // 운동 추가

    private void addExercise(Exercise exercise) throws SQLException {

        ExerciseCard existing = findCard(exercise.getExerciseId());

        if (existing != null) {

            showScreen("WORKOUT");

            SwingUtilities.invokeLater(() -> focusCard(existing));

            return;
        }

        WorkoutRecord record =
                controller.findDailyRecord(exercise, date);

        if (record == null) {

            record = new WorkoutRecord(
                    0, 0, exercise.getExerciseId(), date);

        } else {

            long id = record.getRecordId();

            if (records.stream().noneMatch(r -> r.getRecordId() == id))
                records.add(record);
        }

        ExerciseCard card = new ExerciseCard(exercise, record);

        exerciseCards.add(card);

        rebuildCards();
        refreshSummary();

        showScreen("WORKOUT");

        SwingUtilities.invokeLater(() -> focusCard(card));
    }

    private ExerciseCard findCard(long exerciseId) {

        for (ExerciseCard card : exerciseCards)
            if (card.exercise.getExerciseId() == exerciseId)
                return card;

        return null;
    }

    private Exercise findExercise(long id) {

        for (Exercise exercise : exercises)
            if (exercise.getExerciseId() == id)
                return exercise;

        return null;
    }

    public void focusRecord(long id) {

        if (id == 0) return;

        for (ExerciseCard card : exerciseCards) {

            if (card.record.getRecordId() == id) {

                focusCard(card);
                return;
            }
        }
    }

    private void focusCard(ExerciseCard card) {

        Rectangle bounds = card.getBounds();

        bounds.height = Math.min(bounds.height, 100);

        cards.scrollRectToVisible(bounds);
    }

    private void rebuildCards() {

        cards.removeAll();

        if (exerciseCards.isEmpty()) {

            RoundedPanel empty =
                    new RoundedPanel(CARD, CARD_ARC);

            empty.setLayout(
                    new BoxLayout(empty, BoxLayout.Y_AXIS));

            empty.setBorder(
                    new EmptyBorder(32, 12, 32, 12));

            JLabel icon = new JLabel(new NavigationIcon("WORKOUT"));

            icon.setForeground(MUTED);

            icon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel title = bold(
                    "운동을 시작해 보세요.", 15, TEXT);

            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            empty.add(icon);
            empty.add(Box.createVerticalStrut(12));
            empty.add(title);

            cards.add(empty);

        } else {

            for (ExerciseCard card : exerciseCards) {

                cards.add(card);
                cards.add(Box.createVerticalStrut(10));
            }
        }

        cards.revalidate();
        cards.repaint();
    }

    // 개별 운동 카드

    private class ExerciseCard extends RoundedPanel {

        final Exercise exercise;
        final WorkoutRecord record;
        final boolean bodyweight;

        final ArrayList<SetLine> lines = new ArrayList<>();
        final JPanel setPanel = vertical();

        WorkoutRecord previousRecord;

        ArrayList<SetRecord> previousSets = new ArrayList<>();

        ExerciseCard(
                Exercise exercise, WorkoutRecord record)
                throws SQLException {

            super(CARD, CARD_ARC);

            this.exercise = exercise;
            this.record = record;

            this.bodyweight =
                    isBodyweight(exercise.getExerciseName());

            setLayout(new BorderLayout(0, 8));
            setBorder(new EmptyBorder(12, 12, 12, 12));

            ArrayList<WorkoutRecord> candidates =
                    new ArrayList<>();

            for (WorkoutRecord item : records) {

                if (item.getExerciseId() == exercise.getExerciseId()
                        && item.getWorkoutDate().isBefore(date)) {

                    candidates.add(item);
                }
            }

            candidates.sort(Comparator
                    .comparing(WorkoutRecord::getWorkoutDate)
                    .thenComparingLong(WorkoutRecord::getRecordId)
                    .reversed());

            for (WorkoutRecord candidate : candidates) {

                ArrayList<SetRecord> sets =
                        controller.getSets(candidate.getRecordId());

                if (!sets.isEmpty()) {

                    previousRecord = candidate;
                    previousSets = sets;

                    break;
                }
            }

            JPanel top = vertical();

            JPanel titleRow = new JPanel(new BorderLayout());

            titleRow.setOpaque(false);

            titleRow.add(bold(exercise.getExerciseName(),
                    18, TEXT), BorderLayout.CENTER);

            JButton delete = textButton("");

            delete.setIcon(new TrashIcon());
            delete.setPreferredSize(new Dimension(40, 38));
            delete.setToolTipText("운동 삭제");

            delete.getAccessibleContext()
                    .setAccessibleName("운동 삭제");

            delete.addActionListener(e ->
                    runAction(this::deleteExercise));

            titleRow.add(delete, BorderLayout.EAST);

            top.add(titleRow);

            JPanel info = new JPanel(new BorderLayout());

            info.setOpaque(false);

            String description = exercise.getBodyPart();

            if (bodyweight) {

                description += " · 맨몸 / 중량";

            } else if (normalize(exercise.getExerciseName())
                    .contains("덤벨")) {

                description += " · 한 손 기준";
            }

            info.add(label(description, 12, MUTED),
                    BorderLayout.WEST);

            if (previousRecord != null) {

                JButton previous = textButton("이전 "
                        + previousRecord.getWorkoutDate().format(
                        DateTimeFormatter.ofPattern("M/d")) + " ›");

                previous.addActionListener(e ->
                        runAction(this::compare));

                info.add(previous, BorderLayout.EAST);
            }

            top.add(info);
            top.add(Box.createVerticalStrut(6));

            // 입력칸과 제목의 열 정렬 유지

            top.add(row(true,
                    centered("세트", 11, MUTED),
                    centered(bodyweight ? "추가 kg" : "kg",
                            11, MUTED),
                    centered("횟수", 11, MUTED),
                    centered("휴식", 11, MUTED),
                    centered("완료", 11, MUTED),
                    new JLabel("")));

            alignLeft(top);

            add(top, BorderLayout.NORTH);

            if (record.getRecordId() != 0) {

                for (SetRecord set :
                        controller.getSets(record.getRecordId())) {

                    lines.add(new SetLine(
                            this, set, false, false));
                }
            }

            ensureDraft();
            rebuildLines();

            add(setPanel, BorderLayout.CENTER);
        }

        SetRecord previousSet(int number) {

            for (SetRecord set : previousSets)
                if (set.getSetNo() == number)
                    return set;

            return null;
        }

        // 다음 세트 자동 준비

        void ensureDraft() {

            for (SetLine line : lines)
                if (!line.saved) return;

            int number = lines.stream()
                    .mapToInt(line -> line.setNo)
                    .max().orElse(0) + 1;

            if (number > 9999) return;

            BigDecimal weight = BigDecimal.ZERO;

            int reps = 1;

            boolean filled = false;

            if (!lines.isEmpty()) {

                SetLine last = lines.get(lines.size() - 1);

                weight = last.savedWeight;
                reps = last.savedReps;

                filled = true;

            } else {

                SetRecord previous = previousSet(number);

                if (previous != null) {

                    weight = previous.getWeightKg();
                    reps = previous.getReps();

                    filled = true;
                }
            }

            SetRecord draft = new SetRecord(
                    0, record.getRecordId(), number,
                    weight, reps, null);

            lines.add(new SetLine(
                    this, draft,
                    !bodyweight && !filled, !filled));
        }

        void rebuildLines() {

            setPanel.removeAll();

            for (SetLine line : lines) {

                setPanel.add(line.panel);

                setPanel.add(Box.createVerticalStrut(5));
            }

            setPanel.revalidate();
            setPanel.repaint();

            cards.revalidate();
            cards.repaint();
        }

        void compare() throws SQLException {

            if (previousRecord == null) return;

            List<SetRecord> current = record.getRecordId() == 0
                    ? List.of()
                    : controller.getSets(record.getRecordId());

            ComparisonDialog dialog = new ComparisonDialog(
                    frameOwner,
                    exercise.getExerciseName(),
                    previousRecord,
                    controller.getSets(previousRecord.getRecordId()),
                    record,
                    current);

            dialog.setVisible(true);
        }

        void deleteExercise() throws SQLException {

            boolean persisted = record.getRecordId() != 0;

            boolean changed = lines.stream().anyMatch(l -> l.dirty);

            if (persisted || changed) {

                if (!AppDialogs.confirm(
                        WorkoutDayDialog.this,
                        "운동 삭제",
                        persisted
                                ? "이 운동의 기록을 삭제할까요?"
                                : "입력 중인 운동을 제거할까요?",
                        "취소", "삭제", true)) return;
            }

            if (persisted) {

                long id = record.getRecordId();

                controller.deleteRecord(id);

                records.removeIf(r -> r.getRecordId() == id);
            }

            exerciseCards.remove(this);

            rebuildCards();
            refreshSummary();
        }
    }

    // 세트 입력 및 저장

    private class SetLine {

        final ExerciseCard owner;
        final int setNo;

        long setId;

        boolean saved;
        boolean dirty;

        BigDecimal savedWeight;
        int savedReps;

        String initialWeight;
        String initialReps;
        String initialRest;

        final JTextField weight;
        final JTextField reps;
        final JTextField rest;

        final JLabel setNumber;
        final JLabel previousHint;

        final JButton complete;
        final JButton delete;
        final JPanel panel;

        SetLine(ExerciseCard owner, SetRecord set,
                boolean blankWeight, boolean blankReps) {

            this.owner = owner;

            setNo = set.getSetNo();
            setId = set.getSetRecordId();

            saved = setId != 0;

            savedWeight = set.getWeightKg();
            savedReps = set.getReps();

            weight = numberField(blankWeight ? "" :
                    formatWeight(savedWeight, owner.bodyweight));

            reps = numberField(blankReps ? "" :
                    String.valueOf(savedReps));

            rest = numberField(
                    formatRest(set.getRestSeconds()));

            rest.setFont(AppTheme.regular(13));

            weight.putClientProperty(
                    "JTextField.placeholderText",
                    owner.bodyweight ? "맨몸" : "kg");

            reps.putClientProperty(
                    "JTextField.placeholderText", "횟수");

            rest.putClientProperty(
                    "JTextField.placeholderText", "—");

            weight.getAccessibleContext()
                    .setAccessibleName(setNo + "세트 중량");

            reps.getAccessibleContext()
                    .setAccessibleName(setNo + "세트 횟수");

            rest.getAccessibleContext()
                    .setAccessibleName(setNo + "세트 휴식시간");

            rest.setToolTipText("선택 입력: 2:00 또는 120초");

            setNumber = centered(
                    String.valueOf(setNo), 13, TEXT);

            complete = button("○", false);

            complete.setFont(AppTheme.bold(18));
            complete.setMargin(new Insets(0, 0, 0, 0));

            complete.addActionListener(e ->
                    runAction(() -> save(readInput(), true)));

            delete = textButton("×");

            delete.setFont(AppTheme.regular(22));
            delete.setMargin(new Insets(0, 0, 0, 0));

            delete.setToolTipText("세트 삭제");

            delete.getAccessibleContext()
                    .setAccessibleName(setNo + "세트 삭제");

            delete.addActionListener(e ->
                    runAction(this::delete));

            panel = vertical();

            panel.add(row(false,
                    setNumber, weight, reps, rest, complete, delete));

            SetRecord previous = owner.previousSet(setNo);

            if (previous != null) {

                String value = formatWeight(
                        previous.getWeightKg(), owner.bodyweight);

                String unit = owner.bodyweight
                        && previous.getWeightKg().signum() == 0
                        ? "" : " kg";

                previousHint = label("이전 " + value + unit
                        + " × " + previous.getReps() + "회", 11, MUTED);

            } else {

                previousHint = label("", 11, MUTED);
            }

            previousHint.setBorder(
                    new EmptyBorder(2, 30, 2, 0));

            panel.add(previousHint);

            remember();

            watch(weight, this::changed);
            watch(reps, this::changed);
            watch(rest, this::changed);

            weight.addActionListener(e ->
                    reps.requestFocusInWindow());

            // 횟수 입력 후 Enter로 바로 저장

            reps.addActionListener(e ->
                    runAction(() -> save(readInput(), true)));

            rest.addActionListener(e ->
                    runAction(() -> save(readInput(), true)));

            updateAppearance();
        }

        void remember() {

            initialWeight = weight.getText();
            initialReps = reps.getText();
            initialRest = rest.getText();

            dirty = false;
        }

        void changed() {

            dirty = !initialWeight.equals(weight.getText())
                    || !initialReps.equals(reps.getText())
                    || !initialRest.equals(rest.getText());

            updateAppearance();
        }

        // 완료 세트와 현재 세트의 시각적 구분

        void updateAppearance() {

            boolean completed = saved && !dirty;

            Color fieldColor =
                    completed ? SAVED_FIELD : ACTIVE_FIELD;

            Color textColor =
                    completed ? MUTED : TEXT;

            for (JTextField field :
                    new JTextField[]{weight, reps, rest}) {

                field.setBackground(fieldColor);
                field.setForeground(textColor);
            }

            weight.setFont(completed
                    ? AppTheme.regular(15) : AppTheme.bold(15));

            reps.setFont(completed
                    ? AppTheme.regular(15) : AppTheme.bold(15));

            rest.setFont(AppTheme.regular(13));

            setNumber.setForeground(textColor);

            complete.setBackground(
                    completed ? SAVED_GREEN : ACTIVE_FIELD);

            complete.setForeground(
                    completed ? GREEN : TEXT);

            complete.setText(completed ? "✓" : "○");

            String action = completed
                    ? "저장 완료" : "세트 완료 및 저장";

            complete.setToolTipText(action);

            complete.getAccessibleContext()
                    .setAccessibleName(setNo + "세트 " + action);

            // 자동 생성된 미수행 세트의 삭제는 불필요

            delete.setEnabled(saved || dirty);

            // 현재 세트 아래에만 이전 운동 기록 표시

            previousHint.setVisible(!saved
                    && !previousHint.getText().isBlank());

            panel.revalidate();
            panel.repaint();
        }

        Input readInput() {

            try {

                String value = weight.getText().trim();

                BigDecimal kg;

                if (owner.bodyweight &&
                        (value.isEmpty() || value.equals("맨몸")
                                || value.equalsIgnoreCase("BW"))) {

                    kg = BigDecimal.ZERO;

                } else {

                    kg = new BigDecimal(value);
                }

                int count = Integer.parseInt(reps.getText().trim());

                if (kg.signum() < 0
                        || kg.compareTo(
                        new BigDecimal("99999.99")) > 0
                        || kg.stripTrailingZeros().scale() > 2
                        || count < 1 || count > 99999) {

                    throw new IllegalArgumentException();
                }

                return new Input(
                        kg, count, parseRest(rest.getText()));

            } catch (IllegalArgumentException e) {

                throw new IllegalArgumentException(
                        owner.exercise.getExerciseName()
                                + " " + setNo
                                + "세트의 입력값을 확인하세요.");
            }
        }

        long resolveId() throws SQLException {

            if (setId != 0) return setId;

            for (SetRecord set :
                    controller.getSets(owner.record.getRecordId())) {

                if (set.getSetNo() == setNo) {

                    setId = set.getSetRecordId();

                    return setId;
                }
            }

            throw new SQLException("저장된 세트를 찾을 수 없습니다.");
        }

        void save(Input input, boolean createNext)
                throws SQLException {

            if (saved && !dirty) return;

            boolean newSet = !saved;

            SetRecord set = new SetRecord(
                    saved ? resolveId() : 0,
                    owner.record.getRecordId(),
                    setNo,
                    input.weight(),
                    input.reps(),
                    input.rest());

            if (owner.record.getRecordId() == 0) {

                long id = controller.insertFirstSet(
                        owner.exercise, date, set);

                owner.record.setRecordId(id);

                records.add(owner.record);

            } else if (saved) {

                controller.updateSet(set);

            } else {

                controller.insertSet(set);
            }

            saved = true;

            savedWeight = input.weight();
            savedReps = input.reps();

            weight.setText(
                    formatWeight(savedWeight, owner.bodyweight));

            reps.setText(String.valueOf(savedReps));

            rest.setText(formatRest(input.rest()));

            remember();
            updateAppearance();

            refreshSummary();

            if (newSet && createNext) {

                owner.ensureDraft();
                owner.rebuildLines();

                SwingUtilities.invokeLater(() -> {

                    if (!owner.lines.isEmpty()) {

                        SetLine next = owner.lines.get(
                                owner.lines.size() - 1);

                        if (next.panel.getParent() == null) return;

                        Rectangle bounds =
                                SwingUtilities.convertRectangle(
                                        next.panel.getParent(),
                                        next.panel.getBounds(),
                                        cards);

                        cards.scrollRectToVisible(bounds);
                    }
                });
            }
        }

        void delete() throws SQLException {

            if (saved || dirty) {

                if (!AppDialogs.confirm(
                        WorkoutDayDialog.this,
                        "세트 삭제",
                        saved ? "이 세트를 삭제할까요?"
                                : "입력 중인 내용을 지울까요?",
                        "취소", "삭제", true)) return;
            }

            if (saved) {

                long id = owner.record.getRecordId();

                boolean removed =
                        controller.deleteSetAndRemoveEmptyRecord(
                                resolveId());

                if (removed) {

                    records.removeIf(r -> r.getRecordId() == id);

                    owner.record.setRecordId(0);
                }
            }

            owner.lines.remove(this);

            if (owner.record.getRecordId() == 0
                    && owner.lines.stream()
                    .noneMatch(l -> l.saved || l.dirty)) {

                exerciseCards.remove(owner);

                rebuildCards();

            } else {

                owner.ensureDraft();
                owner.rebuildLines();
            }

            refreshSummary();
        }
    }

    // 미저장 입력 저장

    private void savePending() throws SQLException {

        Map<SetLine, Input> pending =
                new LinkedHashMap<>();

        for (ExerciseCard card : exerciseCards) {

            for (SetLine line : card.lines) {

                // 자동 입력 후 변경하지 않은 세트는 제외

                if (line.dirty)
                    pending.put(line, line.readInput());
            }
        }

        for (var entry : pending.entrySet())
            entry.getKey().save(entry.getValue(), false);
    }

    public boolean prepareToLeave() {

        boolean changed = exerciseCards.stream()
                .flatMap(card -> card.lines.stream())
                .anyMatch(line -> line.dirty);

        if (!changed) return true;

        Object[] options = {
                "저장 후 나가기", "저장하지 않기", "계속 기록"
        };

        int choice = JOptionPane.showOptionDialog(
                this,
                "저장하지 않은 입력 내용이 있습니다.",
                "운동 기록",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[2]);

        if (choice == 1) return true;

        if (choice == 0) {

            try {

                savePending();
                return true;

            } catch (SQLException | IllegalArgumentException e) {

                showError(e.getMessage());
                return false;
            }
        }

        return false;
    }

    // 휴식시간 입력 처리

    private static Integer parseRest(String input) {

        String value = input.trim();

        if (value.isEmpty() || value.equals("—"))
            return null;

        if (value.endsWith("초"))
            value = value.substring(0, value.length() - 1).trim();

        long total;

        if (value.contains(":")) {

            String[] pieces = value.split(":", -1);

            if (pieces.length != 2)
                throw new IllegalArgumentException();

            long minutes = Long.parseLong(pieces[0].trim());
            long seconds = Long.parseLong(pieces[1].trim());

            if (minutes < 0 || minutes > 16666
                    || seconds < 0 || seconds >= 60) {

                throw new IllegalArgumentException();
            }

            total = minutes * 60 + seconds;

        } else {

            total = Long.parseLong(value);
        }

        if (total < 0 || total > 999999)
            throw new IllegalArgumentException();

        return (int) total;
    }

    private static String formatRest(Integer value) {

        return value == null ? "" :
                String.format("%d:%02d", value / 60, value % 60);
    }

    // 운동 요약

    private void refreshSummary() {

        Set<Long> completed = new HashSet<>();

        long sets = 0;

        BigDecimal volume = BigDecimal.ZERO;

        for (ExerciseCard card : exerciseCards) {

            for (SetLine line : card.lines) {

                if (!line.saved) continue;

                completed.add(card.exercise.getExerciseId());

                sets++;

                volume = volume.add(
                        line.savedWeight.multiply(
                                BigDecimal.valueOf(line.savedReps)));
            }
        }

        setMetric(exerciseCount,
                String.valueOf(completed.size()));

        setMetric(setCount, String.valueOf(sets));

        setMetric(volumeCount,
                new DecimalFormat("#,##0.##").format(volume));
    }

    private static void setMetric(JLabel label, String value) {

        label.setText(value);

        label.setFont(AppTheme.bold(
                value.length() > 6 ? 16 : 23));
    }

    // 공통 UI

    private static JPanel header(
            String title, String subtitle, Runnable backAction) {

        JPanel panel = new JPanel(new BorderLayout(8, 0));

        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton back = textButton("←");

        back.setFont(AppTheme.regular(24));
        back.setForeground(TEXT);
        back.setPreferredSize(new Dimension(44, 44));

        back.addActionListener(e -> backAction.run());

        panel.add(back, BorderLayout.WEST);

        panel.add(bold(title, PAGE_TITLE, TEXT),
                BorderLayout.CENTER);

        if (!subtitle.isEmpty())
            panel.add(label(subtitle, 12, MUTED),
                    BorderLayout.EAST);

        return panel;
    }

    // 세트 번호와 입력창 및 제목 정렬

    private static JPanel row(
            boolean header, JComponent... items) {

        int height = header ? 24 : 44;

        JPanel panel =
                new JPanel(new BorderLayout(4, 0));

        panel.setOpaque(false);

        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.setPreferredSize(new Dimension(300, height));

        panel.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, height));

        JPanel left = new JPanel(new BorderLayout());

        left.setOpaque(false);
        left.setPreferredSize(new Dimension(26, height));

        left.add(items[0], BorderLayout.CENTER);

        JPanel center =
                new JPanel(new GridLayout(1, 3, 4, 0));

        center.setOpaque(false);

        center.add(items[1]);
        center.add(items[2]);
        center.add(items[3]);

        JPanel right =
                new JPanel(new BorderLayout(4, 0));

        right.setOpaque(false);

        right.setPreferredSize(new Dimension(70, height));

        items[4].setPreferredSize(new Dimension(42, height));
        items[5].setPreferredSize(new Dimension(24, height));

        right.add(items[4], BorderLayout.CENTER);
        right.add(items[5], BorderLayout.EAST);

        panel.add(left, BorderLayout.WEST);
        panel.add(center, BorderLayout.CENTER);
        panel.add(right, BorderLayout.EAST);

        return panel;
    }

    private static void alignLeft(JPanel panel) {

        for (Component component : panel.getComponents()) {

            if (component instanceof JComponent jc)
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }

    private static JPanel metric(String title, JLabel value) {

        JPanel panel = vertical();

        panel.add(label(title, 12, MUTED));

        panel.add(Box.createVerticalStrut(6));

        panel.add(value);

        return panel;
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

    private static JLabel centered(
            String text, int size, Color color) {

        JLabel label = label(text, size, color);

        label.setHorizontalAlignment(SwingConstants.CENTER);

        return label;
    }

    private static JButton button(String text, boolean primary) {

        JButton button = new JButton(text);

        button.setFont(AppTheme.bold(14));

        button.setBackground(primary ? TEXT : CARD);

        button.setForeground(primary ? BG : TEXT);

        button.setBorderPainted(false);

        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        button.putClientProperty("JButton.buttonType", "roundRect");

        return button;
    }

    private static JButton textButton(String text) {

        JButton button = new JButton(text);

        button.setFont(AppTheme.regular(12));
        button.setForeground(MUTED);

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);

        button.setMargin(new Insets(3, 4, 3, 4));

        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        return button;
    }

    private static void fullWidth(JComponent item, int height) {

        item.setPreferredSize(new Dimension(300, height));

        item.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, height));

        item.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private static void styleField(JTextField field) {

        field.setBackground(FIELD);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);

        field.setFont(AppTheme.body());

        field.putClientProperty("JComponent.roundRect", true);
    }

    private static JTextField numberField(String text) {

        JTextField field = new JTextField(text);

        styleField(field);

        field.setFont(AppTheme.bold(15));

        field.setHorizontalAlignment(SwingConstants.CENTER);

        field.setMargin(new Insets(2, 3, 2, 3));

        field.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {

                SwingUtilities.invokeLater(() -> {

                    if (field.isFocusOwner())
                        field.selectAll();
                });
            }
        });

        return field;
    }

    private static void watch(JTextField field, Runnable action) {

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

    private static JScrollPane scroll(JComponent component) {

        JScrollPane pane = new JScrollPane(component);

        pane.setBorder(null);

        pane.getViewport().setBackground(BG);

        pane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        pane.getVerticalScrollBar().setUnitIncrement(20);

        return pane;
    }

    private static class TrashIcon implements Icon {

        public int getIconWidth() { return 20; }
        public int getIconHeight() { return 20; }

        public void paintIcon(Component c,
                              Graphics graphics, int x, int y) {

            Graphics2D g = (Graphics2D) graphics.create();

            g.translate(x, y);

            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(c.getForeground());

            g.setStroke(new BasicStroke(1.5f));

            g.drawLine(2, 5, 18, 5);
            g.drawLine(7, 2, 13, 2);

            g.drawRoundRect(4, 5, 12, 13, 3, 3);

            g.drawLine(8, 8, 8, 15);
            g.drawLine(12, 8, 12, 15);

            g.dispose();
        }
    }

    private static class RoundedPanel extends JPanel {

        final Color color;
        final int arc;

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

            g.fillRoundRect(0, 0,
                    getWidth(), getHeight(), arc, arc);

            g.dispose();

            super.paintComponent(graphics);
        }

        @Override
        public Dimension getMaximumSize() {

            return new Dimension(Integer.MAX_VALUE,
                    getPreferredSize().height);
        }
    }

    private static class ScrollableCards
            extends JPanel implements Scrollable {

        ScrollableCards() {

            setBackground(BG);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

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

            showError(e.getMessage());
        }
    }

    private void showError(String message) {

        JOptionPane.showMessageDialog(
                this, message,
                "운동 기록 확인",
                JOptionPane.WARNING_MESSAGE);
    }
}