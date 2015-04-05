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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.signature.StringSignature;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import cz.yetanotherview.webcamviewer.app.actions.AddDialog;
import cz.yetanotherview.webcamviewer.app.actions.EditDialog;
import cz.yetanotherview.webcamviewer.app.actions.JsonFetcherDialog;
import cz.yetanotherview.webcamviewer.app.actions.SaveDialog;
import cz.yetanotherview.webcamviewer.app.actions.SelectionDialog;
import cz.yetanotherview.webcamviewer.app.actions.ShareDialog;
import cz.yetanotherview.webcamviewer.app.actions.SuggestionDialog;
import cz.yetanotherview.webcamviewer.app.actions.WelcomeDialog;
import cz.yetanotherview.webcamviewer.app.adapter.CategoryAdapter;
import cz.yetanotherview.webcamviewer.app.fullscreen.FullScreenActivity;
import cz.yetanotherview.webcamviewer.app.adapter.WebCamAdapter;
import cz.yetanotherview.webcamviewer.app.helper.DatabaseHelper;
import cz.yetanotherview.webcamviewer.app.helper.WebCamListener;
import cz.yetanotherview.webcamviewer.app.model.Category;
import cz.yetanotherview.webcamviewer.app.model.KnownLocation;
import cz.yetanotherview.webcamviewer.app.model.WebCam;

public class MainActivity extends ActionBarActivity implements WebCamListener, JsonFetcherDialog.ReloadInterface, SwipeRefreshLayout.OnRefreshListener {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();
    protected Object mActionMode;

    private DatabaseHelper db;
    private WebCam webCam;
    private List<WebCam> allWebCams;
    private List<Category> allCategories;
    private Category allWebCamsCategory;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private WebCamAdapter mAdapter;
    private float zoom;
    private int numberOfColumns, mOrientation, selectedCategory, autoRefreshInterval, mPosition;
    private boolean firstRun, fullScreen, autoRefresh, autoRefreshFullScreenOnly, screenAlwaysOn,
            notUndo, imagesOnOff, simpleList;
    private String allWebCamsString, allWebCamsTitle, selectedCategoryName, mStringSignature;
    private String sortOrder = "position";
    private FloatingActionsMenu floatingActionsMenu;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private CategoryAdapter mArrayAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private SwipeRefreshLayout swipeLayout;
    private Toolbar mToolbar;
    private MaterialDialog dialog;
    private DialogFragment dialogFragment;
    private MenuItem searchItem;
    private SearchView searchView;
    private View shadowView;
    private EventListener eventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // loading saved preferences
        allWebCamsString = getString(R.string.all_webcams);
        allWebCamsTitle = getString(R.string.app_name);
        loadPref();

        // Inflating main layout
        setContentView(R.layout.activity_main);
        mEmptyView = findViewById(R.id.empty);

        // Auto Refreshing
        if (autoRefresh && !autoRefreshFullScreenOnly) {
            autoRefreshTimer(autoRefreshInterval);
        }

        // Go FullScreen only on KitKat and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && fullScreen) {
            goFullScreen();
        }

        // First run
        if (firstRun){
            showWelcomeDialog();
            // Save the state
            firstRun = false;
            saveToPref();
        }

        // Screen Always on
        if (screenAlwaysOn){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Init DatabaseHelper for late use
        db = new DatabaseHelper(getApplicationContext());

        // Get current orientation
        mOrientation = getResources().getConfiguration().orientation;

        // New signature
        mStringSignature = UUID.randomUUID().toString();

        // Other core init
        initToolbar();
        loadCategories();
        initDrawer();
        initRecyclerView();
        initFab();
        initPullToRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.clearImageCache(getApplicationContext());
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        if (selectedCategoryName.contains(allWebCamsString)) {
            mToolbar.setTitle(allWebCamsTitle);
        }
        else {
            mToolbar.setTitle(selectedCategoryName);
        }
        setSupportActionBar(mToolbar);
    }

    private void loadCategories() {
        allCategories = db.getAllCategories();
        for (Category category : allCategories) {
            category.setCount(db.getCategoryItemsCount(category.getId()));
        }

        allWebCamsCategory = new Category();
        allWebCamsCategory.setCategoryIcon("@drawable/icon_all");
        allWebCamsCategory.setCategoryName(allWebCamsString);
        allWebCamsCategory.setCount(db.getWebCamCount());
        db.closeDB();
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = (ListView) findViewById(R.id.drawer);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);


        if (mDrawerList != null) {
            ArrayList<Category> arrayOfCategories = new ArrayList<>();
            mArrayAdapter = new CategoryAdapter(this, arrayOfCategories);

            mArrayAdapter.add(allWebCamsCategory);
            mArrayAdapter.addAll(allCategories);

            mDrawerList.setAdapter(mArrayAdapter);
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        }
        db.closeDB();

        if (selectedCategoryName.contains(allWebCamsString)) {
            mDrawerList.setItemChecked(0, true);
        }
        else {
            mDrawerList.setItemChecked(selectedCategory, true);
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void initRecyclerView() {
        int mLayoutId = 1;
        if (numberOfColumns == 1 && mOrientation == 1) {
            mLayoutId = 1;
        }
        else if(numberOfColumns == 1 && mOrientation == 2) {
            mLayoutId = 2;
        }
        else if(numberOfColumns == 2 && mOrientation == 1) {
            mLayoutId = 2;
        }
        else if(numberOfColumns == 2 && mOrientation == 2) {
            mLayoutId = 3;
        }
        RecyclerView.LayoutManager mLayoutManager = new StaggeredGridLayoutManager(mLayoutId, StaggeredGridLayoutManager.VERTICAL);

        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mRecyclerView.setLayoutManager(mLayoutManager);
        if (selectedCategory == 0) {
            allWebCams = db.getAllWebCams(sortOrder);
            selectedCategoryName = allWebCamsString;
        }
        else {
            Category category = allCategories.get(selectedCategory - 1);
            allWebCams = db.getAllWebCamsByCategory(category.getId(),sortOrder);
            selectedCategoryName = category.getCategoryName();
        }
        db.closeDB();

        mAdapter = new WebCamAdapter(this, allWebCams, mOrientation, mLayoutId,
                new StringSignature(mStringSignature), imagesOnOff, simpleList);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setClickListener(new WebCamAdapter.ClickListener() {

            @Override
            public void onClick(View v, int position, boolean isEditClick, boolean isLongClick) {
                if (isEditClick) {
                    showOptionsDialog(position);
                }
                else if (isLongClick) {
                    mPosition = position;
                    moveItem();
                }
                else showImageFullscreen(position, false);
            }
        });

        checkAdapterIsEmpty();
    }

    private void initFab() {
        floatingActionsMenu = (FloatingActionsMenu) findViewById(R.id.fab);
        shadowView = findViewById(R.id.shadowView);

        FloatingActionsMenu.OnFloatingActionsMenuUpdateListener listener = new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                shadowView.animate()
                        .setDuration(400)
                        .alpha(1.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                shadowView.setVisibility(View.VISIBLE);
                            }
                        });
            }

            @Override
            public void onMenuCollapsed() {
                shadowView.animate()
                        .setDuration(400)
                        .alpha(0.0f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                shadowView.setVisibility(View.GONE);
                            }
                        });
            }
        };
        floatingActionsMenu.setOnFloatingActionsMenuUpdateListener(listener);
        eventListener = new EventListener() {
            @Override
            public void onShow(Snackbar snackbar) {
                floatingActionsMenu.animate().translationYBy(-snackbar.getHeight());
            }

            @Override
            public void onShowByReplace(Snackbar snackbar) {
            }

            @Override
            public void onShown(Snackbar snackbar) {
            }

            @Override
            public void onDismiss(Snackbar snackbar) {
            }

            @Override
            public void onDismissByReplace(Snackbar snackbar) {
            }

            @Override
            public void onDismissed(Snackbar snackbar) {
                floatingActionsMenu.animate().translationYBy(snackbar.getHeight());
            }
        };
    }

    public void hideShadowView(View view) {
        shadowView.animate()
                .setDuration(400)
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        shadowView.setVisibility(View.GONE);
                    }
                });
        floatingActionsMenu.collapse();
    }

    private void initPullToRefresh() {
        // Pull To Refresh 1/2
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.primary, R.color.swipe, R.color.yellow);
    }

    private void checkAdapterIsEmpty () {
        if (mAdapter.getItemCount() == 0) {
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectedCategory = position;
            reInitializeRecyclerViewAdapter(position);
            reInitializeDrawerListAdapter();

            if (selectedCategoryName.contains(allWebCamsString)) {
                getSupportActionBar().setTitle(allWebCamsTitle);
            }
            else {
                getSupportActionBar().setTitle(selectedCategoryName);
            }
            mDrawerLayout.closeDrawers();
        }
    }

    private void reInitializeRecyclerViewAdapter(int position) {
        if (db.getWebCamCount() != 0) {
            allWebCams.clear();
            if (position == 0) {
                allWebCams = db.getAllWebCams(sortOrder);
                mAdapter.swapData(allWebCams);
                selectedCategoryName = allWebCamsString;
            }
            else {
                Category category = allCategories.get(position - 1);
                allWebCams = db.getAllWebCamsByCategory(category.getId(),sortOrder);
                mAdapter.swapData(allWebCams);
                selectedCategoryName = category.getCategoryName();
            }
            db.closeDB();
            saveToPref();
            checkAdapterIsEmpty();
        }
    }

    private void reInitializeDrawerListAdapter() {
        int checked = mDrawerList.getCheckedItemPosition();
        loadCategories();
        mArrayAdapter.clear();
        mArrayAdapter.add(allWebCamsCategory);
        mArrayAdapter.addAll(allCategories);
        mDrawerList.setAdapter(mArrayAdapter);
        mDrawerList.setItemChecked(checked, true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        searchItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                searchView.setIconified(false);
                searchView.requestFocus();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.clearFocus();
                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                searchItem.collapseActionView();
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mAdapter.filter(newText);
                return true;
            }
        });

        MenuItem dashboard = menu.findItem(R.id.action_dashboard);
        if (numberOfColumns == 1) {
            dashboard.setIcon(R.drawable.ic_action_dashboard);
        }
        else dashboard.setIcon(R.drawable.ic_action_view_day);

        MenuItem imagesOnOffItem = menu.findItem(R.id.action_image_on_off);
        if (imagesOnOff) {
            imagesOnOffItem.setTitle(R.string.images_off);
            imagesOnOffItem.setIcon(R.drawable.ic_action_image_off);
        }
        else {
            imagesOnOffItem.setTitle(R.string.images_on);
            imagesOnOffItem.setIcon(R.drawable.ic_action_image_on);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {

            //Search
            case R.id.action_search:
                return super.onOptionsItemSelected(item);

            //Refresh
            case R.id.action_refresh:
                refresh(false);
                break;

            //View
            case R.id.action_dashboard:
                if (numberOfColumns == 1) {
                    numberOfColumns = 2;
                    item.setIcon(R.drawable.ic_action_view_day);
                }
                else if (numberOfColumns == 2) {
                    numberOfColumns = 1;
                    item.setIcon(R.drawable.ic_action_dashboard);
                }
                initRecyclerView();
                saveToPref();
                break;

            //Sort view
            case R.id.action_sort:
                showSortDialog();
                break;

            //Images on/off
            case R.id.action_image_on_off:
                if (imagesOnOff) {
                    imagesOnOff = false;
                    item.setTitle(R.string.images_on);
                    item.setIcon(R.drawable.ic_action_image_on);
                }
                else {
                    imagesOnOff = true;
                    item.setTitle(R.string.images_off);
                    item.setIcon(R.drawable.ic_action_image_off);
                }
                initRecyclerView();
                saveToPref();
                break;

            //Settings
            case R.id.action_settings:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SettingsActivity.class);
                startActivityForResult(intent, 0);
                selectedCategoryName = allWebCamsString;
                saveToPref();
                break;

            //Help
            case R.id.menu_help:
                Intent helpIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://youtu.be/Xcp0j2vwbxI"));
                startActivity(helpIntent);
                break;

            //Donate
            case R.id.menu_donate:
                String mLoc;
                String mDon = ".html#donatingTable";
                String mLocale = Utils.getLocaleCode();
                if (mLocale.equals("cs_CZ")) {
                    mLoc = "cs";
                }
                else mLoc = "en";
                String mDonUrl = "http://www.yetanotherview.cz/wcv/" + mLoc + mDon;
                Intent donateIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mDonUrl));
                startActivity(donateIntent);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        MenuItemCompat.expandActionView(searchItem);
        searchView.requestFocus();
        return true;
    }

    private void showSortDialog() {

        int whatMarkToCheck = 3;
        if (sortOrder.contains(" ) ASC")) {
            whatMarkToCheck = 0;
        }
        else if (sortOrder.contains(" ) DESC")) {
            whatMarkToCheck = 1;
        }
        else if (sortOrder.contains("position")) {
            whatMarkToCheck = 2;
        }
        else if (sortOrder.contains("created_at ASC")) {
            whatMarkToCheck = 3;
        }
        else if (sortOrder.contains("created_at DESC")) {
            whatMarkToCheck = 4;
        }
        else if (sortOrder.contains("UNICODE ASC")) {
            whatMarkToCheck = 5;
        }
        else if (sortOrder.contains("UNICODE DESC")) {
            whatMarkToCheck = 6;
        }

        dialog = new MaterialDialog.Builder(this)
                .title(R.string.action_sort)
                .items(R.array.sort_values)
                .itemsCallbackSingleChoice(whatMarkToCheck, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

                        KnownLocation knownLocation;
                        Double fudge;
                        switch (which) {
                            case 0:
                                knownLocation = Utils.getLastKnownLocation(getApplicationContext());
                                fudge = Math.pow(Math.cos(Math.toRadians(knownLocation.getLatitude())), 2);

                                sortOrder = "((" + knownLocation.getLatitude() + " - latitude) * (" +
                                        knownLocation.getLatitude() + " - latitude) + (" + knownLocation.getLongitude() +
                                        " - longitude) * (" + knownLocation.getLongitude() + " - longitude) * " + fudge + " ) ASC";
                                if (knownLocation.isNotDetected()) {
                                    showLocationWarningDialog();
                                }
                                break;
                            case 1:
                                knownLocation = Utils.getLastKnownLocation(getApplicationContext());
                                fudge = Math.pow(Math.cos(Math.toRadians(knownLocation.getLatitude())), 2);

                                sortOrder = "((" + knownLocation.getLatitude() + " - latitude) * (" +
                                        knownLocation.getLatitude() + " - latitude) + (" + knownLocation.getLongitude() +
                                        " - longitude) * (" + knownLocation.getLongitude() + " - longitude) * " + fudge + " ) DESC";
                                if (knownLocation.isNotDetected()) {
                                    showLocationWarningDialog();
                                }
                                break;
                            case 2:
                                sortOrder = "position";
                                break;
                            case 3:
                                sortOrder = "created_at ASC";
                                break;
                            case 4:
                                sortOrder = "created_at DESC";
                                break;
                            case 5:
                                sortOrder = "webcam_name COLLATE UNICODE ASC";
                                break;
                            case 6:
                                sortOrder = "webcam_name COLLATE UNICODE DESC";
                                break;
                            default:
                                break;
                        }
                        reInitializeRecyclerViewAdapter(selectedCategory);

                        return true;
                    }
                })
                .negativeText(R.string.close)
                .show();
    }

    private void showLocationWarningDialog() {
        dialog = new MaterialDialog.Builder(this)
                .title(R.string.no_location)
                .content(R.string.no_location_description)
                .neutralText(R.string.location_settings)
                .positiveText(android.R.string.ok)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNeutral(MaterialDialog dialog) {
                        Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(viewIntent);
                    }
                })
                .show();
    }

    private void showWelcomeDialog() {
        dialogFragment = new WelcomeDialog();
        dialogFragment.show(getFragmentManager(), "WelcomeDialog");
    }

    private void showImageFullscreen(int position, boolean map) {
        webCam = (WebCam) mAdapter.getItem(position);

        Intent intent = new Intent(this, FullScreenActivity.class);
        intent.putExtra("signature", mStringSignature);
        intent.putExtra("map", map);
        intent.putExtra("name", webCam.getName());
        intent.putExtra("url", webCam.getUrl());
        intent.putExtra("latitude", webCam.getLatitude());
        intent.putExtra("longitude", webCam.getLongitude());
        intent.putExtra("zoom", zoom);
        intent.putExtra("fullScreen", fullScreen);
        intent.putExtra("autoRefresh", autoRefresh);
        intent.putExtra("interval", autoRefreshInterval);
        intent.putExtra("screenAlwaysOn", screenAlwaysOn);

        if (!map){
            startActivity(intent);
        }
        else {
            if (webCam.getLatitude() != 0 && webCam.getLongitude() != 0) {
                startActivity(intent);
            }
            else {
                new MaterialDialog.Builder(this)
                        .title(R.string.no_coordinates)
                        .content(R.string.no_coordinates_summary)
                        .positiveText(android.R.string.ok)
                        .show();
            }
        }
    }

    private void showOptionsDialog(final int position) {
        webCam = (WebCam) mAdapter.getItem(position);

        String[] options_values = getResources().getStringArray(R.array.opt_values);
        if (webCam.getUniId() != 0) {
            options_values[7] = getString(R.string.report_problem);
        }
        else {
            options_values[7] = getString(R.string.submit_to_appr);
        }

        dialog = new MaterialDialog.Builder(this)
                .items(options_values)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        switch (which) {
                            case 0:
                                showEditDialog(position);
                                break;
                            case 1:
                                webCamDeleted(webCam, position);
                                break;
                            case 2:
                                mPosition = position;
                                moveItem();
                                break;
                            case 3:
                                showImageFullscreen(position, false);
                                break;
                            case 4:
                                SaveDialog saveDialog = new SaveDialog();
                                Bundle saveDialogBundle = new Bundle();
                                saveDialogBundle.putString("name", webCam.getName());
                                saveDialogBundle.putString("url", webCam.getUrl());
                                saveDialog.setArguments(saveDialogBundle);
                                saveDialog.show(getFragmentManager(), "SaveDialog");
                                break;
                            case 5:
                                ShareDialog shareDialog = new ShareDialog();
                                Bundle shareDialogBundle = new Bundle();
                                shareDialogBundle.putString("url", webCam.getUrl());
                                shareDialog.setArguments(shareDialogBundle);
                                shareDialog.show(getFragmentManager(), "ShareDialog");
                                break;
                            case 6:
                                showImageFullscreen(position, true);
                                break;
                            case 7:
                                if (webCam.getUniId() != 0) {
                                    sendEmail(webCam, true);
                                } else sendEmail(webCam, false);
                                break;
                            default:
                                break;
                        }

                    }
                })
                .show();
    }

    public void showSelectionDialog(View view) {
        dialogFragment = new SelectionDialog();
        dialogFragment.show(getFragmentManager(), "SelectionDialog");
        hideAfterDelay();
    }

    public void showAddDialog(View view) {
        dialogFragment = AddDialog.newInstance(this);
        dialogFragment.show(getFragmentManager(), "AddDialog");
        hideAfterDelay();
    }

    public void showSuggestionDialog(View view) {
        dialogFragment = new SuggestionDialog();
        dialogFragment.show(getFragmentManager(), "SuggestionDialog");
        hideAfterDelay();
    }

    private void hideAfterDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                floatingActionsMenu.collapse();
            }
        }, 500);
    }

    private void showEditDialog(int position) {
        dialogFragment = EditDialog.newInstance(this);

        webCam = (WebCam) mAdapter.getItem(position);
        Bundle bundle = new Bundle();
        bundle.putLong("id", webCam.getId());
        bundle.putInt("position", position);
        dialogFragment.setArguments(bundle);

        dialogFragment.show(getFragmentManager(), "EditDialog");
    }

    @Override
    public void webCamAdded(WebCam wc, long[] category_ids, boolean share) {
        synchronized (sDataLock) {
            if (category_ids != null) {
                wc.setId(db.createWebCam(wc, category_ids));
            }
            else {
                wc.setId(db.createWebCam(wc, null));
            }
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.addItem(mAdapter.getItemCount(), wc);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);

        checkAdapterIsEmpty();
        reInitializeDrawerListAdapter();

        if (share) {
            sendEmail(wc, false);
        }
        else saveDone();
    }

    @Override
    public void webCamEdited(int position, WebCam wc, long[] category_ids) {
        synchronized (sDataLock) {
            if (category_ids != null) {
                db.updateWebCam(wc, category_ids);
            }
            else {
                db.updateWebCam(wc, null);
            }
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.modifyItem(position,wc);
        reInitializeDrawerListAdapter();

        saveDone();
    }

    @Override
    public void webCamDeleted(final WebCam wc, final int position) {
        notUndo = true;

        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            mAdapter.removeItem(mAdapter.getItemAt(position));
        }

        checkAdapterIsEmpty();
        reInitializeDrawerListAdapter();

        SnackbarManager.show(
                Snackbar.with(getApplicationContext())
                        .text(R.string.action_deleted)
                        .actionLabel(R.string.undo)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .actionListener(new ActionClickListener() {
                            @Override
                            public void onActionClicked(Snackbar snackbar) {
                                mAdapter.addItem(position, wc);
                                checkAdapterIsEmpty();
                                reInitializeDrawerListAdapter();
                                notUndo = false;
                                floatingActionsMenu.animate().translationYBy(snackbar.getHeight());
                            }
                        })
                        .eventListener(new EventListener() {
                            @Override
                            public void onShow(Snackbar snackbar) {
                                floatingActionsMenu.animate().translationYBy(-snackbar.getHeight());
                            }

                            @Override
                            public void onShowByReplace(Snackbar snackbar) {
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                            }

                            @Override
                            public void onDismiss(Snackbar snackbar) {
                            }

                            @Override
                            public void onDismissByReplace(Snackbar snackbar) {
                            }

                            @Override
                            public void onDismissed(Snackbar snackbar) {
                                if (notUndo) {
                                    new deleteWebCamBackgroundTask().execute(wc.getId());
                                    floatingActionsMenu.animate().translationYBy(snackbar.getHeight());
                                }
                            }
                        })
                , this);
    }

    private class deleteWebCamBackgroundTask extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(Long... longs) {

            synchronized (sDataLock) {
                db.deleteWebCam(longs[0]);
                db.closeDB();
            }
            BackupManager backupManager = new BackupManager(getApplicationContext());
            backupManager.dataChanged();
            this.publishProgress();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            reInitializeDrawerListAdapter();
        }
    }

    @Override
    public void invokeReload() {
        reInitializeRecyclerViewAdapter(selectedCategory);
        reInitializeDrawerListAdapter();
    }

    private void moveItem() {

        mToolbar.startActionMode(new ActionMode.Callback() {

            int pos = mPosition;
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.move_menu, menu);
                mode.setTitle(R.string.move);
                return true;
            }

            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.up:
                        mAdapter.moveItemUp(mAdapter.getItemAt(pos));
                        if (pos > 0) {
                            pos = pos - 1;
                        }
                        return true;
                    case R.id.down:
                        mAdapter.moveItemDown(mAdapter.getItemAt(pos));
                        if (pos < (mAdapter.getItemCount() - 1)) {
                            pos = pos + 1;
                        }
                        return true;
                    case R.id.done:
                        sortOrder = "position";
                        new savePositionsToDB().execute();
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            public void onDestroyActionMode(ActionMode mode) {
                mActionMode = null;
            }
        });
    }

    private class savePositionsToDB extends AsyncTask<Long, Void, Void> {

        @Override
        protected Void doInBackground(Long... longs) {

            List<WebCam> newWebCams = mAdapter.getItems();
            synchronized (sDataLock) {
                int i = 0;
                for (WebCam mWebCam : newWebCams) {
                    mWebCam.setPosition(i);
                    db.updateWebCamPosition(mWebCam);
                    i++;
                }
            }
            db.closeDB();
            BackupManager backupManager = new BackupManager(getApplicationContext());
            backupManager.dataChanged();
            this.publishProgress();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            saveDone();
        }
    }

    private void saveDone() {
        SnackbarManager.show(
                Snackbar.with(getApplicationContext())
                        .text(R.string.dialog_positive_toast_message)
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .eventListener(eventListener)
                , this);
    }

     private void refreshIsRunning() {
         SnackbarManager.show(
                 Snackbar.with(getApplicationContext())
                         .text(R.string.refresh_is_running)
                         .actionLabel(R.string.dismiss)
                         .actionColor(getResources().getColor(R.color.yellow))
                         .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                         .eventListener(eventListener)
                 , this);
    }

    private void nothingToRefresh() {
        SnackbarManager.show(
                Snackbar.with(getApplicationContext())
                        .text(R.string.nothing_to_refresh)
                        .actionLabel(R.string.dismiss)
                        .actionColor(getResources().getColor(R.color.yellow))
                        .duration(Snackbar.SnackbarDuration.LENGTH_SHORT)
                        .eventListener(eventListener)
                , this);
    }

    // Pull To Refresh 2/2
    @Override
    public void onRefresh() {
        refresh(false);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(false);
            }
        }, 2500);
    }

    private void refresh(boolean fromAutoRefresh) {
        mStringSignature = UUID.randomUUID().toString();
        mAdapter.refreshViewImages(new StringSignature(mStringSignature));
        if (!fromAutoRefresh) {
            if (mAdapter.getItemCount() != 0) {
                refreshIsRunning();
            }
            else nothingToRefresh();
        }
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
                            refresh(true);
                        } catch (Exception e) {
                            // Auto-generated catch block
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, interval);
    }

    private void sendEmail(WebCam webCam, boolean fromCommunityList) {

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", Utils.email, null));

        if (fromCommunityList) {
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Something is wrong");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "WebCam UniID: " + webCam.getUniId()
                    + "\n" + "WebCam Name: " + webCam.getName());
        }
        else {
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "New WebCam for approval");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "WebCam Name: " + webCam.getName()
                    + "\n" + "WebCam URL: " + webCam.getUrl() + "\n" + "WebCam Coordinates: "
                    + webCam.getLatitude() + " - " + webCam.getLongitude());
        }

        List<ResolveInfo> list = getPackageManager().queryIntentActivities(emailIntent, 0);
        if(list.isEmpty()) {
            noEmailClientsFound();
        }
        else {
            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_via_email)));
            } catch (android.content.ActivityNotFoundException ex) {
                noEmailClientsFound();
            }
        }
    }

    private void noEmailClientsFound() {
        dialog = new MaterialDialog.Builder(this)
                .title(R.string.oops)
                .content(getString(R.string.no_email_clients_installed))
                .positiveText(android.R.string.ok)
                .show();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullScreen) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void goFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void loadPref(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        firstRun = preferences.getBoolean("pref_first_run", true);
        numberOfColumns = preferences.getInt("number_of_columns", 1);
        imagesOnOff = preferences.getBoolean("pref_images_on_off", true);
        fullScreen = preferences.getBoolean("pref_full_screen", false);
        autoRefresh = preferences.getBoolean("pref_auto_refresh", false);
        autoRefreshInterval = preferences.getInt("pref_auto_refresh_interval", 30000);
        autoRefreshFullScreenOnly = preferences.getBoolean("pref_auto_refresh_fullscreen", false);
        zoom = preferences.getFloat("pref_zoom", 2);
        selectedCategory = preferences.getInt("pref_selected_category", 0);
        selectedCategoryName = preferences.getString("pref_selected_category_name", allWebCamsString);
        screenAlwaysOn = preferences.getBoolean("pref_screen_always_on", false);
        simpleList = preferences.getBoolean("pref_simple_list", false);
    }

    private void saveToPref(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("pref_first_run", firstRun);
        editor.putInt("number_of_columns", numberOfColumns);
        editor.putBoolean("pref_images_on_off", imagesOnOff);
        editor.putInt("pref_selected_category", selectedCategory);
        editor.putString("pref_selected_category_name", selectedCategoryName);
        editor.apply();
    }
}
