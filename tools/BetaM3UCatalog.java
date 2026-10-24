package com.mundoplay.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Temporary in-app viewer for the supplied M3U movie list. */
public final class BetaM3UCatalog {
    private static final int BG = Color.rgb(18, 18, 36);
    private static final int CARD = Color.rgb(31, 31, 57);
    private static final int PURPLE = Color.rgb(125, 48, 255);
    private static final int CYAN = Color.rgb(62, 192, 240);
    private static volatile ArrayList<Film> cache;
    private static final LruCache<String, Bitmap> POSTER_CACHE = new LruCache<String, Bitmap>(20 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private static final ExecutorService POSTER_WORKERS = Executors.newFixedThreadPool(4);

    private BetaM3UCatalog() {}

    public static void open(Activity activity) { showCatalog(activity); }

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
        TextView title = text(a, "PELÍCULAS Y SERIES", 20, Color.WHITE, true);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(a, 48), 1));
        TextView close = action(a, "SALIR", PURPLE);
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

        close.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); a.finish(); } });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface ignored) { if (!a.isFinishing()) a.finish(); } });
        dialog.setContentView(page);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface ignored) { sizeFull(dialog); }
        });
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
                        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                play(a, adapter.getItem(position));
                            }
                        });
                        for (final String group : groups) {
                            TextView chip = action(a, group, "Todas".equals(group) ? PURPLE : CARD);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(a, 38));
                            lp.setMargins(0, 0, dp(a, 8), 0);
                            categoriesRow.addView(chip, lp);
                            chip.setOnClickListener(new View.OnClickListener() {
                                @Override public void onClick(View v) {
                                    for (int i = 0; i < categoriesRow.getChildCount(); i++) {
                                        View child = categoriesRow.getChildAt(i);
                                        if (child instanceof TextView) child.setBackground(shape(CARD, CYAN, dp(a, 14)));
                                    }
                                    chip.setBackground(shape(PURPLE, PURPLE, dp(a, 14)));
                                    adapter.setGroup(group);
                                    grid.setSelection(0);
                                }
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
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override public void onPrepared(MediaPlayer mp) { video.start(); }
        });
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(a, "No se pudo reproducir este título.", Toast.LENGTH_LONG).show();
                return true;
            }
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
                    if (!display.isEmpty()) result.add(new Film(display, line, group, attrs.get("tvg-logo")));
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

    private static void loadPoster(final Activity a, final String url, final ImageView target) {
        if (url == null || url.isEmpty()) { target.setImageDrawable(null); return; }
        target.setTag(url);
        Bitmap cached = POSTER_CACHE.get(url);
        if (cached != null) { target.setImageBitmap(cached); return; }
        target.setImageDrawable(null);
        POSTER_WORKERS.execute(new Runnable() {
            @Override public void run() {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(7000);
                    connection.setInstanceFollowRedirects(true);
                    connection.connect();
                    try (InputStream in = connection.getInputStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(in);
                        if (bitmap != null) {
                            POSTER_CACHE.put(url, bitmap);
                            final Bitmap result = bitmap;
                            a.runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    if (url.equals(target.getTag())) target.setImageBitmap(result);
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        });
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
        final String title, url, group, logo;
        Film(String title, String url, String group, String logo) { this.title = title; this.url = url; this.group = group; this.logo = logo; }
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
            Holder holder;
            if (convertView instanceof LinearLayout) {
                holder = (Holder) convertView.getTag();
            } else {
                LinearLayout card = new LinearLayout(activity);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(shape(CARD, Color.rgb(67, 67, 92), dp(activity, 12)));
                card.setClipToOutline(true);
                ImageView poster = new ImageView(activity);
                poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
                poster.setBackgroundColor(Color.rgb(38, 38, 66));
                card.addView(poster, new LinearLayout.LayoutParams(-1, dp(activity, 200)));
                TextView label = new TextView(activity);
                label.setTextColor(Color.WHITE);
                label.setTextSize(13);
                label.setGravity(Gravity.CENTER_VERTICAL);
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                label.setPadding(dp(activity, 9), dp(activity, 6), dp(activity, 9), dp(activity, 8));
                card.addView(label, new LinearLayout.LayoutParams(-1, dp(activity, 50)));
                holder = new Holder(poster, label);
                card.setTag(holder);
                convertView = card;
            }
            Film film = getItem(position);
            holder.label.setText(film.title);
            loadPoster(activity, film.logo, holder.poster);
            convertView.setFocusable(true);
            return convertView;
        }
    }

    private static final class Holder {
        final ImageView poster;
        final TextView label;
        Holder(ImageView poster, TextView label) { this.poster = poster; this.label = label; }
    }
}
