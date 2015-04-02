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

package cz.yetanotherview.webcamviewer.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.bumptech.glide.signature.StringSignature;

import java.util.UUID;

import cz.yetanotherview.webcamviewer.app.R;

public class WvWidgetProvider extends AppWidgetProvider {

    public static String WIDGET_BUTTON = "cz.yetanotherview.webcamviewer.app.widget.WIDGET_BUTTON";

    private RemoteViews mRemoteViews;
    private ComponentName mComponentName;
    private AppWidgetTarget mAppWidgetTarget;

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        if (WIDGET_BUTTON.equals(intent.getAction())) {
            mRemoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);
            mComponentName = new ComponentName(context, WvWidgetProvider.class);

            mAppWidgetTarget = new AppWidgetTarget(context,mRemoteViews,R.id.wImage,400,400,mComponentName) {};

            Glide.with(context)
                    .load("http://www2.brno.cz/kamery/malinak/tmp/image.jpg")
                    .asBitmap()
                    .signature(new StringSignature(UUID.randomUUID().toString()))
                    .into(mAppWidgetTarget);
        }
    }

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        mRemoteViews = new RemoteViews(context.getPackageName(),
                R.layout.widget_layout);

        Intent intent = new Intent(WIDGET_BUTTON);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.sync_button, pendingIntent );

        mRemoteViews.setTextViewText(R.id.wTitle, "Working Example");

        mComponentName = new ComponentName(context, WvWidgetProvider.class);

        mAppWidgetTarget = new AppWidgetTarget(context,mRemoteViews,R.id.wImage,400,400,mComponentName) {};

        Glide.with(context)
                .load("http://www2.brno.cz/kamery/malinak/tmp/image.jpg")
                .asBitmap()
                .signature(new StringSignature(UUID.randomUUID().toString()))
                .into(mAppWidgetTarget);

        appWidgetManager.updateAppWidget(mComponentName, mRemoteViews);
	}
}
