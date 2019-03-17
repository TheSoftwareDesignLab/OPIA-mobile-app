package com.lanabeji.opia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    public static final String PACKAGE = "PACKAGE_SELECTED";
    public static final String APP = "OPIA";
    Context context;
    SharedPreferences sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        sharedPref = context.getSharedPreferences(APP, Context.MODE_PRIVATE);
    }

    public void writeSelected(String packageSelected){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PACKAGE, packageSelected);
        editor.commit();
    }

    public void onClickWA(View v)
    {
        writeSelected("com.whatsapp");
    }

    public void onClickYT(View v)
    {
        writeSelected("com.google.android.youtube");
    }

    public void onClickFB(View v)
    {
        writeSelected("com.facebook.katana");
    }
}
