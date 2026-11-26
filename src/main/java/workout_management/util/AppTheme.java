package workout_management.util;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.io.InputStream;

public final class AppTheme {

    // 공통 색상
    public static final Color BG = new Color(23, 25, 29);
    public static final Color CARD = new Color(39, 42, 47);
    public static final Color FIELD = new Color(51, 54, 59);

    public static final Color TEXT = new Color(245, 245, 245);
    public static final Color MUTED = new Color(180, 180, 180);
    public static final Color LINE = new Color(65, 68, 74);

    public static final Color GREEN = new Color(145, 214, 172);
    public static final Color RED = new Color(230, 160, 160);
    public static final Color RED_BG = new Color(73, 50, 55);

    // 공통 글자 크기
    public static final int LOGO = 28;
    public static final int PAGE_TITLE = 24;
    public static final int SECTION_TITLE = 18;
    public static final int ITEM_TITLE = 15;
    public static final int BODY = 14;
    public static final int CAPTION = 12;
    public static final int SMALL = 11;
    public static final int NUMBER = 26;

    // 공통 UI 크기
    public static final int BUTTON_HEIGHT = 44;
    public static final int CARD_ARC = 24;
    public static final int FIELD_ARC = 18;
    public static final int PADDING = 16;
    public static final int GAP = 8;

    private static final Font REGULAR = loadFont(
            "/fonts/Pretendard-Regular.otf", Font.PLAIN);

    private static final Font BOLD = loadFont(
            "/fonts/Pretendard-Bold.otf", Font.BOLD);

    private AppTheme() {
    }

    // 폰트 불러오기
    private static Font loadFont(String path, int style) {

        try (InputStream stream =
                     AppTheme.class.getResourceAsStream(path)) {

            if (stream == null) {
                throw new IllegalStateException(
                        "폰트 파일을 찾을 수 없습니다.");
            }

            Font font = Font.createFont(
                    Font.TRUETYPE_FONT, stream);

            GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .registerFont(font);

            return font;

        } catch (Exception e) {

            System.err.println(
                    "폰트 로딩 실패: " + path);

            return new Font(
                    Font.SANS_SERIF, style, BODY);
        }
    }

    public static Font font(int style, float size) {

        Font base = (style & Font.BOLD) != 0
                ? BOLD : REGULAR;

        if ((style & Font.ITALIC) != 0) {

            return base.deriveFont(
                    base.getStyle() | Font.ITALIC, size);
        }

        return base.deriveFont(size);
    }

    public static Font regular(float size) {
        return font(Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return font(Font.BOLD, size);
    }

    public static Font logo() {
        return bold(LOGO);
    }

    public static Font pageTitle() {
        return bold(PAGE_TITLE);
    }

    public static Font sectionTitle() {
        return bold(SECTION_TITLE);
    }

    public static Font itemTitle() {
        return bold(ITEM_TITLE);
    }

    public static Font body() {
        return regular(BODY);
    }

    public static Font caption() {
        return regular(CAPTION);
    }

    public static Font number() {
        return bold(NUMBER);
    }

    // 전체 프로그램 기본 디자인 설정
    public static void install() {

        UIManager.put(
                "defaultFont",
                new FontUIResource(regular(BODY)));

        // 배경 및 글자 색상
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.disabledForeground", MUTED);

        UIManager.put("Button.background", CARD);
        UIManager.put("Button.foreground", TEXT);

        UIManager.put("TextField.background", CARD);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);

        UIManager.put("PasswordField.background", CARD);
        UIManager.put("PasswordField.foreground", TEXT);

        UIManager.put("ComboBox.background", CARD);
        UIManager.put("ComboBox.foreground", TEXT);

        UIManager.put("PopupMenu.background", CARD);
        UIManager.put("MenuItem.background", CARD);
        UIManager.put("MenuItem.foreground", TEXT);

        UIManager.put("List.background", BG);
        UIManager.put("List.foreground", TEXT);

        UIManager.put("Separator.foreground", LINE);

        // 모서리 곡률
        UIManager.put("Button.arc", CARD_ARC);
        UIManager.put("Component.arc", FIELD_ARC);
        UIManager.put("TextComponent.arc", FIELD_ARC);

        UIManager.put("CheckBox.arc", 8);
        UIManager.put("ProgressBar.arc", 16);

        // 입력창 테두리
        UIManager.put("Component.borderColor", LINE);

        UIManager.put(
                "Component.focusedBorderColor",
                new Color(110, 114, 120));

        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 0);

        // 콤보박스
        UIManager.put("ComboBox.buttonStyle", "none");
        UIManager.put("ComboBox.selectionArc", 12);

        UIManager.put(
                "ComboBox.selectionInsets",
                new Insets(2, 4, 2, 4));

        // 미니멀 스크롤바
        UIManager.put("ScrollBar.width", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);

        UIManager.put("ScrollBar.showButtons", false);

        UIManager.put(
                "ScrollBar.thumbInsets",
                new Insets(1, 1, 1, 1));

        UIManager.put("ScrollBar.track", BG);

        UIManager.put(
                "ScrollBar.thumb",
                new Color(90, 94, 100));

        // 선택 영역
        UIManager.put(
                "TextField.selectionBackground",
                new Color(80, 85, 92));

        UIManager.put(
                "TextField.selectionForeground", TEXT);

        System.out.println(
                "LIFTLOG Regular: " + REGULAR.getFontName());

        System.out.println(
                "LIFTLOG Bold: " + BOLD.getFontName());

        System.out.println("LIFTLOG Theme Loaded");
    }
}