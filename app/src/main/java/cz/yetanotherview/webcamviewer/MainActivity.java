/*
* ******************************************************************************
* Copyright (c) 2013-2014 Tomas Valenta.
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

package cz.yetanotherview.webcamviewer;

import android.app.DialogFragment;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import com.afollestad.materialdialogs.MaterialDialog;
import com.melnykov.fab.FloatingActionButton;
import com.nispok.snackbar.Snackbar;

import java.util.List;

import cz.yetanotherview.webcamviewer.actions.AddDialog;
import cz.yetanotherview.webcamviewer.actions.EditDialog;
import cz.yetanotherview.webcamviewer.fullscreen.FullScreenImage;
import cz.yetanotherview.webcamviewer.helper.ItemClickListener;
import cz.yetanotherview.webcamviewer.adapter.WebCamAdapter;
import cz.yetanotherview.webcamviewer.db.DatabaseHelper;
import cz.yetanotherview.webcamviewer.helper.WebCamListener;
import cz.yetanotherview.webcamviewer.model.Category;
import cz.yetanotherview.webcamviewer.model.Webcam;

public class MainActivity extends ActionBarActivity implements WebCamListener, SwipeRefreshLayout.OnRefreshListener {

    // Object for intrinsic lock
    public static final Object sDataLock = new Object();

    private DatabaseHelper db;
    private Webcam webcam;
    private List<Webcam> allWebCams;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private WebCamAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private SwipeRefreshLayout swipeLayout;
    private Spinner mSpinner;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEmptyView = findViewById(R.id.empty);

        initToolbar();
        initDrawer();
        initRecyclerView();
        initFab();
        initPullToRefresh();

    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
    }

    private void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = (ListView) findViewById(R.id.drawer);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        if (mDrawerList != null) {
            ArrayAdapter<CharSequence> mArrayAdapter = ArrayAdapter.createFromResource(this, R.array.category_array, android.R.layout.simple_list_item_1);
            mDrawerList.setAdapter(mArrayAdapter);

            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        }


        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.drawable.ic_action_content_new, R.drawable.ic_action_sort_by_size_white) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle("mTitle");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle("mDrawerTitle");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void initRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.cardList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        db = new DatabaseHelper(getApplicationContext());
        allWebCams = db.getAllWebCams();
        db.closeDB();

        mAdapter = new WebCamAdapter(allWebCams);
        mRecyclerView.setAdapter(mAdapter);

        checkAdapterIsEmpty();

        mRecyclerView.addOnItemTouchListener(new ItemClickListener(getApplicationContext(),
            mRecyclerView, new ItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    showImageFullscreen(position);
                }
                @Override
                public void onItemLongClick(View view, int position) {
                    showEditDialog(position);
                }
            })
        );
    }

    private void initFab() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(mRecyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddDialog();
            }
        });
    }

    private void initPullToRefresh() {
        // Pull To Refresh 1/2
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorSchemeResources(R.color.primary_dark);
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
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        setItemChecked(menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
         * The action bar home/up should open or close the drawer.
         * ActionBarDrawerToggle will take care of this.
         */
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {

            //Refresh
            case R.id.action_refresh:
                refresh();
                break;

            //Sort view
            case R.id.sort_def:
//                sortOrder = defSort;
//                mCardsFragment.setSortOrder(sortOrder);
//                item.setChecked(true);
//                saveToPref();
                break;
            case R.id.sort_asc:
//                sortOrder = ascSort;
//                mCardsFragment.setSortOrder(sortOrder);
//                item.setChecked(true);
//                saveToPref();
                break;
            case R.id.sort_desc:
//                sortOrder = descSort;
//                mCardsFragment.setSortOrder(sortOrder);
//                item.setChecked(true);
//                saveToPref();
                break;

            //Settings
            case R.id.action_settings:
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this, SettingsActivity.class);
//                startActivityForResult(intent, 0);
                break;

            //About
            case R.id.menu_about:
                showAbout();
//                Cursor cursor = dbManager.fetch();
//                exportToExcel(cursor);
                break;
            default:
                break;
        }

        // Handle your other action bar items...
        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        final String VERSION_UNAVAILABLE = "N/A";

        // Get app version
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        String versionName;
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = VERSION_UNAVAILABLE;
        }

        new MaterialDialog.Builder(this)
                .title(getString(R.string.app_name)+ " " + versionName)
                .content(Html.fromHtml(getString(R.string.about_body)))
                .contentLineSpacing(1)
                .positiveText(android.R.string.ok)
                .callback(new MaterialDialog.SimpleCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                    }
                })
                .icon(R.drawable.ic_launcher)
                .build()
                .show();
    }

    private void setItemChecked(Menu menu) {
        MenuItem def = menu.findItem(R.id.sort_def);
        MenuItem asc = menu.findItem(R.id.sort_asc);
        MenuItem desc = menu.findItem(R.id.sort_desc);
//        if (sortOrder.equals(defSort)) {
//            def.setChecked(true);
//        }
//        else if (sortOrder.equals(ascSort)) {
//            asc.setChecked(true);
//        }
//        else if (sortOrder.equals(descSort)) {
//            desc.setChecked(true);
//        }
    }

    private void showImageFullscreen(int position) {
        webcam = (Webcam) mAdapter.getItem(position);
        Intent fullScreenIntent = new Intent(getApplicationContext(), FullScreenImage.class);
        fullScreenIntent.putExtra("url", webcam.getUrl());
        startActivity(fullScreenIntent);
    }

    private void showAddDialog() {
        DialogFragment newFragment = AddDialog.newInstance(this);
        newFragment.show(getFragmentManager(), "AddDialog");
    }

    private void showEditDialog(int position) {
        DialogFragment newFragment = EditDialog.newInstance(this);

        webcam = (Webcam) mAdapter.getItem(position);
        Bundle bundle = new Bundle();
        bundle.putLong("id", webcam.getId());
        bundle.putInt("position", position);
        newFragment.setArguments(bundle);

        newFragment.show(getFragmentManager(), "EditDialog");
    }

    @Override
    public void webcamAdded(Webcam wc) {
        synchronized (sDataLock) {
            wc.setId(
                    db.createWebCam(wc,
                            new long[]{db.createCategory(new Category(""))})
            );
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.addItem(mAdapter.getItemCount(), wc);
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);

        checkAdapterIsEmpty();

        saveDone();
    }

    @Override
    public void webcamEdited(int position, Webcam wc) {
        synchronized (sDataLock) {
            db.updateWebCam(wc);
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        mAdapter.modifyItem(position,wc);

        saveDone();
    }

    @Override
    public void webcamDeleted(long id, int position) {
        synchronized (sDataLock) {
            db.deleteWebCam(id);
            db.closeDB();
        }
        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();

        if (mAdapter != null && mAdapter.getItemCount() > 0) {
            mAdapter.removeItem(mAdapter.getItemAt(position));
        }

        checkAdapterIsEmpty();

        delDone();
    }

    private void saveDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.dialog_positive_toast_message)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .show(this);
    }

    private void delDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.action_deleted)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .show(this);
    }

    private void refreshDone() {
        Snackbar.with(getApplicationContext())
                .text(R.string.refresh_done)
                .actionLabel(R.string.dismiss)
                .actionColor(getResources().getColor(R.color.yellow))
                .show(this);
    }

    // Pull To Refresh 2/2
    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                swipeLayout.setRefreshing(false);
                refresh();
            }
        }, 600);
    }

    private void refresh() {
        Utils.deletePicassoCache(getApplicationContext().getCacheDir());
        mAdapter.notifyDataSetChanged();
        refreshDone();
    }

//    /**
//     * Exports the cursor value to an excel sheet.
//     * Recommended to call this method in a separate thread,
//     * especially if you have more number of threads.
//     *
//     * @param cursor
//     */
//    private void exportToExcel(Cursor cursor) {
//        final String fileName = "WebcamList.xls";
//        //Saving file in external storage
//        File sdCard = Environment.getExternalStorageDirectory();
//        File directory = new File(sdCard.getAbsolutePath() + "/javatechig.webcam");
//        //create directory if not exist
//        if(!directory.isDirectory()){
//            directory.mkdirs();
//        }
//        //file path
//        File file = new File(directory, fileName);
//        WorkbookSettings wbSettings = new WorkbookSettings();
//        wbSettings.setLocale(new Locale("en", "EN"));
//        WritableWorkbook workbook;
//        try {
//            workbook = Workbook.createWorkbook(file, wbSettings);
//            //Excel sheet name. 0 represents first sheet
//            WritableSheet sheet = workbook.createSheet("MyShoppingList", 0);
//            try {
//                sheet.addCell(new Label(0, 0, "Subject")); // column and row
//                sheet.addCell(new Label(1, 0, "Description"));
//                if (cursor.moveToFirst()) {
//                    do {
//                        String title = cursor.getString(cursor.getColumnIndex(DatabaseHelper.WEBCAM_SUBJECT));
//                        String desc = cursor.getString(cursor.getColumnIndex(DatabaseHelper.WEBCAM_DESC));
//                        int i = cursor.getPosition() + 1;
//                        sheet.addCell(new Label(0, i, title));
//                        sheet.addCell(new Label(1, i, desc));
//                    } while (cursor.moveToNext());
//                }
//                //closing cursor
//                cursor.close();
//            } catch (RowsExceededException e) {
//                e.printStackTrace();
//            } catch (WriteException e) {
//                e.printStackTrace();
//            }
//            workbook.write();
//            try {
//                workbook.close();
//            } catch (WriteException e) {
//                e.printStackTrace();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
