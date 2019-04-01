package com.lanabeji.opia.Model;

import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.TextUtils;

import java.util.List;

/**
 * Created by lanabeji on 30/03/19.
 */

public class NodeOpia {

    private CharSequence packageName;
    private CharSequence className;
    private CharSequence text;
    private CharSequence contentDescription;
    private String id;
    private int childCount;

    public NodeOpia(CharSequence packageName, CharSequence className, CharSequence text, CharSequence contentDescription, String id, int childCount) {
        this.packageName = packageName;
        this.className = className;
        this.text = text;
        this.contentDescription = contentDescription;
        this.id = id;
        this.childCount = childCount;
    }



    public CharSequence getPackageName() {
        return packageName;
    }

    public void setPackageName(CharSequence packageName) {
        this.packageName = packageName;
    }

    public CharSequence getClassName() {
        return className;
    }

    public void setClassName(CharSequence className) {
        this.className = className;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public CharSequence getContentDescription() {
        return contentDescription;
    }

    public void setContentDescription(CharSequence contentDescription) {
        this.contentDescription = contentDescription;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }


    public boolean equals(NodeOpia other){
        boolean pack = TextUtils.equals(packageName,other.getPackageName());
        boolean cl = TextUtils.equals(className, other.getClassName());
        boolean txt = text == other.getText();
        boolean cont = TextUtils.equals(contentDescription, other.getContentDescription());
        boolean i = id == other.getId();
        boolean count = childCount == other.getChildCount();

        return pack && cl && txt && cont && i && count;
    }
}
