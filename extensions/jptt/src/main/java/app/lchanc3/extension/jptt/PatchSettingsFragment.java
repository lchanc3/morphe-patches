package app.lchanc3.extension.jptt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The settings page these patches add, built in code rather than from an XML
 * resource so that the bundle stays a bytecode-only patch.
 *
 * <p>It lives as one more tab in JPTT's own settings, so everything on it is
 * stored in the same {@code SharedPreferences} as the rest of the app's
 * settings, and is therefore part of what export writes out.
 */
@SuppressWarnings("unused")
public final class PatchSettingsFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_EXPORT = 0x6C63;
    private static final int REQUEST_IMPORT = 0x6C64;

    /*
     * Every androidx.preference call here has to exist in JPTT's own copy of the
     * library, which R8 has already shrunk to what the app itself uses. So the
     * preferences are built with the two argument constructors the XML inflater
     * needs -- the Context only ones are gone -- and createPreferenceScreen(),
     * setDialogTitle() and setPersistent() are avoided for the same reason.
     */

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        PreferenceScreen screen = new PreferenceScreen(context, null);

        java.util.List<PatchSettings.Setting> options = PatchSettings.registered();
        if (!options.isEmpty()) {
            PreferenceCategory category = category(context, "Patch 選項");
            screen.addPreference(category);
            for (PatchSettings.Setting setting : options) {
                category.addPreference(number(context, setting));
            }
        }

        PreferenceCategory backup = category(context, "設定備份");
        screen.addPreference(backup);

        backup.addPreference(action(context, "匯出設定",
                "把目前所有設定存成一個 JSON 檔。不含帳號密碼，那些存在另一個檔案裡。",
                preference -> {
                    startExport();
                    return true;
                }));

        backup.addPreference(action(context, "匯入設定",
                "讀回之前匯出的檔案。檔案裡沒提到的設定會保持原樣，匯入後要重開 app 才會全部生效。",
                preference -> {
                    startImport();
                    return true;
                }));

        setPreferenceScreen(screen);
    }

    private void startExport() {
        String name = "jptt-settings-"
                + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date())
                + ".json";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json")
                .putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, REQUEST_EXPORT);
    }

    private void startImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                // Some file pickers will not show a .json saved by another app
                // when the filter is that narrow, so ask for anything readable.
                .setType("*/*");
        startActivityForResult(intent, REQUEST_IMPORT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        Context context = getContext();
        if (context == null) {
            return;
        }

        try {
            if (requestCode == REQUEST_EXPORT) {
                SettingsBackup.write(
                        context.getContentResolver().openOutputStream(uri),
                        SettingsBackup.export(context));
                toast("設定已匯出");
            } else if (requestCode == REQUEST_IMPORT) {
                int written = SettingsBackup.importFrom(
                        context,
                        SettingsBackup.read(context.getContentResolver().openInputStream(uri)));
                toast("已匯入 " + written + " 項設定，請重新啟動 JPTT");
            }
        } catch (Throwable ex) {
            Log.e(JpttContext.LOG_TAG, "Settings backup failed", ex);
            String message = ex.getMessage();
            toast((requestCode == REQUEST_EXPORT ? "匯出失敗" : "匯入失敗")
                    + (message == null ? "" : "：" + message));
        }
    }

    private void toast(String message) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    private EditTextPreference number(Context context, PatchSettings.Setting setting) {
        EditTextPreference preference = new EditTextPreference(context, null);
        preference.setTitle(setting.title);
        preference.setIconSpaceReserved(false);
        preference.setText(String.valueOf(PatchSettings.value(setting.key)));
        preference.setOnBindEditTextListener(editText ->
                editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        preference.setSummaryProvider(anyPreference ->
                PatchSettings.value(setting.key)
                        + "　（預設 " + setting.defaultValue
                        + "，可填 " + setting.min + "–" + setting.max + "）\n"
                        + setting.summary);
        // Preference.setKey() is not in the app's copy of the library, so this
        // stores the value itself. Without a key the preference persists nothing
        // on its own, which is exactly what is wanted here.
        preference.setOnPreferenceChangeListener((changed, newValue) -> {
            PatchSettings.store(getContext(), setting.key, String.valueOf(newValue));
            return true;
        });
        return preference;
    }

    private static PreferenceCategory category(Context context, String title) {
        PreferenceCategory category = new PreferenceCategory(context, null);
        category.setTitle(title);
        category.setIconSpaceReserved(false);
        return category;
    }

    private static Preference action(
            Context context,
            String title,
            String summary,
            Preference.OnPreferenceClickListener onClick
    ) {
        Preference preference = new Preference(context, null);
        preference.setTitle(title);
        preference.setSummary(summary);
        preference.setIconSpaceReserved(false);
        preference.setOnPreferenceClickListener(onClick);
        return preference;
    }
}
