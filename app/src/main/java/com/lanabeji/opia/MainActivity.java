package com.lanabeji.opia;

import android.*;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public static final String PACKAGE = "PACKAGE_SELECTED";
    public static final String APP = "OPIA";
    public static final String SERVER = "SERVER";
    private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 1;
    Context context;
    SharedPreferences sharedPref;
    public final static String DEVICE = String.valueOf(UUID.randomUUID());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
;        setContentView(R.layout.activity_main);
        context = this;
        sharedPref = context.getSharedPreferences(APP, Context.MODE_PRIVATE);
    }

    public void start(View v)
    {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    public void askPermission(){
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    public void ask2Permission(){
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(writeExternalStoragePermission!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    private void seeExternal(){
        Log.d("EXTERNAL",context.getExternalFilesDirs(null).toString());
    }

}


