package com.justonebean.shortvideo.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.justonebean.shortvideo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2017-10-11.
 * Created by JustOneBean.
 * Address：JustOneBean@outlook.com
 * Description:
 */

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 0x101;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
        }

        findViewById(R.id.activity_main_video_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VideoEditActivity.class));
            }
        });

        findViewById(R.id.activity_main_video_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, VideoRecordActivity.class));
            }
        });

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        requestPermission();
    }

    /**
     * 请求权限
     */
    private void requestPermission() {
        List<String> noPermissions = new ArrayList<>();
        String[] noPermissionList = null;

        String[] permissionList = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS
        };

        for (int i = 0; i < permissionList.length; i++) {
            if (ActivityCompat.checkSelfPermission(this, permissionList[i]) == PackageManager.PERMISSION_DENIED) {
                noPermissions.add(permissionList[i]);
            }
        }

        if (!noPermissions.isEmpty()) {
            noPermissionList = noPermissions.toArray(new String[noPermissions.size()]);
        }
        if (noPermissionList != null && noPermissionList.length > 0) {
            ActivityCompat.requestPermissions(MainActivity.this, noPermissionList, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
