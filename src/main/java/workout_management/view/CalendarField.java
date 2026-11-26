
package workout_management.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;

public class CalendarField extends JTextField {

    private final boolean allowEmpty;

    public CalendarField(LocalDate date, boolean allowEmpty) {

        this.allowEmpty = allowEmpty;

        setDate(date);
        setEditable(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setToolTipText("클릭하여 날짜를 선택하세요.");

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled()) {
                    showCalendar();
                }
            }
        });

        getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke("SPACE"), "calendar");

        getActionMap().put("calendar", new AbstractAction() {

            @Override
            public void actionPerformed(
                    java.awt.event.ActionEvent e) {

                showCalendar();
            }
        });
    }

    public LocalDate getDate() {

        return getText().isBlank()
                ? null : LocalDate.parse(getText());
    }

    public void setDate(LocalDate date) {

        setText(date == null ? "" : date.toString());
    }

    private void showCalendar() {

        LocalDate selected = getDate();

        LocalDate initial = selected == null
                ? LocalDate.now() : selected;

        YearMonth[] month = {YearMonth.from(initial)};

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "날짜 선택",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel root = new JPanel(
                new BorderLayout(0, 15));

        root.setBorder(
                new EmptyBorder(15, 15, 15, 15));

        // 상단 연도 및 월 선택
        JPanel header = new JPanel(new BorderLayout());

        JLabel monthLabel = new JLabel(
                "", SwingConstants.CENTER);

        monthLabel.setFont(
                workout_management.util.AppTheme.font(Font.BOLD, 17));

        JButton previous = new JButton("‹");
        JButton next = new JButton("›");

        header.add(previous, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // 달력 영역
        JPanel days = new JPanel(
                new GridLayout(7, 7, 4, 4));

        Runnable render = () -> {

            days.removeAll();

            monthLabel.setText(
                    month[0].getYear() + "년 "
                            + month[0].getMonthValue() + "월");

            String[] weekdays = {
                    "일", "월", "화", "수",
                    "목", "금", "토"
            };

            for (String weekday : weekdays) {

                JLabel label = new JLabel(
                        weekday, SwingConstants.CENTER);

                days.add(label);
            }

            int firstDay = month[0].atDay(1)
                    .getDayOfWeek().getValue() % 7;

            for (int i = 0; i < 42; i++) {

                int day = i - firstDay + 1;

                if (day < 1 ||
                        day > month[0].lengthOfMonth()) {

                    days.add(new JLabel(""));
                    continue;
                }

                LocalDate date = month[0].atDay(day);

                JButton dayButton =
                        new JButton(String.valueOf(day));

                dayButton.setUI(
                        new javax.swing.plaf.basic.BasicButtonUI());

                dayButton.setPreferredSize(
                        new Dimension(42, 42));

                dayButton.setFont(
                        workout_management.util.AppTheme.font(Font.PLAIN, 14));

                dayButton.setMargin(new Insets(0, 0, 0, 0));
                dayButton.setBorder(new EmptyBorder(0, 0, 0, 0));
                dayButton.setFocusPainted(false);
                dayButton.setHorizontalAlignment(SwingConstants.CENTER);
                dayButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

                // 선택된 날짜 표시
                if (date.equals(selected)) {

                    dayButton.setBackground(
                            new Color(39, 42, 47));

                    dayButton.setForeground(Color.WHITE);
                    dayButton.setOpaque(true);
                    dayButton.setBorderPainted(false);

                } else if (date.equals(LocalDate.now())) {

                    dayButton.setFont(
                            workout_management.util.AppTheme.font(Font.BOLD, 14));
                }

                dayButton.addActionListener(e -> {

                    setDate(date);
                    dialog.dispose();
                });

                days.add(dayButton);
            }

            days.revalidate();
            days.repaint();
        };

        previous.addActionListener(e -> {

            month[0] = month[0].minusMonths(1);
            render.run();
        });

        next.addActionListener(e -> {

            month[0] = month[0].plusMonths(1);
            render.run();
        });

        root.add(days, BorderLayout.CENTER);

        // 하단 버튼
        JPanel bottom = new JPanel(new FlowLayout());

        JButton todayButton = new JButton("오늘");
        JButton cancelButton = new JButton("취소");

        todayButton.addActionListener(e -> {

            setDate(LocalDate.now());
            dialog.dispose();
        });

        cancelButton.addActionListener(
                e -> dialog.dispose());

        bottom.add(todayButton);

        if (allowEmpty) {

            JButton clearButton =
                    new JButton("전체 날짜");

            clearButton.addActionListener(e -> {

                setDate(null);
                dialog.dispose();
            });

            bottom.add(clearButton);
        }

        bottom.add(cancelButton);

        root.add(bottom, BorderLayout.SOUTH);

        render.run();

        dialog.setContentPane(root);
        dialog.setDefaultCloseOperation(
                JDialog.DISPOSE_ON_CLOSE);

        dialog.setSize(400, 430);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
