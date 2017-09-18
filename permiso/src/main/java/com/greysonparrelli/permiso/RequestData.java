package com.greysonparrelli.permiso;

import android.support.annotation.NonNull;

/**
 * FretXapp for FretX
 * Created by pandor on 21/04/17 01:47.
 */

class RequestData {
    IOnPermissionResult onResultListener;
    ResultSet resultSet;

    public RequestData(@NonNull IOnPermissionResult onResultListener, String... permissions) {
        this.onResultListener = onResultListener;
        resultSet = new ResultSet(permissions);
    }
}