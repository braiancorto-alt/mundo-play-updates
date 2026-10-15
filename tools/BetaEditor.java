package com.mundoplay.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
        row.setOnClickListener(v -> showCategories(activity));
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
            labels[copy.size()] = "Restaurar todos los cambios";
            new AlertDialog.Builder(a).setTitle("Editor beta · Categorías")
                .setItems(labels, (dialog, which) -> {
                    if (which == copy.size()) { reset(a); return; }
                    showCategoryActions(a, copy.get(which));
                }).setNegativeButton("Cerrar", null).show();
        } catch (Exception e) { toast(a, "No se pudo abrir el editor."); }
    }

    private static void showCategoryActions(final Activity a, final Object category) {
        final String id = field(category, "id");
        JSONObject edits = read(a).optJSONObject("categories");
        JSONObject saved = edits == null ? null : edits.optJSONObject(id);
        final boolean enabled = saved == null || saved.optBoolean("enabled", true);
        String[] actions = {"Cambiar nombre", enabled ? "Ocultar categoría" : "Activar categoría", "Editar canales", "Volver"};
        new AlertDialog.Builder(a).setTitle(displayCategory(category, saved))
            .setItems(actions, (d, which) -> {
                if (which == 0) prompt(a, "Nombre de categoría", displayCategory(category, saved), value -> {
                    JSONObject root = read(a), all = object(root, "categories"), edit = object(all, id);
                    put(edit, "originalName", originalCategoryName(category, root)); put(edit, "name", value); put(edit, "enabled", enabled);
                    save(a, root); refresh(a);
                });
                else if (which == 1) {
                    JSONObject root = read(a), all = object(root, "categories"), edit = object(all, id);
                    put(edit, "originalName", originalCategoryName(category, root)); put(edit, "name", saved == null ? "" : saved.optString("name", "")); put(edit, "enabled", !enabled);
                    save(a, root); refresh(a);
                } else if (which == 2) showChannels(a, category);
                else showCategories(a);
            }).show();
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
            labels[i] = name + (active ? "" : "  ·  Oculto");
        }
        new AlertDialog.Builder(a).setTitle("Canales · " + displayCategory(category, root.optJSONObject("categories") == null ? null : root.optJSONObject("categories").optJSONObject(categoryId)))
            .setItems(labels, (d, which) -> showChannelActions(a, category, keys.get(which), channels.get(keys.get(which))))
            .setNegativeButton("Volver", (d, w) -> showCategoryActions(a, category)).show();
    }

    private static void showChannelActions(final Activity a, final Object category, final String key, Object current) {
        JSONObject root = read(a), all = object(root, "channels"), saved = all.optJSONObject(key);
        final String originalName = saved == null ? field(current, "name") : saved.optString("originalName", field(current, "name"));
        final String name = saved == null ? originalName : saved.optString("name", originalName);
        final String originalIcon = saved == null ? field(current, "icon") : saved.optString("originalIcon", field(current, "icon"));
        final boolean enabled = saved == null || saved.optBoolean("enabled", true);
        String[] actions = {"Cambiar nombre", "Cambiar logo (URL)", enabled ? "Ocultar canal" : "Activar canal", "Volver"};
        new AlertDialog.Builder(a).setTitle(name)
            .setItems(actions, (d, which) -> {
                if (which == 0) prompt(a, "Nombre del canal", name, value -> {
                    JSONObject r = read(a), e = object(r, "channels"), x = object(e, key);
                    put(x, "categoryId", field(category, "id")); put(x, "originalCategory", originalCategoryName(category, r));
                    put(x, "originalName", originalName); put(x, "originalIcon", originalIcon); put(x, "name", value); put(x, "enabled", enabled);
                    save(a, r); refresh(a);
                });
                else if (which == 1) prompt(a, "URL del logo", saved == null ? originalIcon : saved.optString("icon", originalIcon), value -> {
                    JSONObject r = read(a), e = object(r, "channels"), x = object(e, key);
                    put(x, "categoryId", field(category, "id")); put(x, "originalCategory", originalCategoryName(category, r));
                    put(x, "originalName", originalName); put(x, "originalIcon", originalIcon); put(x, "name", name); put(x, "icon", value); put(x, "enabled", enabled);
                    save(a, r); refresh(a);
                });
                else if (which == 2) {
                    JSONObject r = read(a), e = object(r, "channels"), x = object(e, key);
                    put(x, "categoryId", field(category, "id")); put(x, "originalCategory", originalCategoryName(category, r));
                    put(x, "originalName", originalName); put(x, "originalIcon", originalIcon); put(x, "name", saved == null ? "" : saved.optString("name", ""));
                    put(x, "icon", saved == null ? "" : saved.optString("icon", "")); put(x, "enabled", !enabled);
                    save(a, r); refresh(a);
                } else showChannels(a, category);
            }).show();
    }

    private interface ValueHandler { void accept(String value); }
    private static void prompt(Activity a, String title, String old, ValueHandler handler) {
        EditText input = new EditText(a); input.setSingleLine(true); input.setText(old == null ? "" : old);
        input.setSelection(input.getText().length()); input.setHint(title);
        LinearLayout wrap = new LinearLayout(a); wrap.setPadding(dp(a, 20), dp(a, 8), dp(a, 20), 0); wrap.addView(input);
        new AlertDialog.Builder(a).setTitle(title).setView(wrap).setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", (d, w) -> { String value = input.getText().toString().trim(); if (value.isEmpty()) toast(a, "El valor no puede quedar vacío."); else handler.accept(value); }).show();
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
