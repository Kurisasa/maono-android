package com.com.boha.monitor.library.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.boha.monitor.library.R;
import com.com.boha.monitor.library.dialogs.StatusDialog;
import com.com.boha.monitor.library.dto.ProjectSiteDTO;
import com.com.boha.monitor.library.dto.ProjectSiteTaskDTO;
import com.com.boha.monitor.library.dto.ProjectSiteTaskStatusDTO;
import com.com.boha.monitor.library.dto.transfer.PhotoUploadDTO;
import com.com.boha.monitor.library.dto.transfer.ResponseDTO;
import com.com.boha.monitor.library.fragments.SiteTaskAndStatusAssignmentFragment;

import java.util.ArrayList;
import java.util.List;

import static com.com.boha.monitor.library.util.Util.showErrorToast;
import static com.com.boha.monitor.library.util.Util.showToast;

public class TaskAssignmentActivity extends ActionBarActivity implements
        SiteTaskAndStatusAssignmentFragment.ProjectSiteTaskListener {

    Context ctx;
    ProjectSiteDTO site;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_assignment);
        ctx = getApplicationContext();
        site = (ProjectSiteDTO) getIntent()
                .getSerializableExtra("projectSite");
        int type = getIntent().getIntExtra("type", SiteTaskAndStatusAssignmentFragment.OPERATIONS);

        taf = (SiteTaskAndStatusAssignmentFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment);
        taf.setProjectSite(site, type);
        setTitle(site.getProjectSiteName());
        getSupportActionBar().setSubtitle(site.getProjectName());
        taf.setProjectSiteTaskList(site.getProjectSiteTaskList());
    }

    SiteTaskAndStatusAssignmentFragment taf;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.task_assignment, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add) {
            taf.popupTaskList();
            return true;
        }
        if (id == R.id.action_help) {
            showToast(ctx, ctx.getString(R.string.under_cons));
            return true;
        }
        if (id == R.id.action_camera) {
            Intent i = new Intent(this, PictureActivity.class);
            i.putExtra("projectSite", site);
            i.putExtra("type", PhotoUploadDTO.SITE_IMAGE);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTaskClicked(ProjectSiteTaskDTO task) {

    }

    @Override
    public void onProjectSiteTaskAdded(ProjectSiteTaskDTO task) {
        Log.w(LOG, "## onProjectSiteTaskAdded " + task.getTask().getTaskName());
        projectSiteTaskList.add(task);
    }

    List<ProjectSiteTaskDTO> projectSiteTaskList = new ArrayList<>();

    @Override
    public void onProjectSiteTaskDeleted() {

    }

    @Override
    public void onSubTaskListRequested(ProjectSiteTaskDTO task, ProjectSiteTaskStatusDTO taskStatus) {
        if (task == null)
            throw new UnsupportedOperationException("##onSubTaskListRequested, ProjectSiteTaskDTO is null");
        Intent i = new Intent(ctx, SubTaskStatusAssignmentActivity.class);
        task.setProjectSiteName(site.getProjectSiteName());
        task.setProjectName(site.getProjectName());
        i.putExtra("projectSiteTask", task);
        i.putExtra("projectSiteTaskStatus", taskStatus);
        startActivityForResult(i, SUBTASK_ASSIGNMENT);
    }

    static final int SUBTASK_ASSIGNMENT = 11413;

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        switch (reqCode) {
            case SUBTASK_ASSIGNMENT:
                if (resCode == RESULT_OK) {
                    //TODO - what is in the intent, to update something??
                    taf.setProjectSiteTaskList(site.getProjectSiteTaskList());
                }
                break;
        }
    }

    @Override
    public void onStatusDialogRequested(ProjectSiteDTO projectSite, ProjectSiteTaskDTO siteTask) {
        StatusDialog d = new StatusDialog();
        d.setProjectSite(projectSite);
        d.setProjectSiteTask(siteTask);
        d.setContext(getApplicationContext());
        d.setListener(new StatusDialog.StatusDialogListener() {
            @Override
            public void onStatusAdded(ProjectSiteTaskStatusDTO taskStatus) {
                taf.updateList(taskStatus);
            }

            @Override
            public void onError(final String message) {
                Log.e("TaskAssignmentActivity", "---- ERROR websocket - " + message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorToast(ctx, message);
                    }
                });
            }
        });
        d.show(getFragmentManager(), "DIAG_STATUS");
    }

    @Override
    public void onProjectSiteTaskStatusAdded(ProjectSiteTaskStatusDTO taskStatus) {
        Log.w(LOG, "## onProjectSiteTaskStatusAdded " + taskStatus.getTask().getTaskName() + " "
                + taskStatus.getTaskStatus().getTaskStatusName());

        projectSiteTaskStatusList.add(taskStatus);
    }

    List<ProjectSiteTaskStatusDTO> projectSiteTaskStatusList = new ArrayList<>();

    @Override
    public void onCameraRequested(ProjectSiteTaskDTO siteTask, int type) {
        Intent i = new Intent(this, PictureActivity.class);
        i.putExtra("type", PhotoUploadDTO.TASK_IMAGE);
        i.putExtra("projectSiteTask", siteTask);
        startActivityForResult(i, TASK_PICTURE_REQUIRED);
    }

    static final int TASK_PICTURE_REQUIRED = 9582;
    private Menu mMenu;

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (mMenu != null) {
            final MenuItem refreshItem = mMenu.findItem(R.id.action_help);
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
    public void onBackPressed() {
        ResponseDTO r = new ResponseDTO();
        r.setProjectSiteTaskStatusList(projectSiteTaskStatusList);
        r.setProjectSiteTaskList(projectSiteTaskList);
        Intent i = new Intent();
        i.putExtra("response", r);

        setResult(RESULT_OK, i);
        finish();
    }

    @Override
    public void onPause() {
        overridePendingTransition(com.boha.monitor.library.R.anim.slide_in_left, com.boha.monitor.library.R.anim.slide_out_right);
        super.onPause();
    }

    static final String LOG = TaskAssignmentActivity.class.getSimpleName();
}
