package com.lanabeji.opia;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    AppListAdapter appListAdapter;
    ArrayList<AppItem> apps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        apps = new ArrayList<AppItem>();

        final PackageManager pm = getPackageManager();
        //get a list of installed apps.

        String tag = "APP INFO";
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {

            Intent launchActivity = pm.getLaunchIntentForPackage(packageInfo.packageName);

            if (!packageInfo.packageName.startsWith("com.android") && launchActivity != null && !packageInfo.packageName.equals("com.lanabeji.opia")){
                AppItem currentApp = new AppItem();

                currentApp.setPackageName(packageInfo.packageName);
                currentApp.setSourceDir(packageInfo.sourceDir);
                currentApp.setLaunchActivity(launchActivity.toString());
                currentApp.setImg(packageInfo.loadIcon(getPackageManager()));
                currentApp.setName(packageInfo.loadLabel(getPackageManager()).toString());
                apps.add(currentApp);
            }
        }

        Collections.sort(apps, new Comparator<AppItem>() {
            public int compare(AppItem o1, AppItem o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        appListAdapter = new AppListAdapter(apps);

        recyclerView = (RecyclerView)findViewById(R.id.rvApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(appListAdapter);
    }
}
