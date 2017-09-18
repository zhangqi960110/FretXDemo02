package com.greysonparrelli.permiso;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class Permiso {

    private static final String TAG = "KJKP6_PERMISO_LIB";
    private final Map<Integer, RequestData> mCodesToRequests = new HashMap<>();;
    private WeakReference<Activity> mActivity;
    private int mActiveRequestCode = 1;
    private static Permiso sInstance = new Permiso();
    private IOnPermissionComplete onComplete;

    public static Permiso getInstance() {
        return sInstance;
    }
    private Permiso() {}

    public void setActivity(@NonNull Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    public void setOnComplete(IOnPermissionComplete onComplete) {
        this.onComplete = onComplete;
    }

    @MainThread
    public void requestPermissions(@NonNull IOnPermissionResult callback, String... permissions) {
        Activity activity = checkActivity();

        final RequestData requestData = new RequestData(callback, permissions);

        // Mark any permissions that are already granted
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
                requestData.resultSet.grantPermissions(permission);
            }
        }

        // If we had all of them, yay! No need to do anything else.
        if (requestData.resultSet.areAllPermissionsGranted()) {
            Log.d(TAG, "All permissions already granted");
            requestData.onResultListener.onPermissionResult(requestData.resultSet);
            onComplete.onComplete();
        } else {
            // If we have some unsatisfied ones, let's first see if they can be satisfied by an active request. If it
            // can, we'll re-wire the callback of the active request to also trigger this new one.
            boolean linkedToExisting = linkToExistingRequestIfPossible(requestData);

            // If there was no existing request that can satisfy this one, then let's make a new permission request to
            // the system
            if (!linkedToExisting) {
                // Mark the request as active
                final int requestCode = markRequestAsActive(requestData);

                // First check if there's any permissions for which we need to provide a rationale for using
                String[] permissionsThatNeedRationale = requestData.resultSet.getPermissionsThatNeedRationale(activity);

                // If there are some that need a rationale, show that rationale, then continue with the request
                if (permissionsThatNeedRationale.length > 0) {
                    requestData.onResultListener.onRationaleRequested(new IOnRationaleProvided() {
                        @Override
                        public void onRationaleProvided() {
                            makePermissionRequest(requestCode);
                        }
                    }, permissionsThatNeedRationale);
                } else {
                    makePermissionRequest(requestCode);
                }
            }
        }
    }
   @MainThread
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        Activity activity = checkActivity();
        if (mCodesToRequests.containsKey(requestCode)) {
            RequestData requestData = mCodesToRequests.get(requestCode);
            requestData.resultSet.parsePermissionResults(permissions, grantResults, activity);
            requestData.onResultListener.onPermissionResult(requestData.resultSet);
            mCodesToRequests.remove(requestCode);
            onComplete.onComplete();
        } else {
            Log.w(TAG, "onRequestPermissionResult() was given an unrecognized request code.");
        }
    }


    @MainThread
    public void showRationaleInDialog(
            @Nullable String title,
            @NonNull String message,
            @Nullable String buttonText,
            @NonNull final IOnRationaleProvided rationaleCallback) {
        PermisoDialogFragment.Builder builder = new PermisoDialogFragment.Builder()
                .setTitle(title)
                .setMessage(message)
                .setButtonText(buttonText);
        showRationaleInDialog(builder, rationaleCallback);
    }

    @MainThread
    private void showRationaleInDialog(
            final PermisoDialogFragment.Builder builder,
            final IOnRationaleProvided rationaleCallback) {
        Activity activity = checkActivity();
        FragmentManager fm = activity.getFragmentManager();

        PermisoDialogFragment dialogFragment = (PermisoDialogFragment) fm.findFragmentByTag(PermisoDialogFragment.TAG);
        if (dialogFragment != null) {
            dialogFragment.dismiss();
        }

        dialogFragment = builder.build(activity);

        // We show the rationale after the dialog is closed. We use setRetainInstance(true) in the dialog to ensure that
        // it retains the listener after an app rotation.
        dialogFragment.setOnCloseListener(new PermisoDialogFragment.IOnCloseListener() {
            @Override
            public void onClose() {
                rationaleCallback.onRationaleProvided();
            }
        });
        dialogFragment.show(fm, PermisoDialogFragment.TAG);
    }

    private boolean linkToExistingRequestIfPossible(final RequestData newRequest) {
        boolean found = false;

        // Go through all outstanding requests
        for (final RequestData activeRequest : mCodesToRequests.values()) {
            // If we find one that can satisfy all of the new request's permissions, we re-wire the active one's
            // callback to also call this new one's callback
            if (activeRequest.resultSet.containsAllUngrantedPermissions(newRequest.resultSet)) {
                final IOnPermissionResult originalOnResultListener = activeRequest.onResultListener;
                activeRequest.onResultListener = new IOnPermissionResult() {
                    @Override
                    public void onPermissionResult(ResultSet resultSet) {
                        // First, call the active one's callback. It was added before this new one.
                        originalOnResultListener.onPermissionResult(resultSet);

                        // Next, copy over the results to the new one's resultSet
                        String[] unsatisfied = newRequest.resultSet.getUngrantedPermissions();
                        for (String permission : unsatisfied) {
                            newRequest.resultSet.requestResults.put(permission, resultSet.requestResults.get(permission));
                        }

                        // Finally, trigger the new one's callback
                        newRequest.onResultListener.onPermissionResult(newRequest.resultSet);
                    }

                    @Override
                    public void onRationaleRequested(IOnRationaleProvided callback, String... permissions) {
                        activeRequest.onResultListener.onRationaleRequested(callback, permissions);
                    }
                };
                found = true;
                break;
            }
        }
        return found;
    }

    private int markRequestAsActive(RequestData requestData) {
        int requestCode = mActiveRequestCode++;
        mCodesToRequests.put(requestCode, requestData);
        return requestCode;
    }

    private void makePermissionRequest(int requestCode) {
        Activity activity = checkActivity();
        RequestData requestData = mCodesToRequests.get(requestCode);
        String[] s = requestData.resultSet.getUngrantedPermissions();
        ActivityCompat.requestPermissions(activity, s, requestCode);
    }

    private Activity checkActivity() {
        Activity activity = mActivity.get();
        if (activity == null) {
            throw new IllegalStateException("No activity set. Either subclass PermisoActivity or call Permiso.setActivity() in onCreate() and onResume() of your Activity.");
        }
        return activity;
    }
}
