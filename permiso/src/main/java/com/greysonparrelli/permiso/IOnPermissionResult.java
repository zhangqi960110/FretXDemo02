package com.greysonparrelli.permiso;

/**
 * FretXapp for FretX
 * Created by pandor on 21/04/17 01:49.
 */

public interface IOnPermissionResult {
    void onPermissionResult(ResultSet resultSet);
    void onRationaleRequested(IOnRationaleProvided callback, String... permissions);
}