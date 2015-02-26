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

package cz.yetanotherview.webcamviewer.app.fullscreen;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.PicassoTools;

import java.util.Timer;
import java.util.TimerTask;

import cz.yetanotherview.webcamviewer.app.R;
import cz.yetanotherview.webcamviewer.app.actions.SaveDialog;
import cz.yetanotherview.webcamviewer.app.actions.ShareDialog;

public class FullScreenFragment extends Fragment {

    private View view;
    private RelativeLayout mButtonsLayout;
    private TouchImageView image;
    private ProgressBar progressBar;
    private Animation fadeOut;
    private String name;
    private String url;
    private float zoom;
    private boolean autoRefresh;
    private int autoRefreshInterval;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.full_screen_layout, container, false);

        mButtonsLayout = (RelativeLayout) view.findViewById(R.id.buttons_layout);

        Intent intent = getActivity().getIntent();
        name = intent.getExtras().getString("name");
        url = intent.getExtras().getString("url");
        zoom = intent.getExtras().getFloat("zoom");
        autoRefresh = intent.getExtras().getBoolean("autoRefresh");
        autoRefreshInterval = intent.getExtras().getInt("interval");

        // Auto Refresh timer
        if (autoRefresh) {
            autoRefreshTimer(autoRefreshInterval);
        }

        initViews();
        loadImage();
        setAnimation();

        return view;
    }

    private void initViews() {
        image = (TouchImageView) view.findViewById(R.id.touch_image);
        image.setMaxZoom(zoom);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButtonsLayout.setVisibility(View.VISIBLE);
                mButtonsLayout.startAnimation(fadeOut);
            }
        });

        ImageButton mapButton = (ImageButton) view.findViewById(R.id.maps_button);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FullScreenActivity) getActivity()).replaceFragments(true);
            }
        });

        ImageButton shareButton = (ImageButton) view.findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareDialog shareDialog = new ShareDialog();
                Bundle shareDialogBundle = new Bundle();
                shareDialogBundle.putString("url", url);
                shareDialog.setArguments(shareDialogBundle);
                shareDialog.show(getFragmentManager(), "ShareDialog");
            }
        });

        ImageButton refreshButton = (ImageButton) view.findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        ImageButton saveButton = (ImageButton) view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new SaveDialog();
                Bundle bundle = new Bundle();
                bundle.putString("name", name);
                bundle.putString("url", url);
                newFragment.setArguments(bundle);
                newFragment.show(getFragmentManager(), "SaveDialog");
            }
        });

        ImageButton backButton = (ImageButton) view.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        progressBar = (ProgressBar) view.findViewById(R.id.loadingProgressBar);
    }

    private void setAnimation() {
        fadeOut = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.fade_out);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mButtonsLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void loadImage() {
        //Picasso.with(image.getContext()).setIndicatorsEnabled(true);
        Picasso.with(image.getContext())
                .load(url)
                .fit()
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder_error)
                .into(image, new Callback() {
                    @Override
                    public void onSuccess() {
                        progressBar.setVisibility(View.GONE);
                        mButtonsLayout.startAnimation(fadeOut);
                        mButtonsLayout.setBackgroundResource(R.drawable.selector);
                    }

                    @Override
                    public void onError() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void autoRefreshTimer(int interval) {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            refresh();
                        } catch (Exception e) {
                            // Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, interval);
    }

    private void refresh() {
        PicassoTools.clearCache(Picasso.with(image.getContext()));
        mButtonsLayout.setBackgroundResource(0);
        progressBar.setVisibility(View.VISIBLE);
        loadImage();
    }
}
