package com.mundoplay.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Beta-only local catalog editor injected into the 6.32 APK rebuild. */
public final class BetaEditor {
    private static final String PREFS = "mundo_play";
    private static final String KEY = "beta_catalog_edits_v1";
    private static final int BG = Color.rgb(22, 22, 43);
    private static final int PURPLE = Color.rgb(125, 48, 255);
    private static final int CYAN = Color.rgb(62, 192, 240);

    private BetaEditor() {}

    public static void addEntry(final Activity activity, LinearLayout parent) {
        TextView row = new TextView(activity);
        row.setText("✎   Editor de canales (beta)");
        row.setTextColor(Color.WHITE);
        row.setTextSize(16);
        row.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.setGravity(17);
        row.setPadding(dp(activity, 18), dp(activity, 14), dp(activity, 18), dp(activity, 14));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(BG);
        shape.setCornerRadius(dp(activity, 16));
        shape.setStroke(dp(activity, 1), Color.rgb(67, 67, 92));
        row.setBackground(shape);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(activity, 8), 0, dp(activity, 8));
        parent.addView(row, lp);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCategories(activity); }
        });
    }

    /** Apply category-name overrides while keeping the source list intact for the editor. */
    public static void applyCategoryNames(Activity activity, List<?> categories) {
        JSONObject edits = read(activity).optJSONObject("categories");
        if (edits == null) return;
        for (Object category : categories) {
            String id = field(category, "id");
            JSONObject edit = edits.optJSONObject(id);
            if (edit == null) continue;
            String original = field(category, "name");
            if (!edit.has("originalName")) put(edit, "originalName", original);
            String name = edit.optString("name", "");
            if (!name.isEmpty()) setField(category, "name", name);
        }
        save(activity, read(activity));
    }

    /** Return only enabled categories for the visible home sections. */
    public static ArrayList<Object> filterCategories(Activity activity, List<?> categories) {
        JSONObject edits = read(activity).optJSONObject("categories");
        ArrayList<Object> visible = new ArrayList<>();
        for (Object category : categories) {
            String id = field(category, "id");
            JSONObject edit = edits == null ? null : edits.optJSONObject(id);
            if (edit == null || edit.optBoolean("enabled", true)) visible.add(category);
        }
        return visible;
    }

    /** Apply saved channel name/logo edits and omit channels switched off by the user. */
    public static void applyChannelEdits(Activity activity, Map<?, ?> categoryItems) {
        JSONObject root = read(activity);
        JSONObject edits = root.optJSONObject("channels");
        JSONObject categories = root.optJSONObject("categories");
        for (Map.Entry<?, ?> entry : categoryItems.entrySet()) {
            String categoryId = String.valueOf(entry.getKey());
            JSONObject categoryEdit = categories == null ? null : categories.optJSONObject(categoryId);
            if (categoryEdit != null && !categoryEdit.optBoolean("enabled", true)) {
                ((List<?>) entry.getValue()).clear();
                continue;
            }
            if (!(entry.getValue() instanceof List)) continue;
            @SuppressWarnings("unchecked") List<Object> items = (List<Object>) entry.getValue();
            java.util.Iterator<Object> iterator = items.iterator();
            while (iterator.hasNext()) {
                Object item = iterator.next();
                JSONObject edit = edits == null ? null : edits.optJSONObject(itemKey(item));
                if (edit == null) continue;
                if (!edit.optBoolean("enabled", true)) { iterator.remove(); continue; }
                String name = edit.optString("name", "");
                String icon = edit.optString("icon", "");
                if (!name.isEmpty()) setField(item, "name", name);
                if (!icon.isEmpty()) setField(item, "icon", icon);
            }
        }
    }

    /** Used by the render callback so disabled channels never become cards or play targets. */
    public static ArrayList<Object> filterChannels(Activity activity, String categoryId, List<?> items) {
        JSONObject root = read(activity);
        JSONObject edits = root.optJSONObject("channels");
        JSONObject categories = root.optJSONObject("categories");
        JSONObject cat = categories == null ? null : categories.optJSONObject(categoryId);
        ArrayList<Object> visible = new ArrayList<>();
        if (cat != null && !cat.optBoolean("enabled", true)) return visible;
        for (Object item : items) {
            JSONObject edit = edits == null ? null : edits.optJSONObject(itemKey(item));
            if (edit == null || edit.optBoolean("enabled", true)) visible.add(item);
        }
        return visible;
    }

    private interface MenuHandler { void select(int which); }

    /** Branded modal used by every beta editor menu instead of Android's gray AlertDialog. */
    private static void showMenu(final Activity a, String title, String subtitle, final String[] rows,
                                 String footer, final MenuHandler handler, final Runnable footerAction) {
        final Dialog dialog = new Dialog(a);
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(a, 18), dp(a, 18), dp(a, 18), dp(a, 12));
        card.setBackground(shape(BG, Color.rgb(67, 67, 92), 22, a));

        TextView heading = text(a, title, 21, Color.WHITE, true);
        card.addView(heading, matchWrap());
        TextView sub = text(a, subtitle == null ? "" : subtitle, 13, CYAN, true);
        sub.setPadding(0, dp(a, 5), 0, dp(a, 12));
        card.addView(sub, matchWrap());

        ScrollView scroll = new ScrollView(a);
        LinearLayout options = new LinearLayout(a);
        options.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < rows.length; i++) {
            final int index = i;
            TextView row = text(a, rows[i], 16, Color.WHITE, false);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(a, 15), dp(a, 13), dp(a, 15), dp(a, 13));
            row.setMinHeight(dp(a, 50));
            row.setBackground(shape(Color.rgb(29, 29, 55), Color.rgb(58, 58, 84), 14, a));
            LinearLayout.LayoutParams rowLp = matchWrap();
            rowLp.bottomMargin = dp(a, 8);
            options.addView(row, rowLp);
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { dialog.dismiss(); handler.select(index); }
            });
        }
        scroll.addView(options);
        int maxHeight = (int) (a.getResources().getDisplayMetrics().heightPixels * 0.55f);
        int desiredHeight = dp(a, Math.max(54, rows.length * 58));
        card.addView(scroll, new LinearLayout.LayoutParams(-1, Math.min(maxHeight, desiredHeight)));

        TextView close = text(a, footer == null ? "Cerrar" : footer, 15, CYAN, true);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(a, 12), dp(a, 13), dp(a, 12), dp(a, 8));
        card.addView(close, matchWrap());
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                dialog.dismiss();
                if (footerAction != null) footerAction.run();
            }
        });

        dialog.setContentView(card);
        dialog.setCancelable(true);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            dialog.getWindow().setDimAmount(0.68f);
            dialog.getWindow().setLayout(a.getResources().getDisplayMetrics().widthPixels - dp(a, 32), -2);
        }
    }

    private static void showCategories(final Activity a) {
        try {
            List<?> categories = (List<?>) getField(a, "liveCategories");
            if (categories == null || categories.isEmpty()) {
                toast(a, "Primero actualizá los canales.");
                return;
            }
            JSONObject edits = read(a).optJSONObject("categories");
            final List<Object> copy = new ArrayList<>(categories);
            String[] labels = new String[copy.size() + 1];
            for (int i = 0; i < copy.size(); i++) {
                Object c = copy.get(i);
                JSONObject edit = edits == null ? null : edits.optJSONObject(field(c, "id"));
                labels[i] = displayCategory(c, edit) + ((edit != null && !edit.optBoolean("enabled", true)) ? "  ·  Oculta" : "");
            }
            labels[copy.size()] = "↺   Restaurar todos los cambios";
            showMenu(a, "Editor beta", "CATEGORÍAS · " + copy.size(), labels, "Listo", new MenuHandler() {
                @Override public void select(int which) {
                    if (which == copy.size()) { reset(a); return; }
                    showCategoryActions(a, copy.get(which));
                }
            }, null);
        } catch (Exception e) { toast(a, "No se pudo abrir el editor."); }
    }

    private static void showCategoryActions(final Activity a, final Object category) {
        final String id = field(category, "id");
        JSONObject edits = read(a).optJSONObject("categories");
        final JSONObject saved = edits == null ? null : edits.optJSONObject(id);
        final boolean enabled = saved == null || saved.optBoolean("enabled", true);
        String[] actions = {"✎   Cambiar nombre", enabled ? "◉   Ocultar categoría" : "◉   Activar categoría", "▤   Editar canales"};
        showMenu(a, displayCategory(category, saved), "OPCIONES DE CATEGORÍA", actions, "←  Categorías", new MenuHandler() {
            @Override public void select(int which) {
                if (which == 0) prompt(a, "Nombre de categoría", displayCategory(category, saved), false, new ValueHandler() {
                    @Override public void accept(String value) {
                        JSONObject root = read(a), all = object(root, "categories"), edit = object(all, id);
                        put(edit, "originalName", originalCategoryName(category, root)); put(edit, "name", value); put(edit, "enabled", enabled);
                        save(a, root); refresh(a);
                    }
                });
                else if (which == 1) {
                    JSONObject root = read(a), all = object(root, "categories"), edit = object(all, id);
                    put(edit, "originalName", originalCategoryName(category, root)); put(edit, "name", saved == null ? "" : saved.optString("name", "")); put(edit, "enabled", !enabled);
                    save(a, root); refresh(a);
                } else if (which == 2) showChannels(a, category);
            }
        }, new Runnable() { @Override public void run() { showCategories(a); } });
    }

    private static void showChannels(final Activity a, final Object category) {
        final String categoryId = field(category, "id");
        final JSONObject root = read(a);
        final JSONObject edits = root.optJSONObject("channels");
        final String originalCategory = originalCategoryName(category, root);
        final LinkedHashMap<String, Object> channels = new LinkedHashMap<>();
        try {
            Map<?, ?> all = (Map<?, ?>) getField(a, "allItems");
            synchronized (all) {
                for (Object item : all.values()) {
                    if (originalCategory.equalsIgnoreCase(field(item, "category"))) channels.put(itemKey(item), item);
                }
            }
        } catch (Exception ignored) {}
        if (edits != null) {
            java.util.Iterator<String> keys = edits.keys();
            while (keys.hasNext()) {
                String key = keys.next(); JSONObject edit = edits.optJSONObject(key);
                if (edit != null && categoryId.equals(edit.optString("categoryId"))) channels.put(key, edit);
            }
        }
        if (channels.isEmpty()) { toast(a, "No hay canales cargados en esta categoría."); return; }
        final ArrayList<String> keys = new ArrayList<>(channels.keySet());
        String[] labels = new String[keys.size()];
        for (int i = 0; i < keys.size(); i++) {
            Object value = channels.get(keys.get(i));
            JSONObject edit = value instanceof JSONObject ? (JSONObject) value : (edits == null ? null : edits.optJSONObject(keys.get(i)));
            String name = edit == null ? field(value, "name") : edit.optString("name", edit.optString("originalName", field(value, "name")));
            boolean active = edit == null || edit.optBoolean("enabled", true);
            labels[i] = (active ? "●   " : "○   ") + name + (active ? "" : "  ·  Oculto");
        }
        JSONObject categoryEdits = root.optJSONObject("categories");
        JSONObject categoryEdit = categoryEdits == null ? null : categoryEdits.optJSONObject(categoryId);
        showMenu(a, "Canales", displayCategory(category, categoryEdit) + " · " + keys.size(), labels, "←  Categoría", new MenuHandler() {
            @Override public void select(int which) { showChannelActions(a, category, keys.get(which), channels.get(keys.get(which))); }
        }, new Runnable() { @Override public void run() { showCategoryActions(a, category); } });
    }

    private static void showChannelActions(final Activity a, final Object category, final String key, Object current) {
        JSONObject root = read(a), all = object(root, "channels"), saved = all.optJSONObject(key);
        final String originalName = saved == null ? field(current, "name") : saved.optString("originalName", field(current, "name"));
        final String name = saved == null ? originalName : saved.optString("name", originalName);
        final String originalIcon = saved == null ? field(current, "icon") : saved.optString("originalIcon", field(current, "icon"));
        final boolean enabled = saved == null || saved.optBoolean("enabled", true);
        String[] actions = {"✎   Cambiar nombre", "▣   Cambiar logo (URL)", enabled ? "◉   Ocultar canal" : "◉   Activar canal"};
        showMenu(a, name, "EDITAR CANAL", actions, "←  Canales", new MenuHandler() {
            @Override public void select(int which) {
                if (which == 0) prompt(a, "Nombre del canal", name, false, new ValueHandler() {
                    @Override public void accept(String value) {
                        JSONObject r = read(a), e = object(r, "channels"), x = object(e, key);
                        put(x, "categoryId", field(category, "id")); put(x, "originalCategory", originalCategoryName(category, r));
                        put(x, "originalName", originalName); put(x, "originalIcon", originalIcon); put(x, "name", value); put(x, "enabled", enabled);
                        save(a, r); refresh(a);
                    }
                });
                else if (which == 1) prompt(a, "URL del logo", saved == null ? originalIcon : saved.optString("icon", originalIcon), true, new ValueHandler() {
                    @Override public void accept(String value) {
                        JSONObject r = read(a), e = object(r, "channels"), x = object(e, key);
                        put(x, "categoryId", field(category, "id")); put(x, "originalCategory", originalCategoryName(category, r));
                        put(x, "originalName", originalName); put(x, "originalIcon", originalIcon); put(x, "name", name); put(x, "icon", value); put(x, "enabled", enabled);
                        save(a, r); refresh(a);
                    }
                });
                else if (which == 2) {
                    JSONObject r = read(a), e = object(r, "channels"), x = object(e, key);
                    put(x, "categoryId", field(category, "id")); put(x, "originalCategory", originalCategoryName(category, r));
                    put(x, "originalName", originalName); put(x, "originalIcon", originalIcon); put(x, "name", saved == null ? "" : saved.optString("name", ""));
                    put(x, "icon", saved == null ? "" : saved.optString("icon", "")); put(x, "enabled", !enabled);
                    save(a, r); refresh(a);
                }
            }
        }, new Runnable() { @Override public void run() { showChannels(a, category); } });
    }

    private interface ValueHandler { void accept(String value); }
    private static void prompt(final Activity a, String title, String old, boolean url, final ValueHandler handler) {
        final Dialog dialog = new Dialog(a);
        LinearLayout card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(a, 20), dp(a, 20), dp(a, 20), dp(a, 14));
        card.setBackground(shape(BG, Color.rgb(67, 67, 92), 22, a));
        TextView heading = text(a, title, 20, Color.WHITE, true);
        card.addView(heading, matchWrap());
        TextView sub = text(a, url ? "PEGÁ EL ENLACE DIRECTO DE LA IMAGEN" : "EDITOR DE CATÁLOGO", 12, CYAN, true);
        sub.setPadding(0, dp(a, 5), 0, dp(a, 14));
        card.addView(sub, matchWrap());
        final EditText input = new EditText(a);
        input.setSingleLine(true);
        input.setInputType(url ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(old == null ? "" : old);
        input.setSelection(input.getText().length());
        input.setHint(url ? "https://..." : "Escribí el nuevo valor");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(165, 165, 185));
        input.setTextSize(16);
        input.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));
        input.setBackground(shape(Color.rgb(29, 29, 55), PURPLE, 14, a));
        card.addView(input, matchWrap());
        LinearLayout buttons = new LinearLayout(a);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cancel = actionText(a, "Cancelar", false);
        TextView save = actionText(a, "Guardar", true);
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, dp(a, 48), 1));
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(a, 48), 1));
        LinearLayout.LayoutParams bLp = matchWrap(); bLp.topMargin = dp(a, 12);
        card.addView(buttons, bLp);
        cancel.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { dialog.dismiss(); } });
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String value = input.getText().toString().trim();
                if (value.isEmpty()) toast(a, "El valor no puede quedar vacío.");
                else { dialog.dismiss(); handler.accept(value); }
            }
        });
        dialog.setContentView(card);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(a.getResources().getDisplayMetrics().widthPixels - dp(a, 32), -2);
        }
    }

    private static TextView text(Activity a, String value, int size, int color, boolean bold) {
        TextView v = new TextView(a); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private static TextView actionText(Activity a, String value, boolean primary) {
        TextView v = text(a, value, 15, Color.WHITE, true); v.setGravity(Gravity.CENTER);
        v.setBackground(shape(primary ? PURPLE : Color.rgb(29, 29, 55), primary ? PURPLE : Color.rgb(67, 67, 92), 14, a));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(a, 46));
        lp.setMargins(dp(a, 4), dp(a, 4), dp(a, 4), dp(a, 4)); v.setLayoutParams(lp); return v;
    }
    private static LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private static GradientDrawable shape(int fill, int stroke, int radius, Activity a) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(a, radius));
        d.setStroke(dp(a, 1), stroke); return d;
    }

    private static void reset(Activity a) {
        JSONObject categories = read(a).optJSONObject("categories");
        if (categories != null) {
            try {
                List<?> live = (List<?>) getField(a, "liveCategories");
                for (Object category : live) {
                    JSONObject edit = categories.optJSONObject(field(category, "id"));
                    if (edit != null) setField(category, "name", edit.optString("originalName", field(category, "name")));
                }
            } catch (Exception ignored) {}
        }
        a.getSharedPreferences(PREFS, 0).edit().remove(KEY).apply();
        toast(a, "Se restauró el catálogo original."); refresh(a);
    }

    private static void refresh(Activity a) {
        try {
            Object categories = getField(a, "liveCategories");
            Method start = a.getClass().getDeclaredMethod("startCatalog", ArrayList.class); start.setAccessible(true);
            start.invoke(a, new ArrayList<>((Collection<?>) categories));
        } catch (Exception e) { toast(a, "Guardado. Volvé a actualizar los canales para aplicar los cambios."); }
    }

    private static String originalCategoryName(Object category, JSONObject root) {
        JSONObject categories = root.optJSONObject("categories");
        JSONObject edit = categories == null ? null : categories.optJSONObject(field(category, "id"));
        return edit == null ? field(category, "name") : edit.optString("originalName", field(category, "name"));
    }
    private static String displayCategory(Object c, JSONObject edit) {
        String value = edit == null ? "" : edit.optString("name", "");
        return value.isEmpty() ? field(c, "name") : value;
    }
    private static String itemKey(Object item) { return field(item, "kind") + ":" + field(item, "id"); }
    private static String field(Object o, String name) { try { Object x = getField(o, name); return x == null ? "" : String.valueOf(x); } catch (Exception e) { return ""; } }
    private static Object getField(Object o, String name) throws Exception { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o); }
    private static void setField(Object o, String name, Object value) { try { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); f.set(o, value); } catch (Exception ignored) {} }
    private static JSONObject read(Activity a) { try { return new JSONObject(a.getSharedPreferences(PREFS, 0).getString(KEY, "{}")); } catch (Exception e) { return new JSONObject(); } }
    private static void save(Activity a, JSONObject root) { a.getSharedPreferences(PREFS, 0).edit().putString(KEY, root.toString()).apply(); }
    private static JSONObject object(JSONObject root, String key) { JSONObject x = root.optJSONObject(key); if (x == null) { x = new JSONObject(); put(root, key, x); } return x; }
    private static void put(JSONObject object, String key, Object value) { try { object.put(key, value); } catch (JSONException ignored) {} }
    private static int dp(Activity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f); }
    private static void toast(Activity a, String text) { Toast.makeText(a, text, Toast.LENGTH_LONG).show(); }
}
