package com.example.zhangqi.fretxdemo.utils.bluetooth;

/**
 * FretXapp for FretX
 * Created by pandor on 24/04/17 19:46.
 */

public interface BluetoothListener {
    void onScanFailure();
    void onConnect();
    void onDisconnect();
    void onFailure();
}
