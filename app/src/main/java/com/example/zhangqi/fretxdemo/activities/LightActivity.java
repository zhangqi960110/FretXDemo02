package com.example.zhangqi.fretxdemo.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhangqi.fretxdemo.R;
import com.example.zhangqi.fretxdemo.utils.bluetooth.BluetoothInterface;
import com.example.zhangqi.fretxdemo.utils.bluetooth.BluetoothLEService;
import com.example.zhangqi.fretxdemo.utils.bluetooth.BluetoothListener;
import com.example.zhangqi.fretxdemo.view.FretboardView;

import java.util.ArrayList;

import rocks.fretx.audioprocessing.FretboardPosition;

public class LightActivity extends AppCompatActivity implements BluetoothListener, View.OnClickListener {

    private static final String TAG = "KJKP6_MAIMACTIVITY";
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 1;

    private static final byte clearBytes[] = new byte[]{0};
    private static final byte emBytes[] = new byte[]{24, 25, 01, 02, 03, 06, 0};
    private static final byte asus2Bytes[] = new byte[]{23, 24, 01, 02, 05, 0};

    private static final byte chord1[] = new byte[]{06};
    private static final byte chord2[] = new byte[]{06, 25};
    private static final byte chord3[] = new byte[]{06, 25, 24};
    private static final byte chord4[] = new byte[]{06, 25, 24, 03};
    private static final byte chord5[] = new byte[]{06, 25, 24, 03, 02};
    private static final byte chord6[] = new byte[]{06, 25, 24, 03, 02, 01};

    ArrayList<byte[]> byteList = new ArrayList<>();


    private static final byte fretx[][] = new byte[][]{{6,0},{5, 2}, {4, 2}, {3, 0}, {2, 0}, {1, 0}};

    private FretboardView mFretboardView;
    ArrayList<FretboardPosition> list = new ArrayList<>();

    ArrayList<FretboardPosition> emList = new ArrayList<>();
    ArrayList<FretboardPosition> asusList = new ArrayList<>();

    private TextView status;
    private Button nextBtn;
    private Button action;
    private Button sendEm;
    private Button sendAsus2;
    private Button lightOff;

    private BluetoothInterface com;
    private BluetoothListener listener = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_light);

        status = (TextView) findViewById(R.id.status_textview);
        nextBtn = (Button) findViewById(R.id.next_btn);
        nextBtn.setVisibility(View.INVISIBLE);
        action = (Button) findViewById(R.id.action_button);
        sendEm = (Button) findViewById(R.id.send_em);
        sendEm.setVisibility(View.INVISIBLE);
        sendAsus2 = (Button) findViewById(R.id.send_asus2);
        sendAsus2.setVisibility(View.INVISIBLE);
        lightOff = (Button) findViewById(R.id.light_off);
        lightOff.setVisibility(View.INVISIBLE);

        mFretboardView = (FretboardView) findViewById(R.id.fretboardView);

        emList.add(new FretboardPosition(5,0));
        emList.add(new FretboardPosition(3,0));
        emList.add(new FretboardPosition(2,0));
        emList.add(new FretboardPosition(1,0));
        emList.add(new FretboardPosition(5,2));
        emList.add(new FretboardPosition(4,2));

        asusList.add(new FretboardPosition(5,0));
        asusList.add(new FretboardPosition(2,0));
        asusList.add(new FretboardPosition(1,0));
        asusList.add(new FretboardPosition(4,2));
        asusList.add(new FretboardPosition(3,2));

        byteList.add(chord1);
        byteList.add(chord2);
        byteList.add(chord3);
        byteList.add(chord4);
        byteList.add(chord5);
        byteList.add(chord6);

        nextBtn.setOnClickListener(this);
        action.setOnClickListener(this);
        sendEm.setOnClickListener(this);
        sendAsus2.setOnClickListener(this);
        lightOff.setOnClickListener(this);

        requestRuntimePermissions();

    }

    private static int num = 0;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_button:
                if (com != null) {
                    if (com.isConnected()) {
                        com.disconnect();
                        nextBtn.setVisibility(View.INVISIBLE);
                        sendEm.setVisibility(View.INVISIBLE);
                        sendAsus2.setVisibility(View.INVISIBLE);
                        lightOff.setVisibility(View.INVISIBLE);
                    } else {
                        status.setText("Scanning...");
                        com.connect();
                    }
                }
                break;
            case R.id.next_btn:
                if (num < 6) {
                    list.add(new FretboardPosition(fretx[num][0], fretx[num][1]));
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFretboardView.setFretboardPositions(list);
                        }
                    });
                    com.send(byteList.get(num));
                    num++;
                    if(num != 0){
                        nextBtn.setText("NEXT");
                    }
                }else {
                    Toast.makeText(LightActivity.this, "测试完成", Toast.LENGTH_SHORT).show();
                    list.clear();
                    num = 0;
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFretboardView.setFretboardPositions(list);
                            nextBtn.setText("RESTART");
                        }
                    });
                    com.send(clearBytes);
                }
                break;
            case R.id.send_em:
                if (com != null) {
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFretboardView.setFretboardPositions(emList);
                        }
                    });
                    com.send(emBytes);
                }
                break;
            case R.id.send_asus2:
                if (com != null) {
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFretboardView.setFretboardPositions(asusList);
                        }
                    });
                    com.send(asus2Bytes);
                }
                break;
            case R.id.light_off:
                if (com != null) {
                    com.send(clearBytes);
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    launchCommunicationService();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void requestRuntimePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            }
        } else {
            launchCommunicationService();
        }
    }

    private void launchCommunicationService() {
        final Intent intent = new Intent(this, BluetoothLEService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
                com = (BluetoothInterface) serviceBinder;
                com.registerBluetoothListener(listener);
                status.setText(R.string.scanning_status);
                com.connect();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                //com.unregisterBluetoothListener(listener);
                com = null;
            }
        }, Context.BIND_AUTO_CREATE);
    }

    public void onScanFailure() {
        Log.d(TAG, "onScanFailure");
        this.runOnUiThread(disconnectedRunnable);
        com.send(new byte[]{0});
    }

    public void onConnect() {
        Log.d(TAG, "onConnect");
        this.runOnUiThread(connectedRunnable);
    }

    public void onDisconnect() {
        Log.d(TAG, "onDisconnect");
        this.runOnUiThread(disconnectedRunnable);
    }

    public void onFailure() {
        Log.d(TAG, "onFailure");
        this.runOnUiThread(disconnectedRunnable);
    }

    private Runnable connectedRunnable = new Runnable() {
        @Override
        public void run() {
            status.setText(R.string.connected_status);
            action.setText(R.string.disconnect_action);
            nextBtn.setVisibility(View.VISIBLE);
            sendEm.setVisibility(View.VISIBLE);
            sendAsus2.setVisibility(View.VISIBLE);
            lightOff.setVisibility(View.VISIBLE);
        }
    };

    private Runnable disconnectedRunnable = new Runnable() {
        @Override
        public void run() {
            status.setText(R.string.disconnected_status);
            action.setText(R.string.connect_action);
            nextBtn.setVisibility(View.INVISIBLE);
            sendEm.setVisibility(View.INVISIBLE);
            sendAsus2.setVisibility(View.INVISIBLE);
            lightOff.setVisibility(View.INVISIBLE);
            list.clear();
            mFretboardView.setFretboardPositions(list);
        }
    };
}
