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

package cz.yetanotherview.webcamviewer.app.actions;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.adapter.IconAdapter;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.helper.OnTextChange;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.Icons;

public class EditCategoryDialog extends DialogFragment {

    private String inputName, iconPath;
    private int position;
    private View positiveAction;
    private EditText input;
    private Category category;
    private Icons icons;
    private ImageView category_icon;
    private MaterialDialog gridDialog;
    private DatabaseHelper db;
    private Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        position = bundle.getInt("position", 0);
        int categoryId = bundle.getInt("categoryId", 0);

        db = new DatabaseHelper(mActivity);
        category = db.getCategory(categoryId);

        MaterialDialog dialog = new MaterialDialog.Builder(mActivity)
                .title(R.string.edit_category)
                .customView(R.layout.add_edit_category_dialog, true)
                .positiveText(R.string.dialog_positive_text)
                .negativeText(android.R.string.cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        inputName = input.getText().toString().trim();
                        if (iconPath == null) {
                            iconPath = category.getCategoryIcon();
                        }
                        category.setCategoryIcon(iconPath);
                        category.setCategoryName(inputName);
                        category.setCount(db.getCategoryItemsCount(category.getId()));
                        db.updateCategory(category);
                        db.closeDB();

                        reloadHost();
                    }
                }).build();

        category_icon = (ImageView) dialog.findViewById(R.id.category_icon);

        String iconName = category.getCategoryIcon();
        if (iconName == null) {
            iconName = "@drawable/icon_unknown";
        }
        category_icon.setImageResource(mActivity.getResources().getIdentifier(iconName,
                null, mActivity.getPackageName()));
        category_icon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                icons = new Icons();

                View view = mActivity.getLayoutInflater().inflate(R.layout.icon_selector_grid, null);
                GridView gridView = (GridView) view.findViewById(R.id.icons_grid_view);
                gridView.setAdapter(new IconAdapter(mActivity, icons.getIconsIds()));
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        category_icon.setImageResource(icons.getIconId(position));
                        iconPath = "@drawable/icon_" + icons.getIconName(position);
                        positiveAction.setEnabled(true);
                        gridDialog.dismiss();
                    }
                });
                gridDialog = new MaterialDialog.Builder(mActivity)
                        .customView(gridView, false)
                        .build();

                gridDialog.show();
            }
        });

        input = (EditText) dialog.findViewById(R.id.category_name);
        input.requestFocus();
        input.setText(category.getCategoryName());

        positiveAction = dialog.getActionButton(DialogAction.POSITIVE);
        input.addTextChangedListener(new OnTextChange(positiveAction));

        positiveAction.setEnabled(false);

        return dialog;
    }

    private void reloadHost() {
//        NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment)
//                getFragmentManager().findFragmentById(R.id.fragment_drawer);
//        if (mNavigationDrawerFragment != null) {
//            mNavigationDrawerFragment.editData(position, category);
//        }
//        CategoryDialog categoryDialog = (CategoryDialog) getFragmentManager().findFragmentByTag("CategoryDialog");
//        if (categoryDialog != null) {
//            categoryDialog.editCategoryInAdapter(position, category);
//            if (mNavigationDrawerFragment != null) {
//                mNavigationDrawerFragment.reloadData();
//            }
//        }
    }
}
