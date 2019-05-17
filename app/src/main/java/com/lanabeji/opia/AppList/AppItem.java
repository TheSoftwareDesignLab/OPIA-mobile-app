package com.lanabeji.opia.AppList;

import android.graphics.drawable.Drawable;

/**
 * Created by lanabeji on 17/03/19.
 * Object that represents an app installed with the package name, name and icon
 */

public class AppItem {

    private String name;
    private String packageName;
    private Drawable img;

    //Constructor
    public AppItem() {
        this.packageName = "";
        this.name = "";
    }

    //Getters & Setters
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
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

    public void setName(String name) { this.name = name; }
}
