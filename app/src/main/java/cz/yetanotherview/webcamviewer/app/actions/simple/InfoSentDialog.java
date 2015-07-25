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

package cz.yetanotherview.webcamviewer.app.actions.simple;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;

import cz.yetanotherview.webcamviewer.app.R;

public class InfoSentDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        int action = bundle.getInt("action", 0);

        String content = getString(R.string.stay_tuned_content_approval);
        if (action == 1) {
            content = getString(R.string.stay_tuned_content_wrong);
        }
        else if (action == 2) {
            content = getString(R.string.stay_tuned_content_location);
        }

        return new MaterialDialog.Builder(getActivity())
                .title(R.string.sent)
                .content(content)
                .positiveText(android.R.string.ok)
                .iconRes(R.drawable.settings_about)
                .build();
    }
}
