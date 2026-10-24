package com.mundoplay.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Temporary in-app viewer for the supplied M3U movie list. */
public final class BetaM3UCatalog {
    private static final int BG = Color.rgb(18, 18, 36);
    private static final int CARD = Color.rgb(31, 31, 57);
    private static final int PURPLE = Color.rgb(125, 48, 255);
    private static final int CYAN = Color.rgb(62, 192, 240);
    private static volatile ArrayList<Film> cache;

    private BetaM3UCatalog() {}

    public static void addEntry(final Activity activity, LinearLayout parent) {
        TextView row = new TextView(activity);
        row.setText("▶   Probar lista de películas (M3U)");
        row.setTextColor(Color.WHITE);
        row.setTextSize(16);
        row.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(activity, 18), dp(activity, 14), dp(activity, 18), dp(activity, 14));
        row.setBackground(shape(CARD, CYAN, dp(activity, 16)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(activity, 8), 0, dp(activity, 8));
        parent.addView(row, lp);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCatalog(activity); }
        });
    }

    private static void showCatalog(final Activity a) {
        final Dialog dialog = new Dialog(a);
        final LinearLayout page = new LinearLayout(a);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 12));
        page.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(a);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(a, "CATÁLOGO M3U", 20, Color.WHITE, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(a, 48), 1));
        TextView close = action(a, "CERRAR", PURPLE);
        header.addView(close, new LinearLayout.LayoutParams(dp(a, 96), dp(a, 42)));
        page.addView(header);

        TextView status = text(a, "Cargando lista…", 13, CYAN, false);
        status.setPadding(dp(a, 2), dp(a, 4), dp(a, 2), dp(a, 8));
        page.addView(status);

        final HorizontalScrollView categoriesScroll = new HorizontalScrollView(a);
        categoriesScroll.setHorizontalScrollBarEnabled(false);
        final LinearLayout categoriesRow = new LinearLayout(a);
        categoriesRow.setOrientation(LinearLayout.HORIZONTAL);
        categoriesScroll.addView(categoriesRow);
        page.addView(categoriesScroll, new LinearLayout.LayoutParams(-1, dp(a, 48)));

        final GridView grid = new GridView(a);
        grid.setNumColumns(a.getResources().getDisplayMetrics().widthPixels / a.getResources().getDisplayMetrics().density >= 900 ? 5 :
                a.getResources().getDisplayMetrics().widthPixels / a.getResources().getDisplayMetrics().density >= 600 ? 4 : 2);
        grid.setHorizontalSpacing(dp(a, 8));
        grid.setVerticalSpacing(dp(a, 8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(android.R.color.transparent);
        page.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));

        close.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); } });
        dialog.setContentView(page);
        dialog.setOnShowListener(d -> sizeFull(dialog));
        dialog.show();
        sizeFull(dialog);

        new Thread(new Runnable() {
            @Override public void run() {
                final ArrayList<Film> films = load(a);
                a.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (a.isFinishing() || !dialog.isShowing()) return;
                        if (films.isEmpty()) {
                            status.setText("No se pudo leer la lista M3U.");
                            return;
                        }
                        final ArrayList<String> groups = groups(films);
                        status.setText(films.size() + " películas · " + (groups.size() - 1) + " categorías");
                        final FilmAdapter adapter = new FilmAdapter(a, films, "Todas");
                        grid.setAdapter(adapter);
                        grid.setOnItemClickListener((parent, view, position, id) -> play(a, adapter.getItem(position)));
                        for (final String group : groups) {
                            TextView chip = action(a, group, "Todas".equals(group) ? PURPLE : CARD);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(a, 38));
                            lp.setMargins(0, 0, dp(a, 8), 0);
                            categoriesRow.addView(chip, lp);
                            chip.setOnClickListener(v -> {
                                for (int i = 0; i < categoriesRow.getChildCount(); i++) {
                                    View child = categoriesRow.getChildAt(i);
                                    if (child instanceof TextView) child.setBackground(shape(CARD, CYAN, dp(a, 14)));
                                }
                                chip.setBackground(shape(PURPLE, PURPLE, dp(a, 14)));
                                adapter.setGroup(group);
                                grid.setSelection(0);
                            });
                        }
                    }
                });
            }
        }).start();
    }

    private static void play(final Activity a, Film film) {
        final Dialog playerDialog = new Dialog(a, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout frame = new FrameLayout(a);
        frame.setBackgroundColor(Color.BLACK);
        final VideoView video = new VideoView(a);
        frame.addView(video, new FrameLayout.LayoutParams(-1, -1));
        TextView name = text(a, film.title, 14, Color.WHITE, true);
        name.setGravity(Gravity.CENTER_VERTICAL);
        name.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        name.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams nameLp = new FrameLayout.LayoutParams(-1, dp(a, 44), Gravity.TOP);
        frame.addView(name, nameLp);
        playerDialog.setContentView(frame);
        playerDialog.show();
        sizeFull(playerDialog);
        MediaController controller = new MediaController(a);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setOnPreparedListener(mp -> video.start());
        video.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(a, "No se pudo reproducir este título.", Toast.LENGTH_LONG).show();
            return false;
        });
        video.setVideoURI(Uri.parse(film.url));
        video.requestFocus();
    }

    private static ArrayList<Film> load(Activity a) {
        if (cache != null) return cache;
        ArrayList<Film> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(a.getAssets().open("mundo-play-test.m3u"), "UTF-8"))) {
            String line;
            String metadata = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTINF:")) metadata = line;
                else if (!line.isEmpty() && !line.startsWith("#") && metadata != null) {
                    int comma = metadata.indexOf(',');
                    String display = comma >= 0 ? metadata.substring(comma + 1).trim() : "";
                    Map<String, String> attrs = attributes(metadata);
                    if (display.isEmpty()) display = attrs.get("tvg-name");
                    String group = attrs.get("tvg-group");
                    if (group == null || group.trim().isEmpty()) group = attrs.get("group-title");
                    if (group == null || group.trim().isEmpty()) group = "Sin categoría";
                    if (!display.isEmpty()) result.add(new Film(display, line, group));
                    metadata = null;
                }
            }
        } catch (Exception ignored) { result.clear(); }
        cache = result;
        return result;
    }

    private static Map<String, String> attributes(String line) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        Matcher m = Pattern.compile("([A-Za-z0-9_-]+)=\"([^\"]*)\"").matcher(line);
        while (m.find()) values.put(m.group(1), m.group(2));
        return values;
    }

    private static ArrayList<String> groups(List<Film> films) {
        LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();
        seen.put("Todas", true);
        for (Film film : films) seen.put(film.group, true);
        return new ArrayList<>(seen.keySet());
    }

    private static TextView action(Activity a, String value, int color) {
        TextView v = text(a, value, 13, Color.WHITE, true);
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setPadding(dp(a, 12), 0, dp(a, 12), 0);
        v.setFocusable(true);
        v.setBackground(shape(color, color == PURPLE ? PURPLE : Color.rgb(67, 67, 92), dp(a, 14)));
        return v;
    }

    private static TextView text(Activity a, String value, int size, int color, boolean bold) {
        TextView v = new TextView(a);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return v;
    }

    private static GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radius);
        d.setStroke(1, stroke);
        return d;
    }

    private static int dp(Activity a, int value) {
        return (int) (value * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static void sizeFull(Dialog d) {
        Window w = d.getWindow();
        if (w != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            w.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private static final class Film {
        final String title, url, group;
        Film(String title, String url, String group) { this.title = title; this.url = url; this.group = group; }
    }

    private static final class FilmAdapter extends BaseAdapter {
        private final Activity activity;
        private final ArrayList<Film> all;
        private ArrayList<Film> visible;
        private String group;
        FilmAdapter(Activity activity, ArrayList<Film> all, String group) {
            this.activity = activity; this.all = all; setGroup(group);
        }
        void setGroup(String value) {
            group = value;
            visible = new ArrayList<>();
            for (Film film : all) if ("Todas".equals(value) || value.equals(film.group)) visible.add(film);
            notifyDataSetChanged();
        }
        @Override public int getCount() { return visible.size(); }
        @Override public Film getItem(int position) { return visible.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
            TextView card = convertView instanceof TextView ? (TextView) convertView : new TextView(activity);
            Film film = getItem(position);
            card.setText(film.title);
            card.setTextColor(Color.WHITE);
            card.setTextSize(14);
            card.setGravity(Gravity.CENTER);
            card.setPadding(dp(activity, 8), dp(activity, 10), dp(activity, 8), dp(activity, 10));
            card.setMinHeight(dp(activity, 76));
            card.setFocusable(true);
            card.setBackground(shape(CARD, Color.rgb(67, 67, 92), dp(activity, 12)));
            return card;
        }
    }
}
