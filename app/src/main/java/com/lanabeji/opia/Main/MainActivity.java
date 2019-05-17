package com.lanabeji.opia.Main;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import com.lanabeji.opia.AppList.ListActivity;
import com.lanabeji.opia.R;

import java.util.UUID;

/*
    Main activity, with the logo, name and start button
*/
public class MainActivity extends AppCompatActivity {

    //Name to save package selected in shared preferences
    public static final String PACKAGE = "PACKAGE_SELECTED";
    public static final String APP = "OPIA";
    public static final String SERVER = "SERVER";
    public static final String IP_SERVER = "IP_SERVER";

    //Identifier of device to save executions in firebase
    public final static String DEVICE = String.valueOf(UUID.randomUUID());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /*
    Start the activity that list all installed apps
    */
    public void start(View v)
    {
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

}


