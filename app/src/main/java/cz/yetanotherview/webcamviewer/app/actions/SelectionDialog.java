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
import android.os.Bundle;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.adapter.SelectionAdapter;

public class SelectionDialog extends DialogFragment {

    private final int[] mIcons = {R.drawable.icon_popular, R.drawable.icon_nearby, R.drawable.icon_selected,
            R.drawable.icon_country, R.drawable.icon_mountains, R.drawable.icon_map, R.drawable.icon_live_streams,
            R.drawable.icon_latest};

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        return new MaterialDialog.Builder(getActivity())
                .items(R.array.selection_values)
                .adapter(new SelectionAdapter(getActivity(), R.array.selection_values, mIcons),
                        new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                DialogFragment fetcher = new JsonFetcherDialog();
                                Bundle bundle = new Bundle();
                                bundle.putInt("selection", which);
                                fetcher.setArguments(bundle);
                                fetcher.show(getFragmentManager(), "JsonFetcherDialog");
                                dialog.dismiss();
                            }
                        })
                .build();
    }
}
