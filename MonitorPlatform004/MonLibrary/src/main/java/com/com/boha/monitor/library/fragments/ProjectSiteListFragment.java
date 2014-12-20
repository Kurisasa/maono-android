package com.com.boha.monitor.library.fragments;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import com.boha.monitor.library.R;
import com.com.boha.monitor.library.adapters.ProjectSiteAdapter;
import com.com.boha.monitor.library.dto.ProjectDTO;
import com.com.boha.monitor.library.dto.ProjectSiteDTO;
import com.com.boha.monitor.library.dto.ProjectSiteTaskDTO;
import com.com.boha.monitor.library.dto.ProjectSiteTaskStatusDTO;
import com.com.boha.monitor.library.dto.transfer.RequestDTO;
import com.com.boha.monitor.library.dto.transfer.ResponseDTO;
import com.com.boha.monitor.library.util.CacheUtil;
import com.com.boha.monitor.library.util.ErrorUtil;
import com.com.boha.monitor.library.util.Statics;
import com.com.boha.monitor.library.util.Util;
import com.com.boha.monitor.library.util.WebSocketUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fragment representing a taskStatusList of Items.
 * <project/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <project/>
 * Activities containing this fragment MUST implement the ProjectSiteListListener
 * interface.
 */
public class ProjectSiteListFragment extends Fragment implements PageFragment {

    private ProjectSiteListListener mListener;
    private AbsListView mListView;

    public ProjectSiteListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    Context ctx;
    TextView txtCount, txtName;
    int lastIndex;
    View view, topView;
    ImageView imgLogo;
    ObjectAnimator objectAnimator;
    ImageView imgSearch1, imgSearch2, heroImage;
    EditText editSearch;

    static final String LOG = ProjectSiteListFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_projectsite, container, false);
        Log.i(LOG, "------------- onCreateView");
        ctx = getActivity();
        Bundle b = getArguments();
        if (b != null) {

            project = (ProjectDTO) b.getSerializable("project");
            lastIndex = b.getInt("index", 0);
            Log.e(LOG, "++++ onCreateView getting project object from getArguments: status count: " + project.getStatusCount());
        }
        if (savedInstanceState != null) {
            lastIndex = savedInstanceState.getInt("lastIndex", 0);
            Log.e(LOG, "++++ lastIndex in savedInstanceState: " + lastIndex);
        }
        txtCount = (TextView) view.findViewById(R.id.SITE_LIST_siteCount);
        imgLogo = (ImageView) view.findViewById(R.id.SITE_LIST_imgLogo);
        topView = view.findViewById(R.id.SITE_LIST_TOP);

        imgSearch1 = (ImageView) view.findViewById(R.id.SLT_imgSearch1);
        imgSearch2 = (ImageView) view.findViewById(R.id.SLT_imgSearch2);
        editSearch = (EditText) view.findViewById(R.id.SLT_editSearch);
        heroImage = (ImageView) view.findViewById(R.id.SLT_heroImage);
        heroImage.setImageDrawable(Util.getRandomHeroImage(ctx));

        View v = view.findViewById(R.id.SLT_searchLayout);

        Util.expand(v, 1000, null);

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                search();
            }
        });

        imgSearch1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });
        imgSearch2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        imgLogo.setVisibility(View.GONE);
        mListView = (AbsListView) view.findViewById(R.id.SLT_list);
        Statics.setRobotoFontLight(ctx, txtCount);
        if (project.getProjectSiteList() != null && !project.getProjectSiteList().isEmpty()) {
            projectSiteList = project.getProjectSiteList();
            setList();
        }
        return view;
    }

    private void search() {
        if (editSearch.getText().toString().isEmpty()) {
            return;
        }
        int index = 0;
        boolean found = false;
        for (ProjectSiteDTO site : project.getProjectSiteList()) {
            if (site.getProjectSiteName().contains(editSearch.getText().toString())) {
                found = true;
                break;
            }
            index++;
        }
        if (found) {
            mListView.setSelection(index);
        } else {
            Util.showToast(ctx, ctx.getString(R.string.site_not_found) + " " + editSearch.getText().toString());
        }

    }

    public void rotateLogo() {
        imgLogo.setVisibility(View.VISIBLE);
        objectAnimator = ObjectAnimator.ofFloat(imgLogo, "rotation", 0.0f, 360f);
        objectAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator.setDuration(200);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {

            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        objectAnimator.start();
    }

    public void stopRotatingLogo() {
        imgLogo.setVisibility(View.GONE);
        objectAnimator.cancel();
    }

    public void setProject(ProjectDTO project) {
        this.project = project;
        Collections.sort(project.getProjectSiteList());
        projectSiteAdapter.notifyDataSetChanged();

    }

    List<ProjectSiteDTO> projectSiteList;

    public void updateSiteLocation(ProjectSiteDTO site) {
        Log.e(LOG, "updateSiteLocation site location confirmed: " + site.getLocationConfirmed());
        List<ProjectSiteDTO> list = new ArrayList<>();
        for (ProjectSiteDTO s: projectSiteList) {
            if (s.getProjectSiteID().intValue() == site.getProjectSiteID().intValue()) {
                list.add(site);
                Log.i(LOG,"## confirmed site put in list");
            } else {
                list.add(s);
            }
        }
        projectSiteList = list;
        setList();
    }
    private void setList() {
        Log.i(LOG, "## setList");
        txtCount.setText("" + projectSiteList.size());
        Collections.sort(projectSiteList);

        projectSiteAdapter = new ProjectSiteAdapter(ctx, R.layout.site_item,
                projectSiteList, new ProjectSiteAdapter.ProjectSiteListener() {
            @Override
            public void onProjectSiteClicked(ProjectSiteDTO site, int index) {
                projectSite = site;
                lastIndex = index;
                showPopup();
            }
        });
        mListView.setAdapter(projectSiteAdapter);
        mListView.setSelection(lastIndex);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG,"######## mListView onItemClick: " + position);
                if (null != mListener) {
                    lastIndex = position;
                    projectSite = projectSiteList.get(position);
                    showPopup();
                }
            }
        });
        mListener.onPhotoUploadServiceRequested();
    }

    private void showPopup() {
        Log.d(LOG,"### showPopup");
        list = new ArrayList<>();
        list.add(ctx.getString(R.string.sitestatus));
        if (projectSite.getLocationConfirmed() != null) {
            list.add(ctx.getString(R.string.take_picture));
            list.add(ctx.getString(R.string.site_gallery));
            list.add(ctx.getString(R.string.site_on_map));
        } else {
            list.add(ctx.getString(R.string.get_gps));
        }
        list.add(ctx.getString(R.string.edit_site));

        Util.showPopupBasicWithHeroImage(ctx,getActivity(),list,heroImage,ctx.getString(R.string.select_action), new Util.UtilPopupListener() {
            @Override
            public void onItemSelected(int index) {
                if (list.get(index).equalsIgnoreCase(ctx.getString(R.string.sitestatus))) {
                    mListener.onProjectSiteTasksRequested(projectSite, lastIndex);
                }
                if (list.get(index).equalsIgnoreCase(ctx.getString(R.string.take_picture))) {
                    mListener.onCameraRequested(projectSite, lastIndex);
                }
                if (list.get(index).equalsIgnoreCase(ctx.getString(R.string.site_gallery))) {
                    mListener.onGalleryRequested(projectSite, lastIndex);
                }
                if (list.get(index).equalsIgnoreCase(ctx.getString(R.string.site_on_map))) {
                    mListener.onSiteOnMapRequested(projectSite, lastIndex);
                }
                if (list.get(index).equalsIgnoreCase(ctx.getString(R.string.get_gps))) {
                    mListener.onGPSRequested(projectSite, lastIndex);
                }
                if (list.get(index).equalsIgnoreCase(ctx.getString(R.string.edit_site))) {
                    mListener.onProjectSiteEditRequested(projectSite, lastIndex);
                }

            }
        });


    }

    int index;
    List<String> list;
    ListPopupWindow actionsWindow;
    int newStatusJustDone;

    public void refreshData(ResponseDTO resp) {
        Log.w(LOG, "### refreshData... status: " + resp.getProjectSiteTaskStatusList().size());
        List<ProjectSiteTaskStatusDTO> pList = resp.getProjectSiteTaskStatusList();
        newStatusJustDone = pList.size();
        List<ProjectSiteDTO> list = new ArrayList<>();
        for (ProjectSiteDTO s : projectSiteList) {
            for (ProjectSiteTaskDTO task : s.getProjectSiteTaskList()) {
                for (ProjectSiteTaskStatusDTO sta : pList) {
                    if (task.getProjectSiteTaskID().intValue() == sta.getProjectSiteTaskID().intValue()) {
                        if (s.getStatusCount() == null) {
                            s.setStatusCount(1);
                        } else {
                            s.setStatusCount(s.getStatusCount() + 1);
                        }
                        s.setLastStatus(sta);
                        Log.i(LOG, "## LastStatus updated in list, task: "
                                + sta.getTask().getTaskName() + " status: "
                                + sta.getTaskStatus().getTaskStatusName());
                    }
                }
            }

            list.add(s);
        }
        projectSiteList = list;
        project.setProjectSiteList(list);
        setList();
        //cache data
        ResponseDTO r = new ResponseDTO();
        r.setProjectList(new ArrayList<ProjectDTO>());
        r.getProjectList().add(project);
        CacheUtil.cacheProjectData(ctx,r,project.getProjectID(), null);
        mListener.onNewStatusDone(newStatusJustDone);
    }

    public void refreshPhotoList(ProjectSiteDTO site) {

        Log.i(LOG, "###### refreshPhotoList");
        if (projectSite == null) throw new UnsupportedOperationException("ProjectSiteDTO is null");
        RequestDTO w = new RequestDTO();
        w.setRequestType(RequestDTO.GET_SITE_IMAGES);
        w.setProjectSiteID(site.getProjectSiteID());

        WebSocketUtil.sendRequest(ctx, Statics.COMPANY_ENDPOINT, w, new WebSocketUtil.WebSocketListener() {
            @Override
            public void onMessage(final ResponseDTO response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!ErrorUtil.checkServerError(ctx, response)) {
                            return;
                        }
                        projectSite.setPhotoUploadList(response.getPhotoUploadList());

                        setList();
                        index = 0;
                        for (ProjectSiteDTO ps : project.getProjectSiteList()) {
                            if (ps.getProjectSiteID() == projectSite.getProjectSiteID()) {
                                break;
                            }
                            index++;
                        }
                        mListView.setSelection(index);
                        mListener.onPhotoListUpdated(projectSite, index);
                    }
                });
            }

            @Override
            public void onClose() {

            }

            @Override
            public void onError(final String message) {
                Log.e(LOG, "---- ERROR websocket - " + message);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Util.showErrorToast(ctx, message);
                    }
                });
            }
        });


    }

    public void setListPosition(int index) {
        lastIndex = index;
    }

    public void addProjectSite(ProjectSiteDTO site) {
        if (project.getProjectSiteList() == null) {
            project.setProjectSiteList(new ArrayList<ProjectSiteDTO>());
        }
        project.getProjectSiteList().add(0, site);
        projectSiteAdapter.notifyDataSetChanged();
        txtCount.setText("" + project.getProjectSiteList().size());
        try {
            Thread.sleep(1000);
            Util.animateRotationY(txtCount, 500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ProjectSiteListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ProjectSiteListListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }

    public void setLocationConfirmed(ProjectSiteDTO ps) {
        Log.e(LOG, "## confirmed location of " + ps.getProjectSiteName() + ", rebuild list");
        List<ProjectSiteDTO> list = new ArrayList<>();
        for (ProjectSiteDTO s: projectSiteList) {
            if (ps.getProjectSiteID().intValue() == s.getProjectSiteID().intValue()) {
                list.add(ps);
            } else {
                list.add(s);
            }
        }
        projectSiteList = list;
        projectSiteAdapter.notifyDataSetChanged();
        //the confirmed location has to be cached...

        CacheUtil.getCachedProjectData(ctx, ps.getProjectID(), new CacheUtil.CacheUtilListener() {
            @Override
            public void onFileDataDeserialized(ResponseDTO response) {
                if (response != null) {
                    project.setProjectSiteList(projectSiteList);
                    response.setProjectList(new ArrayList<ProjectDTO>());
                    response.getProjectList().add(project);
                    CacheUtil.cacheProjectData(ctx, response, project.getProjectID(), null);
                }
            }

            @Override
            public void onDataCached() {

            }

            @Override
            public void onError() {

            }
        });
    }


    ProjectSiteDTO projectSite;

    public ProjectSiteDTO getProjectSite() {
        return projectSite;
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the taskStatusList is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void animateCounts() {
        Util.animateRotationY(txtCount, 500);

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow logoAnimator interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <project/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface ProjectSiteListListener {
        public void onProjectSiteClicked(ProjectSiteDTO projectSite, int index);

        public void onProjectSiteEditRequested(ProjectSiteDTO projectSite, int index);

        public void onProjectSiteTasksRequested(ProjectSiteDTO projectSite, int index);

        public void onCameraRequested(ProjectSiteDTO projectSite, int index);

        public void onGalleryRequested(ProjectSiteDTO projectSite, int index);

        public void onPhotoListUpdated(ProjectSiteDTO projectSite, int index);

        public void onStatusListRequested(ProjectSiteDTO projectSite, int index);

        public void onGPSRequested(ProjectSiteDTO projectSite, int index);

        public void onSiteOnMapRequested(ProjectSiteDTO projectSite, int index);

        public void onNewStatusDone(int count);
        public void onPhotoUploadServiceRequested();
    }

    ProjectDTO project;
    ProjectSiteAdapter projectSiteAdapter;
}
