package com.boha.monitor.pmanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.com.boha.monitor.library.ClaimAndInvoicePagerActivity;
import com.com.boha.monitor.library.ImagePagerActivity;
import com.com.boha.monitor.library.MonitorMapActivity;
import com.com.boha.monitor.library.PictureActivity;
import com.com.boha.monitor.library.SitePagerActivity;
import com.com.boha.monitor.library.adapters.DrawerAdapter;
import com.com.boha.monitor.library.dialogs.ProjectDialog;
import com.com.boha.monitor.library.dto.CompanyDTO;
import com.com.boha.monitor.library.dto.CompanyStaffDTO;
import com.com.boha.monitor.library.dto.ProjectDTO;
import com.com.boha.monitor.library.dto.ProjectSiteTaskStatusDTO;
import com.com.boha.monitor.library.dto.transfer.PhotoUploadDTO;
import com.com.boha.monitor.library.dto.transfer.RequestDTO;
import com.com.boha.monitor.library.dto.transfer.ResponseDTO;
import com.com.boha.monitor.library.fragments.PageFragment;
import com.com.boha.monitor.library.fragments.ProjectListFragment;
import com.com.boha.monitor.library.fragments.SiteTaskAndStatusAssignmentFragment;
import com.com.boha.monitor.library.fragments.StatusReportFragment;
import com.com.boha.monitor.library.services.RequestSyncService;
import com.com.boha.monitor.library.util.CacheUtil;
import com.com.boha.monitor.library.util.ErrorUtil;
import com.com.boha.monitor.library.util.SharedUtil;
import com.com.boha.monitor.library.util.Statics;
import com.com.boha.monitor.library.util.Util;
import com.com.boha.monitor.library.util.WebCheck;
import com.com.boha.monitor.library.util.WebCheckResult;
import com.com.boha.monitor.library.util.WebSocketUtil;

import java.util.ArrayList;
import java.util.List;


public class ProjectPagerActivity extends ActionBarActivity
        implements ProjectListFragment.ProjectListListener,
        StatusReportFragment.StatusReportListener {

    private DrawerLayout mDrawerLayout;
    private DrawerAdapter mDrawerAdapter;
    private LayoutInflater inflater;
    ProgressBar progressBar;
    static final String LOG = ProjectPagerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer);
        ctx = getApplicationContext();
        inflater = getLayoutInflater();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mPager = (ViewPager) findViewById(R.id.pager);
        PagerTitleStrip strip = (PagerTitleStrip) findViewById(R.id.pager_title_strip);
        strip.setVisibility(View.GONE);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        drawerListView = (ListView) findViewById(R.id.left_drawer);
        titles = getResources().getStringArray(R.array.action_items);
        setDrawerList();
        setTitle(SharedUtil.getCompany(ctx).getCompanyName());
        CompanyStaffDTO staff = SharedUtil.getCompanyStaff(ctx);
        getSupportActionBar().setSubtitle(staff.getFullName());
        //
        // PhotoUploadService.uploadPendingPhotos(ctx);
    }

    private void setDrawerList() {
        CacheUtil.getCachedData(getApplicationContext(), CacheUtil.CACHE_DATA, new CacheUtil.CacheUtilListener() {
            @Override
            public void onFileDataDeserialized(ResponseDTO r) {
                CompanyDTO company = new CompanyDTO();
                if (r != null) {
                    if (r.getCompany() != null) {
                        response = r;
                        company = r.getCompany();
                        buildPages();
                    }
                }

                WebCheckResult wcr = WebCheck.checkNetworkAvailability(ctx);
                if (wcr.isWifiConnected()) {
                    getCompanyData();
                }
                for (String s : titles) {
                    sTitles.add(s);
                }
                mDrawerAdapter = new DrawerAdapter(getApplicationContext(), R.layout.drawer_item, sTitles, company);
                View v = inflater.inflate(R.layout.hero_image, null);
                ImageView img = (ImageView)v.findViewById(R.id.HERO_image);
                img.setImageDrawable(Util.getRandomHeroImage(ctx));
                TextView txt = (TextView)v.findViewById(R.id.HERO_caption);
                txt.setText(ctx.getString(R.string.projects));
                drawerListView.addHeaderView(v);
                drawerListView.setAdapter(mDrawerAdapter);
                drawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        switch (i) {
                            case PROJECTS:
                                mPager.setCurrentItem(0, true);
                                mDrawerLayout.closeDrawers();
                                break;
                            case STAFF:
                                mPager.setCurrentItem(1, true);
                                mDrawerLayout.closeDrawers();
                                break;
                            case MANAGE_DATA:
                                break;
                            case SITE_REPORTS:
                                break;
                            case BENEFICIARIES:
                                break;
                            case PROJECT_MAPS:
                                break;
                            case INVOICES:
                                break;
                            case HAPPY_LETTERS:
                                break;
                            case STATUS_NOTIFICATIONS:
                                break;
                        }
                    }
                });


            }

            @Override
            public void onDataCached() {

            }

            @Override
            public void onError() {

            }
        });
    }

    private void getCompanyData() {
        Log.w(LOG, "############# getCompanyData................");
        RequestDTO w = new RequestDTO();
        w.setRequestType(RequestDTO.GET_COMPANY_DATA);
        w.setCompanyID(SharedUtil.getCompany(ctx).getCompanyID());

        if (projectListFragment != null)
            projectListFragment.rotateLogo();
        progressBar.setVisibility(View.VISIBLE);
        WebSocketUtil.sendRequest(ctx, Statics.COMPANY_ENDPOINT, w, new WebSocketUtil.WebSocketListener() {
            @Override
            public void onMessage(final ResponseDTO r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        if (!ErrorUtil.checkServerError(ctx,r)) {
                            return;
                        }

                        if (projectListFragment != null)
                            projectListFragment.stopRotatingLogo();
                        Log.e(LOG, "## getCompanyData responded...statusCode: " + r.getStatusCode());
                        response = r;
                        buildPages();
                        //cache company data
                        CacheUtil.cacheData(ctx, r, CacheUtil.CACHE_DATA, new CacheUtil.CacheUtilListener() {
                            @Override
                            public void onFileDataDeserialized(ResponseDTO response) {

                            }

                            @Override
                            public void onDataCached() {
                                Log.i(LOG, "** companyData cached, about to start requestSyncService....");
                               Intent i = new Intent(getApplicationContext(),RequestSyncService.class);
                               startService(i);
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
                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Util.showErrorToast(ctx, message);
                    }
                });
            }
        });
    }

    List<ProjectSiteTaskStatusDTO> projectSiteTaskStatusList;

    public static final int PROJECTS = 0,
            STAFF = 1, MANAGE_DATA = 2, SITE_REPORTS = 3, BENEFICIARIES = 4, PROJECT_MAPS = 5,
            INVOICES = 6, HAPPY_LETTERS = 7, STATUS_NOTIFICATIONS = 8;

    Menu mMenu;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manager_pager, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_help) {
            Util.showToast(ctx, ctx.getString(R.string.under_cons));
            return true;
        }
        if (id == R.id.action_refresh) {
            WebCheckResult r = WebCheck.checkNetworkAvailability(ctx);
            if (!r.isWifiConnected()) {
                Util.showToast(ctx,getString(R.string.wifi_not_available));
                return true;
            }
            getCompanyData();
            return true;
        }
        if (id == R.id.action_gallery) {
            Intent i = new Intent(this, ImagePagerActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void buildPages() {
        if (pageFragmentList == null) {
            pageFragmentList = new ArrayList<>();
            projectListFragment = new ProjectListFragment();
            Bundle data1 = new Bundle();
            data1.putSerializable("response", response);
            data1.putInt("type", ProjectListFragment.PROJECT_TYPE);
            projectListFragment.setArguments(data1);

            statusReportFragment = new StatusReportFragment();
            statusReportFragment.setArguments(data1);


            pageFragmentList.add(projectListFragment);
            pageFragmentList.add(statusReportFragment);

            adapter = new PagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(adapter);
            mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageSelected(int arg0) {
                    currentPageIndex = arg0;
                    if (pageFragmentList.get(currentPageIndex) instanceof StatusReportFragment) {
                        statusReportFragment.getProjectStatus();
                    }
                }

                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) {

                }

                @Override
                public void onPageScrollStateChanged(int arg0) {
                }
            });
        } else {
            //refresh pages
            projectListFragment.refreshData(response);
        }

    }

    ProjectListFragment projectListFragment;
    StatusReportFragment statusReportFragment;

    PagerAdapter adapter;
    ViewPager mPager;
    Context ctx;
    ResponseDTO response;
    int currentPageIndex;

    @Override
    public void onProjectClicked(ProjectDTO project) {

    }

    @Override
    public void onProjectEditDialogRequested(ProjectDTO project) {
        ProjectDialog pd = new ProjectDialog();
        pd.setAction(ProjectDTO.ACTION_UPDATE);
        pd.setContext(ctx);
        pd.setProject(project);
        pd.setListener(new ProjectDialog.ProjectDialogListener() {
            @Override
            public void onProjectAdded(final ProjectDTO project) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        projectListFragment.addProject(project);
                    }
                });
            }

            @Override
            public void onProjectUpdated(ProjectDTO project) {

            }

            @Override
            public void onError(String message) {

            }
        });
        pd.show(getFragmentManager(), "PDIAG");
    }

    @Override
    public void onProjectSitesRequested(ProjectDTO project) {

        Intent i = new Intent(this, SitePagerActivity.class);
        i.putExtra("project", project);
        i.putExtra("type", SiteTaskAndStatusAssignmentFragment.PROJECT_MANAGER);
        startActivityForResult(i, NEW_STATUS_EXPECTED);

    }
    static final int NEW_STATUS_EXPECTED = 2937;
    @Override
    public void onProjectPictureRequested(ProjectDTO project) {
        Intent i = new Intent(this, PictureActivity.class);
        i.putExtra("type", PhotoUploadDTO.PROJECT_IMAGE);
        i.putExtra("project", project);
        startActivity(i);
    }

    @Override
    public void onGalleryRequested(ProjectDTO project) {
        Intent i = new Intent(this, ImagePagerActivity.class);
        i.putExtra("project", project);
        i.putExtra("type", ImagePagerActivity.PROJECT);
        startActivity(i);
    }

    @Override
    public void onMapRequested(ProjectDTO project) {
        Intent i = new Intent(this, MonitorMapActivity.class);
        i.putExtra("project", project);
        startActivity(i);
    }

    @Override
    public void onClaimsAndInvoicesRequested(ProjectDTO project) {
        Intent i = new Intent(this, ClaimAndInvoicePagerActivity.class);
        i.putExtra("project", project);
        startActivity(i);
    }

    @Override
    public void onStatusReportRequested() {
        mPager.setCurrentItem(1, true);
    }

    static final int PICTURE_REQUESTED = 9133;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICTURE_REQUESTED) {
            if (resultCode == RESULT_OK) {

            }
        }
        if (requestCode == NEW_STATUS_EXPECTED) {
            if (resultCode == RESULT_OK) {
                int count = data.getIntExtra("newStatusDone",0);
                projectListFragment.updateStatusCount(count);
            }
        }
    }

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
                    title = ctx.getResources().getString(R.string.company_projects);
                    break;
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;

                default:
                    break;
            }
            return title;
        }
    }

    @Override
    public void onPause() {
        overridePendingTransition(com.boha.monitor.library.R.anim.slide_in_left, com.boha.monitor.library.R.anim.slide_out_right);
        super.onPause();
    }

    private List<PageFragment> pageFragmentList;
    private ListView drawerListView;
    private String[] titles;
    private List<String> sTitles = new ArrayList<>();

}
