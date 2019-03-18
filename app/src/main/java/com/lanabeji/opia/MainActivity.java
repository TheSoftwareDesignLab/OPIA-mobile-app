package com.lanabeji.opia;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

public class MainActivity extends AppCompatActivity {

    public static final String PACKAGE = "PACKAGE_SELECTED";
    public static final String APP = "OPIA";
    private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 1;
    Context context;
    SharedPreferences sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
        setContentView(R.layout.activity_main);
        context = this;
        sharedPref = context.getSharedPreferences(APP, Context.MODE_PRIVATE);

        askPermission();
    }

    public void writeSelected(String packageSelected){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PACKAGE, packageSelected);
        editor.commit();
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

/*        int readExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(readExternalStoragePermission!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION);
        }*/
    }

    private void seeExternal(){
        Log.d("EXTERNAL",context.getExternalFilesDirs(null).toString());
    }
}
