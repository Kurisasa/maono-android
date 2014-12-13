package com.com.boha.monitor.library;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.boha.monitor.library.R;
import com.com.boha.monitor.library.dialogs.ProjectSiteDialog;
import com.com.boha.monitor.library.dto.ProjectDTO;
import com.com.boha.monitor.library.dto.ProjectSiteDTO;
import com.com.boha.monitor.library.dto.transfer.PhotoUploadDTO;
import com.com.boha.monitor.library.dto.transfer.RequestDTO;
import com.com.boha.monitor.library.dto.transfer.ResponseDTO;
import com.com.boha.monitor.library.fragments.GPSScanFragment;
import com.com.boha.monitor.library.fragments.PageFragment;
import com.com.boha.monitor.library.fragments.ProjectSiteListFragment;
import com.com.boha.monitor.library.fragments.SiteTaskAndStatusAssignmentFragment;
import com.com.boha.monitor.library.util.CacheUtil;
import com.com.boha.monitor.library.util.ErrorUtil;
import com.com.boha.monitor.library.util.Statics;
import com.com.boha.monitor.library.util.Util;
import com.com.boha.monitor.library.util.WebCheck;
import com.com.boha.monitor.library.util.WebCheckResult;
import com.com.boha.monitor.library.util.WebSocketUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.List;

import static com.com.boha.monitor.library.util.Util.showErrorToast;
import static com.com.boha.monitor.library.util.Util.showToast;

public class SitePagerActivity extends ActionBarActivity implements com.google.android.gms.location.LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        ProjectSiteListFragment.ProjectSiteListListener, GPSScanFragment.GPSScanFragmentListener {

    static final int NUM_ITEMS = 2;
    GPSScanFragment gpsScanFragment;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_pager);
        ctx = getApplicationContext();
        mPager = (ViewPager) findViewById(R.id.SITE_pager);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        mPager.setOffscreenPageLimit(NUM_ITEMS - 1);
        PagerTitleStrip strip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
        strip.setVisibility(View.GONE);

        project = (ProjectDTO) getIntent().getSerializableExtra("project");
        type = getIntent().getIntExtra("type", SiteTaskAndStatusAssignmentFragment.OPERATIONS);

        setTitle(ctx.getString(R.string.project_sites));
        getSupportActionBar().setSubtitle(project.getProjectName());
        mLocationClient = new LocationClient(ctx, this, this);
    }

    private void getCachedProjectData() {
        //check network
        final WebCheckResult r = WebCheck.checkNetworkAvailability(ctx);
        CacheUtil.getCachedProjectData(ctx, project.getProjectID(), new CacheUtil.CacheUtilListener() {
            @Override
            public void onFileDataDeserialized(ResponseDTO response) {
                if (response != null) {
                    if (response.getProjectList() != null && !response.getProjectList().isEmpty()) {
                        project = response.getProjectList().get(0);
                        buildPages();
                        return;
                    } else {
                        if (r.isWifiConnected()) {
                            getProjectData();
                        } else {
                            Util.showErrorToast(ctx,getString(R.string.connect_wifi));
                            return;
                        }

                    }
                }


                if (r.isWifiConnected()) {
                    getProjectData();
                }

            }

            @Override
            public void onDataCached() {

            }

            @Override
            public void onError() {
                getProjectData();
            }
        });
    }

    private void getProjectData() {
        Log.w(LOG, "################################ getProjectData");
        RequestDTO w = new RequestDTO(RequestDTO.GET_PROJECT_DATA);
        w.setProjectID(project.getProjectID());

        progressBar.setVisibility(View.VISIBLE);
        WebSocketUtil.sendRequest(ctx, Statics.COMPANY_ENDPOINT, w, new WebSocketUtil.WebSocketListener() {
            @Override
            public void onMessage(final ResponseDTO response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //setRefreshActionButtonState(false);
                        progressBar.setVisibility(View.GONE);
                        if (!ErrorUtil.checkServerError(ctx, response)) {
                            return;
                        }
                        project = response.getProjectList().get(0);

                        Log.i(LOG, "----- returned project data, photos: " + project.getPhotoUploadList().size());
                        buildPages();
                        CacheUtil.cacheProjectData(ctx, response,  project.getProjectID(), new CacheUtil.CacheUtilListener() {
                            @Override
                            public void onFileDataDeserialized(ResponseDTO response) {

                            }

                            @Override
                            public void onDataCached() {

                            }

                            @Override
                            public void onError() {

                            }
                        });
                    }
                });
            }

            @Override
            public void onClose() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Log.e(LOG, "getProjectData --------------- websocket closed");
                    }
                });
            }

            @Override
            public void onError(final String message) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        showErrorToast(ctx, message);
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.site_pager, menu);
        mMenu = menu;
        getCachedProjectData();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_help) {
            showToast(ctx, ctx.getString(R.string.under_cons));
            return true;
        }
        if (id == R.id.action_add) {
            ProjectSiteDialog d = new ProjectSiteDialog();
            d.setContext(ctx);
            d.setProject(project);
            d.setProjectSite(new ProjectSiteDTO());
            d.setAction(ProjectSiteDTO.ACTION_ADD);
            d.setListener(new ProjectSiteDialog.ProjectSiteDialogListener() {
                @Override
                public void onProjectSiteAdded(final ProjectSiteDTO site) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            projectSiteListFragment.addProjectSite(site);
                        }
                    });
                }

                @Override
                public void onProjectSiteUpdated(ProjectSiteDTO project) {

                }

                @Override
                public void onError(final String message) {
                    Log.e(LOG, "---- ERROR websocket - " + message);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showErrorToast(ctx, message);
                        }
                    });
                }
            });
            d.show(getFragmentManager(), "PSD_DIAG");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    boolean isRefresh;

    private void getGPSCoordinates() {
        if (!mLocationClient.isConnected()) {
            mLocationClient.connect();
        }
        LocationRequest lr = new LocationRequest();
        lr.setFastestInterval(1000);
        lr.setInterval(2000);
        lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            mLocationClient.requestLocationUpdates(lr, this);
        } catch (IllegalStateException e) {
            Log.e(LOG, "---- mLocationClient.requestLocationUpdates ILLEGAL STATE", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(LOG,
                "#################### onStart");
        if (mLocationClient != null) {
            mLocationClient.connect();
            Log.i(LOG,
                    "#################### onStart - locationClient connecting ... ");
        }

    }

    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
        Log.e(LOG,
                "#################### stopPeriodicUpdates - removeLocationUpdates");
    }

    @Override
    public void onStop() {
        Log.d(LOG,
                "#################### onStop");
        if (mLocationClient != null) {
            if (mLocationClient.isConnected()) {
                stopPeriodicUpdates();
            }
            // After disconnect() is called, the client is considered "dead".
            mLocationClient.disconnect();
            Log.e("map", "### onStop - locationClient disconnected: "
                    + mLocationClient.isConnected());
        }
        super.onStop();
    }

    private Location location;

    @Override
    public void onLocationChanged(Location loc) {

        Log.w(LOG, "### Location changed, lat: "
                + loc.getLatitude() + " lng: "
                + loc.getLongitude()
                + " -- accuracy: " + loc.getAccuracy());

        latitude = loc.getLatitude();
        longitude = loc.getLongitude();
        mCurrentLocation = loc;
        if (gpsScanFragment != null) {
            gpsScanFragment.setLocation(loc);
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG,
                "### ---> PlayServices onConnected() - gotta start something! >>");
        mCurrentLocation = mLocationClient.getLastLocation();
        if (mCurrentLocation != null) {
            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();
            onLocationChanged(mCurrentLocation);
        } else {
            Log.e(LOG, "$$$$ mCurrentLocation is NULL");
        }
    }

    @Override
    public void onDisconnected() {
        Log.e(LOG,
                "### ---> PlayServices onDisconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG,
                "### ---> PlayServices onConnectionFailed: " + connectionResult.toString());
    }

    boolean photosLoaded;

    @Override
    public void onResume() {
        Log.e(LOG, "######### onResume .........getProjectPhotos");
        //getProjectPhotos();

        super.onResume();
    }

    private void buildPages() {

        pageFragmentList = new ArrayList<>();
        projectSiteListFragment = new ProjectSiteListFragment();
        Bundle data1 = new Bundle();
        data1.putSerializable("project", project);
        data1.putInt("index", selectedSiteIndex);
        projectSiteListFragment.setArguments(data1);

        gpsScanFragment = new GPSScanFragment();

        pageFragmentList.add(projectSiteListFragment);
        pageFragmentList.add(gpsScanFragment);

        initializeAdapter();


    }

    private void initializeAdapter() {
        try {
            adapter = new PagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(adapter);
            mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageSelected(int arg0) {
                    currentPageIndex = arg0;
                    if (currentPageIndex == 1) {
                        if (gpsScanFragment.getProjectSite() == null) {
                            mPager.setCurrentItem(0);
                        } else {
                            gpsScanFragment.startScan();
                        }
                    }
                    if (currentPageIndex == 0) {
                        gpsScanFragment.setProjectSite(null);
                    }
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {

                }

                @Override
                public void onPageScrollStateChanged(int arg0) {
                }
            });
        } catch (Exception e) {
            Log.e(LOG, "-- Some shit happened, probably IllegalState of some kind ...");
        }
    }

    @Override
    public void onProjectSiteClicked(ProjectSiteDTO projectSite, int index) {
        selectedSiteIndex = index;
    }

    @Override
    public void onProjectSiteEditRequested(ProjectSiteDTO projectSite, int index) {
        selectedSiteIndex = index;
        ProjectSiteDialog d = new ProjectSiteDialog();
        d.setContext(ctx);
        d.setProject(project);
        d.setProjectSite(projectSite);
        d.setAction(ProjectSiteDTO.ACTION_UPDATE);
        d.setListener(new ProjectSiteDialog.ProjectSiteDialogListener() {
            @Override
            public void onProjectSiteAdded(final ProjectSiteDTO site) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        projectSiteListFragment.addProjectSite(site);
                    }
                });
            }

            @Override
            public void onProjectSiteUpdated(ProjectSiteDTO project) {

            }

            @Override
            public void onError(final String message) {
                Log.e(LOG, "---- ERROR websocket - " + message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorToast(ctx, message);
                    }
                });
            }
        });
        d.show(getFragmentManager(), "PSD_DIAG");
    }

    @Override
    public void onProjectSiteTasksRequested(ProjectSiteDTO projectSite, int index) {
        selectedSiteIndex = index;
        this.projectSite = projectSite;
        Intent i = new Intent(this, TaskAssignmentActivity.class);
        i.putExtra("projectSite", projectSite);
        i.putExtra("type", type);
        startActivityForResult(i, SITE_TASK_REQUEST);
    }

    @Override
    public void onCameraRequested(ProjectSiteDTO projectSite, int index) {

        selectedSiteIndex = index;
        this.projectSite = projectSite;
        Intent i = new Intent(this, PictureActivity.class);
        i.putExtra("projectSite", projectSite);
        i.putExtra("type", PhotoUploadDTO.SITE_IMAGE);
        startActivityForResult(i, SITE_PICTURE_REQUEST);


    }

    @Override
    public void onGalleryRequested(ProjectSiteDTO projectSite, int index) {
        selectedSiteIndex = index;
        this.projectSite = projectSite;
        Intent i = new Intent(this, ImagePagerActivity.class);
        i.putExtra("projectSite", projectSite);
        i.putExtra("type", ImagePagerActivity.SITE);
        startActivity(i);
    }

    @Override
    public void onPhotoListUpdated(final ProjectSiteDTO projectSite, int index) {
        Log.w(LOG, "------ onPhotoListUpdated site photos: " + projectSite.getPhotoUploadList().size());
        photosLoaded = true;
        selectedSiteIndex = index;
        this.projectSite = projectSite;

    }

    @Override
    public void onStatusListRequested(ProjectSiteDTO projectSite, int index) {
        Intent i = new Intent(this, StatusReportActivity.class);
        i.putExtra("projectSite", projectSite);
        startActivity(i);
    }

    @Override
    public void onGPSRequested(ProjectSiteDTO projectSite, int index) {
        getGPSCoordinates();
        gpsScanFragment.setProjectSite(projectSite);
        mPager.setCurrentItem(1, true);
    }

    @Override
    public void onSiteOnMapRequested(ProjectSiteDTO projectSite, int index) {
        Intent i = new Intent(this,MonitorMapActivity.class);
        i.putExtra("projectSite", projectSite);
        startActivity(i);
    }

    int newStatusDone;
    @Override
    public void onNewStatusDone(int count) {
        newStatusDone += count;
    }

    @Override
    public void onActivityResult(int reqCode, int res, Intent data) {
        Log.e(LOG, "##### onActivityResult requestCode: " + reqCode + " resultCode: " + res);
        switch (reqCode) {
            case MAP_REQUESTED:
                Log.w(LOG, "### map has returned with data?");
                if (res == RESULT_OK) {
                    projectSite = (ProjectSiteDTO) data.getSerializableExtra("projectSite");
                    projectSiteListFragment.updateSiteLocation(projectSite);
                }
                break;
            case SITE_PICTURE_REQUEST:
                if (res == RESULT_OK) {
                    projectSiteListFragment.refreshPhotoList(projectSite);
                }
                break;
            case SITE_TASK_REQUEST:

                if (res == RESULT_OK) {
                    ResponseDTO r = (ResponseDTO) data.getSerializableExtra("response");
                    Log.w(LOG, "## data returned, statusList: " + r.getProjectSiteTaskStatusList().size() +
                            " taskList: " + r.getProjectSiteTaskList().size());

                    projectSiteListFragment.refreshData(r);

                }
                break;
        }

    }

    @Override
    public void onStartScanRequested() {
        getGPSCoordinates();
    }

    @Override
    public void onEndScanRequested() {
        stopPeriodicUpdates();
    }

    @Override
    public void onMapRequested(ProjectSiteDTO projectSite) {
        if (projectSite.getLatitude() != null) {
            Intent i = new Intent(ctx, MonitorMapActivity.class);
            i.putExtra("projectSite", projectSite);
            startActivityForResult(i, MAP_REQUESTED);
        }
    }

    static final int MAP_REQUESTED = 9007;

    private class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {

            return (Fragment) pageFragmentList.get(i);
        }

        @Override
        public int getCount() {
            return pageFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            String title = "Title";

            switch (position) {
                case 0:
                    title = project.getProjectName();
                    break;


                default:
                    break;
            }
            return title;
        }
    }

    @Override
    public void onPause() {
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (pageFragmentList == null) return;
        if (pageFragmentList.get(currentPageIndex) instanceof GPSScanFragment) {
            mPager.setCurrentItem(0, true);
            return;
        }
        Intent i = new Intent();
        i.putExtra("newStatusDone",newStatusDone);
        setResult(RESULT_OK,i);

        finish();
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (mMenu != null) {
            final MenuItem refreshItem = mMenu.findItem(R.id.action_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.action_bar_progess);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    int selectedSiteIndex;
    ProjectSiteDTO projectSite;
    ProjectSiteListFragment projectSiteListFragment;
    List<PageFragment> pageFragmentList;
    double latitude, longitude;
    Location mCurrentLocation;
    Context ctx;
    ViewPager mPager;
    int type;
    Menu mMenu;
    int currentPageIndex;
    PagerAdapter adapter;
    ProjectDTO project;
    LocationClient mLocationClient;
    static final String LOG = SitePagerActivity.class.getSimpleName();
    static final int SITE_PICTURE_REQUEST = 113,
            SITE_TASK_REQUEST = 114;

}
