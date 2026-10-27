package tv.mundoplay.demo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 12, 21);
    private static final int PANEL = Color.rgb(22, 25, 39);
    private static final int MUTED = Color.rgb(162, 168, 187);
    private static final int ACCENT = Color.rgb(105, 86, 255);
    private final List<Channel> channels = Arrays.asList(
            new Channel("Canal Central", "Noticias", "CC", "En vivo · Actualidad"),
            new Channel("Mundo Deportes", "Deportes", "MD", "Partido destacado"),
            new Channel("Cine Premium", "Películas", "CP", "Cine · Acción"),
            new Channel("Series Plus", "Series", "SP", "Series · Recomendado"),
            new Channel("Documentales", "Documentales", "DOC", "Historias para descubrir"),
            new Channel("Música 24", "Música", "M24", "Música · En vivo")
    );
    private String selectedCategory = "Todos";
    private String currentTab = "Inicio";
    private LinearLayout page;
    private LinearLayout categoryRow;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
    }

    private void showHome() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.addView(header(), new LinearLayout.LayoutParams(-1, dp(66)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(12), dp(20), dp(20));
        scroll.addView(page);

        TextView eyebrow = label("TU ENTRETENIMIENTO, EN UN SOLO LUGAR", 11, MUTED, true);
        eyebrow.setLetterSpacing(.08f);
        page.addView(eyebrow);
        TextView title = label("Explora canales", 27, Color.WHITE, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
        titleLp.topMargin = dp(5);
        page.addView(title, titleLp);

        page.addView(heroCard(), margins(-1, dp(164), 0, dp(18), 0, dp(18)));
        TextView section = label("Categorías", 17, Color.WHITE, true);
        page.addView(section, margins(-2, -2, 0, 0, 0, dp(10)));
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        categoryRow = new LinearLayout(this);
        categoryRow.setOrientation(LinearLayout.HORIZONTAL);
        horizontal.addView(categoryRow);
        page.addView(horizontal, margins(-1, dp(42), 0, 0, 0, dp(20)));
        fillCategories();

        LinearLayout sectionLine = new LinearLayout(this);
        sectionLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView channelsTitle = label("Canales para ti", 19, Color.WHITE, true);
        sectionLine.addView(channelsTitle, new LinearLayout.LayoutParams(0, -2, 1));
        TextView seeAll = label("VER TODOS  ›", 11, Color.rgb(177, 167, 255), true);
        sectionLine.addView(seeAll);
        page.addView(sectionLine, margins(-1, -2, 0, 0, 0, dp(10)));
        addChannelCards();

        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        shell.addView(bottomBar());
        setContentView(shell);
    }

    private View header() {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(20), 0, dp(20), 0);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = label("MUNDO", 18, Color.WHITE, true);
        brand.setLetterSpacing(.1f);
        row.addView(brand);
        TextView play = label("PLAY", 18, Color.rgb(166, 151, 255), true);
        play.setLetterSpacing(.1f);
        row.addView(play);
        View spacer = new View(this);
        row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView demo = label("DEMO", 10, Color.WHITE, true);
        demo.setPadding(dp(10), dp(6), dp(10), dp(6));
        demo.setBackground(round(ACCENT, 99));
        row.addView(demo);
        return row;
    }

    private View heroCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.BOTTOM);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(57, 47, 128), Color.rgb(31, 39, 84), Color.rgb(20, 24, 41)});
        bg.setCornerRadius(dp(18));
        card.setBackground(bg);
        TextView pill = label("✦  EXPERIENCIA DE PRUEBA", 10, Color.WHITE, true);
        pill.setLetterSpacing(.06f);
        card.addView(pill);
        TextView headline = label("Todo lo que te gusta\nempieza aquí.", 24, Color.WHITE, true);
        card.addView(headline, margins(-2, -2, 0, dp(9), 0, dp(5)));
        TextView sub = label("Explora la interfaz y prueba la navegación.", 12, Color.rgb(220, 218, 239), false);
        card.addView(sub);
        return card;
    }

    private void fillCategories() {
        categoryRow.removeAllViews();
        List<String> names = Arrays.asList("Todos", "Noticias", "Deportes", "Películas", "Series", "Documentales", "Música");
        for (String name : names) {
            Button chip = new Button(this);
            chip.setAllCaps(false);
            chip.setText(name);
            chip.setTextSize(12);
            chip.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            chip.setTextColor(name.equals(selectedCategory) ? Color.WHITE : MUTED);
            chip.setPadding(dp(13), 0, dp(13), 0);
            chip.setMinWidth(0);
            chip.setMinimumWidth(0);
            chip.setMinHeight(0);
            chip.setMinimumHeight(0);
            chip.setBackground(round(name.equals(selectedCategory) ? ACCENT : PANEL, 99));
            chip.setOnClickListener(v -> { selectedCategory = name; fillCategories(); addChannelCards(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(38));
            lp.rightMargin = dp(8);
            categoryRow.addView(chip, lp);
        }
    }

    private void addChannelCards() {
        if (page == null) return;
        // Keep the static header, hero, categories and section row; replace only the cards below it.
        int start = page.getChildCount() - cardCount();
        if (start < 0) start = page.getChildCount();
        while (page.getChildCount() > start) page.removeViewAt(page.getChildCount() - 1);
        int count = 0;
        for (Channel channel : channels) {
            if (!selectedCategory.equals("Todos") && !selectedCategory.equals(channel.category)) continue;
            page.addView(channelCard(channel), margins(-1, dp(68), 0, 0, 0, dp(9)));
            count++;
        }
        if (count == 0) page.addView(label("No hay elementos en esta categoría.", 13, MUTED, false));
    }

    private int cardCount() {
        int n = 0;
        for (int i = 0; page != null && i < page.getChildCount(); i++) {
            Object tag = page.getChildAt(i).getTag();
            if ("channel-card".equals(tag)) n++;
        }
        return n;
    }

    private View channelCard(Channel channel) {
        LinearLayout card = new LinearLayout(this);
        card.setTag("channel-card");
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(11), dp(8), dp(13), dp(8));
        card.setBackground(round(PANEL, 14));
        card.setFocusable(true);
        card.setClickable(true);
        TextView logo = label(channel.logo, 13, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable logoBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{ACCENT, Color.rgb(43, 54, 126)});
        logoBg.setCornerRadius(dp(11));
        logo.setBackground(logoBg);
        card.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, dp(8), 0);
        info.addView(label(channel.name, 14, Color.WHITE, true));
        info.addView(label(channel.subtitle, 11, MUTED, false), margins(-2, -2, 0, dp(3), 0, 0));
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
        TextView live = label("●  LIVE", 9, Color.rgb(255, 124, 143), true);
        card.addView(live);
        card.setOnClickListener(v -> showPlayer(channel));
        return card;
    }

    private View bottomBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackgroundColor(Color.rgb(15, 17, 28));
        for (String item : Arrays.asList("Inicio", "En vivo", "Favoritos", "Ajustes")) {
            TextView tab = label((item.equals("Inicio") ? "⌂\n" : item.equals("En vivo") ? "◉\n" : item.equals("Favoritos") ? "♡\n" : "⚙\n") + item,
                    10, item.equals(currentTab) ? Color.rgb(187, 175, 255) : MUTED, item.equals(currentTab));
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(0, dp(2), 0, dp(2));
            tab.setOnClickListener(v -> { currentTab = item; if (item.equals("Inicio")) showHome(); else showInfo(item); });
            bar.addView(tab, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        return bar;
    }

    private void showPlayer(Channel channel) {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(22), dp(20), dp(22), dp(20));
        screen.setGravity(Gravity.CENTER_HORIZONTAL);
        screen.setBackgroundColor(BG);
        Button back = button("‹   Volver", false);
        back.setOnClickListener(v -> showHome());
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-1, dp(48));
        screen.addView(back, backLp);
        View space = new View(this);
        screen.addView(space, new LinearLayout.LayoutParams(1, 0, 1));
        TextView video = label("▶", 38, Color.WHITE, true);
        video.setGravity(Gravity.CENTER);
        video.setBackground(round(Color.rgb(24, 29, 49), 20));
        screen.addView(video, new LinearLayout.LayoutParams(-1, dp(210)));
        TextView name = label(channel.name, 22, Color.WHITE, true);
        screen.addView(name, margins(-2, -2, 0, dp(18), 0, dp(7)));
        TextView message = label("Reproductor de demostración\nFuente de video sin configurar", 13, MUTED, false);
        message.setGravity(Gravity.CENTER);
        screen.addView(message);
        screen.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        setContentView(screen);
    }

    private void showInfo(String tab) {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(22), dp(20), dp(22), dp(20));
        screen.setGravity(Gravity.CENTER);
        screen.setBackgroundColor(BG);
        TextView title = label(tab, 25, Color.WHITE, true);
        screen.addView(title);
        TextView copy = label("Esta sección estará disponible\nen una próxima versión de prueba.", 14, MUTED, false);
        copy.setGravity(Gravity.CENTER);
        screen.addView(copy, margins(-2, -2, 0, dp(12), 0, 0));
        Button back = button("Volver al inicio", true);
        back.setOnClickListener(v -> { currentTab = "Inicio"; showHome(); });
        screen.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));
        setContentView(screen);
    }

    private Button button(String text, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setBackground(round(primary ? ACCENT : PANEL, 12));
        return b;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return t;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return lp;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

    private static final class Channel {
        final String name, category, logo, subtitle;
        Channel(String name, String category, String logo, String subtitle) {
            this.name = name; this.category = category; this.logo = logo; this.subtitle = subtitle;
        }
    }
}
