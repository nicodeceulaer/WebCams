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

package cz.yetanotherview.webcamviewer.app.helper;

import android.content.Context;
import android.os.AsyncTask;

import com.bumptech.glide.Glide;

public class ClearImageCache extends AsyncTask<Void, Void, Long> {

    private Context mContext;
    public ClearImageCache (Context context){
        mContext = context;
    }

    @Override
    protected Long doInBackground(Void... voids) {
        Glide.get(mContext).clearDiskCache();
        return null;
    }

}
