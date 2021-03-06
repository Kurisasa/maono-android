package com.com.boha.monitor.library.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.com.boha.monitor.library.dto.transfer.PhotoUploadDTO;
import com.com.boha.monitor.library.dto.transfer.ResponseDTO;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by aubreyM on 2014/12/30.
 */
public class PhotoCacheUtil {

    public interface PhotoCacheListener {
        public void onFileDataDeserialized(ResponseDTO response);
        public void onDataCached();
        public void onError();
    }
    public interface PhotoCacheRetrieveListener {
        public void onFileDataDeserialized(ResponseDTO response);
        public void onDataCached();
        public void onError();
    }

    static PhotoCacheListener photoCacheListener;
    static PhotoCacheRetrieveListener photoCacheRetrieveListener = new PhotoCacheRetrieveListener() {
        @Override
        public void onFileDataDeserialized(ResponseDTO response) {

        }

        @Override
        public void onDataCached() {

        }

        @Override
        public void onError() {

        }
    };

    static ResponseDTO response = new ResponseDTO();
    static PhotoUploadDTO photoUpload;
    static Context ctx;
    static final String JSON_PHOTO = "photos.json";


    public static void cachePhoto(Context context, final PhotoUploadDTO photo, PhotoCacheListener listener) {
        photoUpload = photo;
        response.setLastCacheDate(new Date());
        photoCacheListener = listener;
        ctx = context;
        new CacheRetrieveForUpdateTask().execute();
    }

    public static void getCachedPhotos(Context context, PhotoCacheListener listener) {
        photoCacheListener = listener;
        ctx = context;
        new CacheRetrieveTask().execute();
    }
    public static void clearCache(Context context, final List<PhotoUploadDTO> uploadedList) {
        ctx = context;
        getCachedPhotos(context,new PhotoCacheListener() {
            @Override
            public void onFileDataDeserialized(ResponseDTO r) {
                List<PhotoUploadDTO> pending = new ArrayList<>();
                for (PhotoUploadDTO p: r.getPhotoUploadList()) {
                    for (PhotoUploadDTO ps: uploadedList) {
                        if (ps.getThumbFilePath().equalsIgnoreCase(p.getThumbFilePath())) {
                            p.setDateUploaded(new Date());
                            File f = new File(ps.getThumbFilePath());
                            if (f.exists()) {
                                boolean del = f.delete();
                                Log.w(LOG, "### deleted image file: " + ps.getThumbFilePath() + " - " + del);
                            }
                        }
                    }
                }
                for (PhotoUploadDTO p: r.getPhotoUploadList()) {
                    if (p.getDateUploaded() == null) {
                        pending.add(p);
                    }
                }
                r.setPhotoUploadList(pending);
                response = r;
                Log.i(LOG,"## after clearing cache, pending photos: " + pending.size() + " - writing new cache");
                new CacheTask().execute();

            }

            @Override
            public void onDataCached() {

            }

            @Override
            public void onError() {

            }
        });
    }


    static class CacheTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            String json;
            File file;
            FileOutputStream outputStream;
            try {
                json = gson.toJson(response);
                outputStream = ctx.openFileOutput(JSON_PHOTO, Context.MODE_PRIVATE);
                write(outputStream, json);
                file = ctx.getFileStreamPath(JSON_PHOTO);
                if (file != null) {
                    Log.e(LOG, "Photo cache written, path: " + file.getAbsolutePath() +
                            " - length: " + file.length() + " photos: " + response.getPhotoUploadList().size());
                }
                StringBuilder sb = new StringBuilder();
                sb.append("\n### Photos in cache ###\n");
                for (PhotoUploadDTO p: response.getPhotoUploadList()) {
                    sb.append("+++ ").append(p.getThumbFilePath())
                            .append(" projectSiteID: " + p.getProjectSiteID())
                            .append("\n");
                }
                sb.append("#######################");
                Log.w(LOG,sb.toString());

            } catch (IOException e) {
                Log.e(LOG, "Failed to cache data", e);
                return 9;
            }
            return 0;
        }

        private void write(FileOutputStream outputStream, String json) throws IOException {
            outputStream.write(json.getBytes());
            outputStream.close();
        }

        @Override
        protected void onPostExecute(Integer v) {
            if (photoCacheListener != null) {
                if (v > 0) {
                    photoCacheListener.onError();
                } else
                    photoCacheListener.onDataCached();
            }

        }
    }

    static class CacheRetrieveTask extends AsyncTask<Void, Void, ResponseDTO> {

        private ResponseDTO getData(FileInputStream stream) throws IOException {
            String json = getStringFromInputStream(stream);
            ResponseDTO response = gson.fromJson(json, ResponseDTO.class);
            return response;
        }

        @Override
        protected ResponseDTO doInBackground(Void... voids) {
            ResponseDTO response = new ResponseDTO();
            response.setPhotoUploadList(new ArrayList<PhotoUploadDTO>());
            FileInputStream stream;
            try {
                stream = ctx.openFileInput(JSON_PHOTO);
                response = getData(stream);
                Log.i(LOG, "++ photo cache retrieved, photos: " + response.getPhotoUploadList().size());
            } catch (FileNotFoundException e) {
                Log.w(LOG, "############# cache file not found. not initialised yet. no problem, type = PHOTOS");
                return response;

            } catch (IOException e) {
                Log.e(LOG, "#### doInBackground - returning a new response object, type = PHOTOS");
            }

            return response;
        }

        @Override
        protected void onPostExecute(ResponseDTO v) {
            if (photoCacheListener == null)
                return;
            else
                photoCacheListener.onFileDataDeserialized(v);


        }
    }

    private static String getStringFromInputStream(InputStream is) throws IOException {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } finally {
            if (br != null) {
                br.close();
            }
        }
        String json = sb.toString();
        return json;

    }

    static final String LOG = PhotoCacheUtil.class.getSimpleName();
    static final Gson gson = new Gson();

    static class CacheRetrieveForUpdateTask extends AsyncTask<Void, Void, ResponseDTO> {

        private ResponseDTO getData(FileInputStream stream) throws IOException {
            String json = getStringFromInputStream(stream);
            ResponseDTO response = gson.fromJson(json, ResponseDTO.class);
            return response;
        }

        @Override
        protected ResponseDTO doInBackground(Void... voids) {
            ResponseDTO response = new ResponseDTO();
            response.setPhotoUploadList(new ArrayList<PhotoUploadDTO>());
            FileInputStream stream;
            try {
                stream = ctx.openFileInput(JSON_PHOTO);
                response = getData(stream);
            } catch (FileNotFoundException e) {
                Log.w(LOG, "############# cache file not found. not initialised yet. no problem, type = PHOTOS");
                return response;

            } catch (IOException e) {
                Log.d(LOG, "#### doInBackground - returning a new response object, type = PHOTOS");
            }

            return response;
        }

        @Override
        protected void onPostExecute(ResponseDTO v) {

            if (v.getPhotoUploadList() == null) {
                v.setPhotoUploadList(new ArrayList<PhotoUploadDTO>());
            }
            v.getPhotoUploadList().add(photoUpload);
            response = v;
            new CacheTask().execute();

        }
    }
}
