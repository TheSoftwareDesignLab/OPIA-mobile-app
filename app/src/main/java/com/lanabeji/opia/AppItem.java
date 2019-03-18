package com.lanabeji.opia;

import android.graphics.drawable.Drawable;

/**
 * Created by lanabeji on 17/03/19.
 */

public class AppItem {

    private String name;
    private String packageName;
    private String sourceDir;
    private String launchActivity;
    private Drawable img;

    public AppItem() {
        this.packageName = "";
        this.sourceDir = "";
        this.launchActivity = "";
        this.name = "";
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getSourceDir() {
        return sourceDir;
    }

    public void setSourceDir(String sourceDir) {
        this.sourceDir = sourceDir;
    }

    public String getLaunchActivity() {
        return launchActivity;
    }

    public void setLaunchActivity(String launchActivity) {
        this.launchActivity = launchActivity;
    }

    public Drawable getImg() {
        return img;
    }

    public void setImg(Drawable img) {
        this.img = img;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



}
