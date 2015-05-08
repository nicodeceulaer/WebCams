/*
* ******************************************************************************
* Copyright (c) 2013-2015 Tomas Valenta.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* *****************************************************************************
*/

package cz.yetanotherview.webcamviewer.app;

import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;

import java.util.List;

import cz.yetanotherview.webcamviewer.app.actions.AboutDialog;
import cz.yetanotherview.webcamviewer.app.actions.AddCategoryDialog;
import cz.yetanotherview.webcamviewer.app.actions.EditCategoryDialog;
import cz.yetanotherview.webcamviewer.app.actions.ExportDialog;
import cz.yetanotherview.webcamviewer.app.actions.ImportDialog;
import cz.yetanotherview.webcamviewer.app.adapter.SelectionAdapter;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.helper.DeleteAllWebCams;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class SettingsFragment extends PreferenceFragment {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private Context context;
    private List<Category> allCategories;
    private List<WebCam> allWebCams;
    private Integer[] whichDelete;
    private MaterialDialog indeterminateProgress;
    private SeekBar seekBar;
    private TextView seekBarText;
    private int seekBarProgress, seekBarCorrection, actionColor;
    private DatabaseHelper db;
    private SharedPreferences sharedPref;
    private Preference prefAutoRefreshFullScreen, prefAutoRefreshInterval;
    private PreferenceCategory preferenceCategory;
    private DialogFragment dialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        context = getActivity().getApplicationContext();

        // Enable immersive mode setting only on Kitkat and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getPreferenceScreen().findPreference("pref_full_screen").setEnabled(true);
        }

        db = new DatabaseHelper(context);
        actionColor = getResources().getColor(R.color.yellow);

        setAutoRefreshInterval();
        setAutoHideListener();

        categoryAddEditDelete();

        deleteSelectedWebCams();
        deleteAllWebCams();

        importFromExt();
        exportToExt();

        setZoom();
        resetLastCheck();
        cleanCacheAndTmpFolder();
        showAbout();

        sharedPref = getPreferenceManager().getSharedPreferences();
    }

    private void setAutoRefreshInterval() {
        Preference pref_auto_refresh_interval = findPreference("pref_auto_refresh_interval");
        pref_auto_refresh_interval.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.interval)
                        .customView(R.layout.seekbar_dialog, false)
                        .positiveText(R.string.dialog_positive_text)
                        .iconRes(R.drawable.settings_auto_refresh_interval)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                sharedPref.edit().putInt("pref_auto_refresh_interval", seekBarProgress * 1000).apply();

                                saveDone();
                            }
                        })
                        .build();

                seekBar = (SeekBar) dialog.getCustomView().findViewById(R.id.seekbar_seek);
                seekBarText = (TextView) dialog.getCustomView().findViewById(R.id.seekbar_text);

                seekBarCorrection = 5;
                seekBar.setMax(359);
                seekBarProgress = (sharedPref.getInt("pref_auto_refresh_interval", 30000) / 1000);
                seekBar.setProgress(seekBarProgress - seekBarCorrection);
                seekBarText.setText((seekBar.getProgress() + seekBarCorrection) + "s");

                seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    int val = seekBar.getProgress();

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBarProgress = val;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                        val = progressValue + seekBarCorrection;
                        seekBarText.setText(val + "s");
                    }
                });

                dialog.show();

                return true;
            }
        });
    }

    private void setAutoHideListener() {
        preferenceCategory = (PreferenceCategory)findPreference("pref_category_general");
        CheckBoxPreference prefA = (CheckBoxPreference)findPreference("pref_auto_refresh");

        prefAutoRefreshFullScreen = findPreference("pref_auto_refresh_fullscreen");
        prefAutoRefreshInterval = findPreference("pref_auto_refresh_interval");

        Boolean state = getPreferenceManager().getSharedPreferences().getBoolean("pref_auto_refresh", false);
        if (!state) {
            preferenceCategory.removePreference(prefAutoRefreshFullScreen);
            preferenceCategory.removePreference(prefAutoRefreshInterval);
        }

        prefA.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean switchedOn = (Boolean) newValue;
                if (switchedOn) {
                    preferenceCategory.addPreference(prefAutoRefreshFullScreen);
                    preferenceCategory.addPreference(prefAutoRefreshInterval);

                } else {
                    preferenceCategory.removePreference(prefAutoRefreshFullScreen);
                    preferenceCategory.removePreference(prefAutoRefreshInterval);
                }

                return true;
            }
        });
    }

    private void categoryAddEditDelete() {
        Preference pref_category_add_edit_delete = findPreference("pref_category_add_edit_delete");
        pref_category_add_edit_delete.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference activity_preference) {

                int[] mIcons = {R.drawable.icon_add, R.drawable.icon_edit, R.drawable.icon_delete};
                new MaterialDialog.Builder(getActivity())
                        .items(R.array.add_edit_delete)
                        .title(R.string.pref_category_add_edit_delete)
                        .adapter(new SelectionAdapter(getActivity(), R.array.add_edit_delete, mIcons),
                                new MaterialDialog.ListCallback() {
                                    @Override
                                    public void onSelection(MaterialDialog dialog, View itemView,
                                                            int which, CharSequence text) {

                                        if (which == 0) {
                                            dialogFragment = new AddCategoryDialog();
                                            dialogFragment.show(getFragmentManager(), "AddCategoryDialog");
                                        } else if (which == 1) {
                                            allCategories = db.getAllCategories();
                                            if (allCategories.size() > 0) {
                                                dialogFragment = new EditCategoryDialog();
                                                dialogFragment.show(getFragmentManager(), "EditCategoryDialog");
                                            } else noCategoriesFound();
                                        } else {
                                            allCategories = db.getAllCategories();
                                            String[] items = new String[allCategories.size()];
                                            int count = 0;
                                            for (Category category : allCategories) {
                                                items[count] = category.getCategoryName();
                                                count++;
                                            }

                                            if (allCategories.size() > 0) {
                                                new MaterialDialog.Builder(getActivity())
                                                        .title(R.string.webcam_category)
                                                        .items(items)
                                                        .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                                                            @Override
                                                            public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {

                                                                whichDelete = which;
                                                                if (whichDelete != null) {
                                                                    if (whichDelete.length != 0) {
                                                                        categoryDeleteAlsoWebCamsDialog();
                                                                    }
                                                                }

                                                                return true;
                                                            }
                                                        })
                                                        .positiveText(R.string.choose)
                                                        .show();
                                            } else noCategoriesFound();
                                        }

                                        dialog.dismiss();
                                    }
                                })
                        .show();

                return true;
            }
        });
    }

    private void categoryDeleteAlsoWebCamsDialog (){

            new MaterialDialog.Builder(getActivity())
                    .title(R.string.action_delete)
                    .content(R.string.also_delete_webcams)
                    .positiveText(R.string.Yes)
                    .negativeText(R.string.No)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            showIndeterminateProgress();
                            new deleteAlsoWebCamsBackgroundTask().execute(true);
                        }

                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            showIndeterminateProgress();
                            new deleteAlsoWebCamsBackgroundTask().execute(false);
                        }
                    })
                    .show();
    }

    private class deleteAlsoWebCamsBackgroundTask extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected Void doInBackground(Boolean... booleans) {

            Boolean alsoWebCams = booleans[0];
            if (whichDelete != null && whichDelete.length != 0) {
                synchronized (SettingsFragment.sDataLock) {
                    for (Integer aWhich : whichDelete) {
                        Category deleteCategory = allCategories.get(aWhich);
                        if (alsoWebCams) {
                            db.deleteCategory(deleteCategory.getId(), true);
                        }
                        else db.deleteCategory(deleteCategory.getId(), false);
                    }
                    db.closeDB();
                }
                BackupManager backupManager = new BackupManager(getActivity());
                backupManager.dataChanged();
            }
            saveToPref();

            showDeletedSnackBar();
            return null;
        }
    }

    private void deleteSelectedWebCams() {
        Preference pref_delete_selected = findPreference("pref_delete_selected");
        pref_delete_selected.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                allWebCams = db.getAllWebCams(Utils.defaultSortOrder);

                String[] items = new String[allWebCams.size()];
                int count = 0;
                for (WebCam webCam : allWebCams) {
                    items[count] = webCam.getName();
                    count++;
                }

                if (allWebCams.size() > 0) {
                    new MaterialDialog.Builder(getActivity())
                            .title(R.string.delete_webcams)
                            .items(items)
                            .iconRes(R.drawable.settings_delete_selected)
                            .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {

                                    whichDelete = which;
                                    if (whichDelete != null) {
                                        if (whichDelete.length != 0) {
                                            showIndeterminateProgress();
                                            new deleteSelectedWebCamsBackgroundTask().execute();
                                        }
                                    }

                                    return true;
                                }
                            })
                            .positiveText(R.string.choose)
                            .show();
                } else Snackbar.with(context)
                        .text(R.string.list_is_empty)
                        .actionLabel(R.string.dismiss)
                        .actionColor(actionColor)
                        .show(getActivity());

                return true;
            }
        });
    }

    private class deleteSelectedWebCamsBackgroundTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... integers) {

            if (whichDelete != null && whichDelete.length != 0) {
                synchronized (SettingsFragment.sDataLock) {
                    for (Integer aWhich : whichDelete) {
                        WebCam deleteWebCam = allWebCams.get(aWhich);
                        db.deleteWebCam(deleteWebCam.getId());
                    }
                    db.closeDB();
                }
                BackupManager backupManager = new BackupManager(getActivity());
                backupManager.dataChanged();
            }

            showDeletedSnackBar();
            return null;
        }
    }

    private void deleteAllWebCams() {
        Preference pref_delete_all = findPreference("pref_delete_all");
        pref_delete_all.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_delete_all_webcams)
                        .content(R.string.are_you_sure)
                        .positiveText(R.string.Yes)
                        .negativeText(R.string.No)
                        .iconRes(R.drawable.settings_delete_all)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                showIndeterminateProgress();
                                new deleteAlsoCategoriesBackgroundTask().execute();
                            }
                        })
                        .show();

                return true;
            }
        });
    }

    private class deleteAlsoCategoriesBackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            DeleteAllWebCams.execute(context);
            showDeletedSnackBar();
            return null;
        }
    }

    private void exportToExt() {
        Preference pref_export_to_ext = findPreference("pref_export_to_ext");
        pref_export_to_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                dialogFragment = new ExportDialog();
                dialogFragment.show(getFragmentManager(), "ExportDialog");
                return true;
            }
        });
    }

    private void importFromExt() {
        Preference pref_import_from_ext = findPreference("pref_import_from_ext");
        pref_import_from_ext.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                dialogFragment = new ImportDialog();
                dialogFragment.show(getFragmentManager(), "ImportDialog");
                return true;
            }
        });
    }

    private void setZoom() {
        Preference pref_zoom = findPreference("pref_zoom");
        pref_zoom.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_zoom)
                        .customView(R.layout.seekbar_dialog, false)
                        .positiveText(R.string.dialog_positive_text)
                        .iconRes(R.drawable.settings_zoom)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                sharedPref.edit().putFloat("pref_zoom", seekBarProgress).apply();

                                saveDone();
                            }
                        })
                        .build();

                seekBar = (SeekBar) dialog.getCustomView().findViewById(R.id.seekbar_seek);
                seekBarText = (TextView) dialog.getCustomView().findViewById(R.id.seekbar_text);

                seekBarCorrection = 1;
                seekBar.setMax(3);
                seekBarProgress = Math.round(sharedPref.getFloat("pref_zoom", 2));
                seekBar.setProgress(seekBarProgress - seekBarCorrection);
                seekBarText.setText((seekBar.getProgress() + seekBarCorrection) + "x " + getString(R.string.zoom_small));

                seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    int val = seekBar.getProgress();

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBarProgress = val;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                        val = progressValue + seekBarCorrection;
                        seekBarText.setText(val + "x " + getString(R.string.zoom_small));
                    }
                });

                dialog.show();

                return true;
            }
        });
    }

    private void resetLastCheck() {
        Preference pref_reset_last_check = findPreference("pref_reset_last_check");
        pref_reset_last_check.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_reset_last_check)
                        .content(R.string.reset_last_check_message)
                        .positiveText(R.string.Yes)
                        .negativeText(R.string.No)
                        .iconRes(R.drawable.settings_reset_last_check)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                sharedPref.edit().putLong("pref_last_fetch_popular", 0).apply();
                                sharedPref.edit().putLong("pref_last_fetch_latest", 0).apply();

                                Snackbar.with(context)
                                        .text(R.string.done)
                                        .actionLabel(R.string.dismiss)
                                        .actionColor(actionColor)
                                        .show(getActivity());
                            }
                        })
                        .show();

                return true;
            }
        });
    }

    private void cleanCacheAndTmpFolder() {
        Preference pref_clean_cache_and_tmp = findPreference("pref_clean_cache_and_tmp");
        pref_clean_cache_and_tmp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_clear_cache_and_tmp)
                        .content(R.string.are_you_sure)
                        .positiveText(R.string.Yes)
                        .negativeText(R.string.No)
                        .iconRes(R.drawable.settings_clear_cache_and_tmp)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                Utils.deleteCache(context);

                                deleteDone();
                            }
                        })
                        .show();

                return true;
            }
        });
    }

    private void saveToPref() {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("pref_last_fetch_popular", 0);
        editor.putLong("pref_last_fetch_latest", 0);
        editor.putInt("pref_selected_category", 0);
        editor.putString("pref_selected_category_name", getString(R.string.app_name));
        editor.apply();
    }

    private void showAbout() {
        Preference pref_about = findPreference("pref_about");
        pref_about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                dialogFragment = new AboutDialog();
                dialogFragment.show(getFragmentManager(), "AboutDialog");

                return true;
            }
        });
    }

    private void showIndeterminateProgress() {
        indeterminateProgress = new MaterialDialog.Builder(getActivity())
                .content(R.string.please_wait)
                .progress(true, 0)
                .show();
    }

    private void showDeletedSnackBar() {

        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                indeterminateProgress.dismiss();
                deleteDone();

            }
        });
    }

    private void noCategoriesFound() {
        Snackbar.with(context)
                .text(R.string.no_categories_found)
                .actionLabel(R.string.dismiss)
                .actionColor(actionColor)
                .show(getActivity());
    }

    private void deleteDone() {
        Snackbar.with(context)
                .text(R.string.action_deleted)
                .actionLabel(R.string.dismiss)
                .actionColor(actionColor)
                .show(getActivity());
    }

    private void saveDone() {
        Snackbar.with(context)
                .text(R.string.dialog_positive_toast_message)
                .actionLabel(R.string.dismiss)
                .actionColor(actionColor)
                .show(getActivity());
    }
}
