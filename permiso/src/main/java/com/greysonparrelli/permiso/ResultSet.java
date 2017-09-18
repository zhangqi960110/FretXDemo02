package com.greysonparrelli.permiso;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FretXapp for FretX
 * Created by pandor on 21/04/17 01:44.
 */

public class ResultSet {

    Map<String, Result> requestResults;

    ResultSet(String... permissions) {
        requestResults = new HashMap<>(permissions.length);
        for (String permission : permissions) {
            requestResults.put(permission, Result.DENIED);
        }
    }

    public boolean isPermissionGranted(String permission) {
        return requestResults.containsKey(permission) && requestResults.get(permission) == Result.GRANTED;
    }

    public boolean areAllPermissionsGranted() {
        return !requestResults.containsValue(Result.DENIED) && !requestResults.containsValue(Result.PERMANENTLY_DENIED);
    }

    public boolean isPermissionPermanentlyDenied(String permission) {
        return requestResults.containsKey(permission) && requestResults.get(permission) == Result.PERMANENTLY_DENIED;
    }

    public Map<String, Result> toMap() {
        return new HashMap<>(requestResults);
    }

    void grantPermissions(String... permissions) {
        for (String permission : permissions) {
            requestResults.put(permission, Result.GRANTED);
        }
    }

    void parsePermissionResults(String[] permissions, int[] grantResults, Activity activity) {
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                requestResults.put(permissions[i], Result.GRANTED);
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i])) {
                requestResults.put(permissions[i], Result.PERMANENTLY_DENIED);
            } else {
                requestResults.put(permissions[i], Result.DENIED);
            }
        }
    }

    String[] getUngrantedPermissions() {
        List<String> ungrantedList = new ArrayList<>(requestResults.size());
        for (Map.Entry<String, Result> requestResultsEntry : requestResults.entrySet()) {
            Result result = requestResultsEntry.getValue();
            if (result == Result.DENIED || result == Result.PERMANENTLY_DENIED) {
                ungrantedList.add(requestResultsEntry.getKey());
            }
        }
        return ungrantedList.toArray(new String[ungrantedList.size()]);
    }

    boolean containsAllUngrantedPermissions(ResultSet set) {
        List<String> ungranted = Arrays.asList(set.getUngrantedPermissions());
        return requestResults.keySet().containsAll(ungranted);
    }

    String[] getPermissionsThatNeedRationale(Activity activity) {
        String[] ungranted = getUngrantedPermissions();
        List<String> shouldShowRationale = new ArrayList<>(ungranted.length);
        for (String permission : ungranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale.add(permission);
            }
        }
        return shouldShowRationale.toArray(new String[shouldShowRationale.size()]);
    }
}
