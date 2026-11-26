package workout_management.view;

import workout_management.controller.WorkoutController;
import workout_management.domain.Exercise;
import workout_management.domain.WorkoutRecord;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class ExerciseManagementDialog extends JDialog {

    private static final Color BG = new Color(23, 25, 29);
    private static final Color CARD = new Color(39, 42, 47);
    private static final Color FIELD = new Color(51, 54, 59);
    private static final Color TEXT = new Color(245, 245, 245);
    private static final Color MUTED = workout_management.util.AppTheme.MUTED;
    private static final Color RED = new Color(240, 167, 167);

    private final WorkoutController controller;

    private final Set<Long> activeIds;
    private final Set<Long> usedIds = new HashSet<>();

    private final DefaultListModel<Exercise> model =
            new DefaultListModel<>();

    private final JList<Exercise> list = new JList<>(model);

    private final JTextField nameField = new JTextField();

    private final JComboBox<String> partCombo =
            new JComboBox<>(new String[]{
                    "가슴", "등", "하체",
                    "어깨", "팔", "복근"
            });

    private final JLabel countLabel = label("", 12, MUTED);
    private final JLabel message = label(" ", 12, MUTED);

    private final JButton editButton = button("수정", true);
    private final JButton deleteButton = button("삭제", false);

    public ExerciseManagementDialog(
            JFrame owner,
            WorkoutController controller,
            Set<Long> activeIds)
            throws SQLException {

        super(owner, "내 운동 관리", true);

        this.controller = controller;
        this.activeIds = new HashSet<>(activeIds);

        setSize(400, 590);
        setResizable(false);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 14));

        root.setBackground(BG);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        // 상단 제목

        JPanel top = verticalPanel();

        JPanel titleRow = new JPanel(new BorderLayout());

        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = label("내 운동 관리", 24, TEXT);

        title.setFont(
                workout_management.util.AppTheme.font(Font.BOLD, 24));

        JButton close = button("닫기", false);

        close.addActionListener(e -> dispose());

        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(close, BorderLayout.EAST);

        top.add(titleRow);
        top.add(Box.createVerticalStrut(10));
        top.add(countLabel);

        root.add(top, BorderLayout.NORTH);

        // 개인 운동 목록

        list.setBackground(BG);

        list.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        list.setFixedCellHeight(62);

        list.setCellRenderer(
                (view, exercise, index, selected, focused) -> {

                    JPanel row = new JPanel(new BorderLayout());

                    row.setBackground(BG);
                    row.setBorder(new EmptyBorder(3, 0, 3, 0));

                    JPanel card = verticalPanel();

                    card.setOpaque(true);
                    card.setBackground(selected ? FIELD : CARD);

                    card.setBorder(
                            new EmptyBorder(8, 14, 8, 14));

                    JLabel name = label(
                            exercise.getExerciseName(), 15, TEXT);

                    name.setFont(
                            workout_management.util.AppTheme.font(Font.BOLD, 15));

                    card.add(name);
                    card.add(Box.createVerticalStrut(3));

                    boolean locked = isLocked(
                            exercise.getExerciseId());

                    card.add(label(
                            exercise.getBodyPart()
                                    + (locked ? " · 사용 중" : ""),
                            11, MUTED));

                    row.add(card);

                    return row;
                });

        list.addListSelectionListener(e -> {

            if (!e.getValueIsAdjusting()) {
                showSelectedExercise();
            }
        });

        JScrollPane scroll = new JScrollPane(list);

        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);

        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.getVerticalScrollBar().setUnitIncrement(20);

        root.add(scroll, BorderLayout.CENTER);

        // 운동 수정 폼

        JPanel form = verticalPanel();

        form.add(label("운동 이름", 12, MUTED));
        form.add(Box.createVerticalStrut(6));

        nameField.setBackground(FIELD);
        nameField.setForeground(TEXT);
        nameField.setCaretColor(TEXT);

        nameField.setFont(
                workout_management.util.AppTheme.font(Font.PLAIN, 14));

        nameField.putClientProperty(
                "JComponent.roundRect", true);

        sizeField(nameField);

        form.add(nameField);
        form.add(Box.createVerticalStrut(12));

        form.add(label("운동 부위", 12, MUTED));
        form.add(Box.createVerticalStrut(6));

        partCombo.setBackground(FIELD);
        partCombo.setForeground(TEXT);

        sizeField(partCombo);

        form.add(partCombo);
        form.add(Box.createVerticalStrut(12));

        deleteButton.setForeground(RED);

        deleteButton.addActionListener(
                e -> runAction(this::deleteExercise));

        editButton.addActionListener(
                e -> runAction(this::editExercise));

        JPanel actions = new JPanel(
                new GridLayout(1, 2, 8, 0));

        actions.setOpaque(false);

        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        actions.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 44));

        actions.add(deleteButton);
        actions.add(editButton);

        form.add(actions);
        form.add(Box.createVerticalStrut(12));
        form.add(message);

        root.add(form, BorderLayout.SOUTH);

        setContentPane(root);

        reload();

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private boolean isLocked(long exerciseId) {

        return activeIds.contains(exerciseId)
                || usedIds.contains(exerciseId);
    }

    private void reload() throws SQLException {

        var exercises = controller.getPersonalExercises();
        var records = controller.getRecords();

        usedIds.clear();

        for (WorkoutRecord record : records) {
            usedIds.add(record.getExerciseId());
        }

        model.clear();

        for (Exercise exercise : exercises) {
            model.addElement(exercise);
        }

        countLabel.setText(
                "직접 등록한 운동 " + model.size() + "개");

        if (!model.isEmpty()) {

            list.setSelectedIndex(0);

        } else {

            showSelectedExercise();
        }
    }

    private void showSelectedExercise() {

        Exercise exercise = list.getSelectedValue();

        if (exercise == null) {

            nameField.setText("");
            nameField.setEnabled(false);
            partCombo.setEnabled(false);

            editButton.setEnabled(false);
            deleteButton.setEnabled(false);

            message.setText("직접 등록한 운동이 없습니다.");

            return;
        }

        nameField.setText(exercise.getExerciseName());
        partCombo.setSelectedItem(exercise.getBodyPart());

        boolean editable = !isLocked(
                exercise.getExerciseId());

        nameField.setEnabled(editable);
        partCombo.setEnabled(editable);

        editButton.setEnabled(editable);
        deleteButton.setEnabled(editable);

        message.setForeground(MUTED);

        message.setText(editable
                ? "이름과 운동 부위를 변경할 수 있습니다."
                : "사용 중인 운동은 변경할 수 없습니다.");
    }

    private void editExercise() throws SQLException {

        Exercise exercise = list.getSelectedValue();

        if (exercise == null
                || isLocked(exercise.getExerciseId())) return;

        controller.updateExercise(
                exercise.getExerciseId(),
                nameField.getText(),
                (String) partCombo.getSelectedItem());

        reload();

        message.setForeground(TEXT);
        message.setText("운동 종목이 수정되었습니다.");
    }

    private void deleteExercise() throws SQLException {

        Exercise exercise = list.getSelectedValue();

        if (exercise == null
                || isLocked(exercise.getExerciseId())) return;

        if (!AppDialogs.confirm(
                this,
                "운동 삭제",
                exercise.getExerciseName() + "을(를) 삭제할까요?",
                "취소", "삭제", true)) {

            return;
        }

        controller.deleteExercise(exercise.getExerciseId());

        reload();

        message.setForeground(TEXT);
        message.setText("운동 종목이 삭제되었습니다.");
    }

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

    private static JButton button(
            String text, boolean primary) {

        JButton button = new JButton(text);

        button.setBackground(primary ? TEXT : FIELD);
        button.setForeground(primary ? BG : TEXT);

        button.setFont(
                workout_management.util.AppTheme.font(Font.BOLD, 13));

        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.putClientProperty(
                "JButton.buttonType", "roundRect");

        button.setPreferredSize(new Dimension(100, 44));

        return button;
    }

    private static void sizeField(JComponent component) {

        component.setPreferredSize(
                new Dimension(300, 44));

        component.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 44));

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private void runAction(SqlAction action) {

        try {

            action.run();

        } catch (SQLException | IllegalArgumentException e) {

            message.setForeground(RED);
            message.setText("<html>" + e.getMessage() + "</html>");
        }
    }
}