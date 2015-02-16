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

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import cz.yetanotherview.webcamviewer.app.R;

public class WelcomeDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String VERSION_UNAVAILABLE = "N/A";

        // Get app version
        PackageManager pm = getActivity().getPackageManager();
        String packageName = getActivity().getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        return new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.welcome_to_version) + " " + versionName + "!")
                .content(R.string.welcome_content)
                .positiveText(R.string.import_webcams_now)
                .neutralText(R.string.close)
                .negativeText(R.string.show_me_help)
                .forceStacking(true)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        DialogFragment selection = new SelectionDialog();
                        selection.show(getFragmentManager(), "SelectionDialog");
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://youtu.be/Xcp0j2vwbxI"));
                        startActivity(browserIntent);
                    }

                })
                .build();
    }
}
