package com.lanabeji.opia;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OpiaAccessibility extends AccessibilityService {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static String events = "events";
    private String executionTime = String.valueOf(String.valueOf(System.currentTimeMillis()));
    String device = String.valueOf(UUID.randomUUID());

    FrameLayout mLayout;
    Button powerButton;

    public OpiaAccessibility() {
    }

    // LIFECYCLE

    @Override
    protected void onServiceConnected() {
        Log.d("ACCESSIBILITY", "ON SERVICE");

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.packageNames = addPackages();

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_VIEW_CLICKED
                | AccessibilityEvent.TYPE_VIEW_SCROLLED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 1000;

        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        setServiceInfo(info);

        configureStopButton();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        SharedPreferences preferences = getSharedPreferences(MainActivity.APP, MODE_PRIVATE);
        String packageSelected = preferences.getString(MainActivity.PACKAGE, "com.whatsapp");

        if (String.valueOf(event.getPackageName()).equals(packageSelected)){

            powerButton.setVisibility(View.VISIBLE);
            Log.d("ON EVENT", String.valueOf(event.getEventType()));
            String timestampEvent = String.valueOf(System.currentTimeMillis());

            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                handleTextEvent(event, packageSelected, timestampEvent);
            }
            else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){

                AccessibilityNodeInfo source = event.getSource();
                if (source == null) {
                    return;
                }
                handleWindowEvent(source, timestampEvent);
            }
            else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED ){
                event.getSource().refresh();
                handleClickEvent(event.getSource(), timestampEvent);
            }
            else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED){
                handleScrollEvent(event, timestampEvent);
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    // CONFIGURATION METHODS

    public String[] addPackages(){

        final PackageManager pm = getPackageManager();

        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        String[] apps = new String[packages.size()];

        for(int i = 0; i < packages.size(); i++){
            ApplicationInfo packageInfo = packages.get(i);
            Intent launchActivity = pm.getLaunchIntentForPackage(packageInfo.packageName);
            if (!packageInfo.packageName.startsWith("com.android") && launchActivity != null){
                apps[i] = packageInfo.packageName;
            }
        }
        return apps;
    }

    private void configureStopButton(){

        // Create an overlay and display the action bar
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLayout = new FrameLayout(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.TOP | Gravity.RIGHT;

        LayoutInflater inflater = LayoutInflater.from(this);
        inflater.inflate(R.layout.action_button, mLayout);
        wm.addView(mLayout, lp);

        powerButton = (Button) mLayout.findViewById(R.id.stop);
        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
    }

    // EVENT'S METHODS

    private void handleTextEvent(AccessibilityEvent event, String packageSelected, String timestampEvent){

        String text = "";
        if (!event.getSource().isPassword()) {
            text = event.getSource().getText().toString();
        }

        String elementId = event.getSource().getViewIdResourceName();
        String className = String.valueOf(event.getSource().getClassName());

        Log.d("TEXT_CHANGED", text);

        Map<String, String> newEvent = new HashMap<>();
        newEvent.put("package", packageSelected);
        newEvent.put("device", device);
        newEvent.put("executionTime", executionTime);
        newEvent.put("eventType", "text");
        newEvent.put("elementId", elementId);
        newEvent.put("text", text);
        newEvent.put("className", className);


        writeEvent(newEvent, timestampEvent);

    }

    private void handleWindowEvent(AccessibilityNodeInfo source, String timestampWindow) {
        if (source == null) {
            return;
        }
        if (("android.widget.TextView").equals(source.getClassName()) || ("android.widget.EditText")
                .equals(source.getClassName())) {
            String id = source.getViewIdResourceName();

            String eventData = "id: " + id + ", text:" + source.getText();
            String text = String.valueOf(source.getText());
            String className = String.valueOf(source.getClassName());
            String timestampEvent = String.valueOf(System.currentTimeMillis());
            String packageSelected = String.valueOf(source.getPackageName());

            Log.d("ACTIVITY", eventData);

            Map<String, String> newEvent = new HashMap<>();
            newEvent.put("package", packageSelected);
            newEvent.put("device", device);
            newEvent.put("executionTime", executionTime);
            newEvent.put("eventType", "window");
            newEvent.put("elementId", id);
            newEvent.put("text", text);
            newEvent.put("className", className);
            newEvent.put("windowTime", timestampWindow);

            writeEvent(newEvent, timestampEvent);

        }
        for (int i = 0; i < source.getChildCount(); i++) {
            AccessibilityNodeInfo child = source.getChild(i);
            if (child != null) {
                handleWindowEvent(child, timestampWindow);
                child.recycle();
            }
        }
    }

    private void handleClickEvent(AccessibilityNodeInfo source, String timestampClick){

        if(source != null){
            source.refresh();
        }

        String text = "";
        if (source.getText() != null){
            text = source.getText().toString();
        }

        String packageSelected = String.valueOf(source.getPackageName());
        String elementId = source.getViewIdResourceName();
        String className = String.valueOf(source.getClassName());
        int childCount = source.getChildCount();
        String timestampCurrentEvent = String.valueOf(System.currentTimeMillis());

        Log.d("CLICKED", "en click");

        Map<String, String> newEvent = new HashMap<>();
        newEvent.put("package", packageSelected);
        newEvent.put("device", device);
        newEvent.put("executionTime", executionTime);
        newEvent.put("eventType", "click");
        newEvent.put("elementId", elementId);
        newEvent.put("text", text);
        newEvent.put("className", className);
        newEvent.put("childCount", String.valueOf(childCount));
        newEvent.put("clickTime", timestampClick);

        writeEvent(newEvent, timestampCurrentEvent);

        Log.d("ANTES DEL FOR", "F");
        for (int i = 0; i < source.getChildCount(); i++) {

            Log.d("DENTRO DEL FOR", "F");
            AccessibilityNodeInfo child = source.getChild(i);
            if (child != null) {
                Log.d("DENTRO DEL IF","F");
                handleClickEvent(child, timestampClick);
                child.recycle();
            }
        }
        Log.d("FUERA DEL FOR","F");
    }

    private void handleScrollEvent(AccessibilityEvent event, String timestampEvent){

        AccessibilityNodeInfo source = event.getSource();

        String text = String.valueOf(source.getText());
        String packageSelected = String.valueOf(event.getPackageName());
        String elementId = source.getViewIdResourceName();
        String className = String.valueOf(source.getClassName());
        int scrollX = event.getScrollX();
        int scrollY = event.getScrollY();

        Log.d("SCROLLED", String.valueOf(event.getEventType()));

        Map<String, String> newEvent = new HashMap<>();
        newEvent.put("package", packageSelected);
        newEvent.put("device", device);
        newEvent.put("executionTime", executionTime);
        newEvent.put("eventType", "scroll");
        newEvent.put("elementId", elementId);
        newEvent.put("text", text);
        newEvent.put("className", className);
        newEvent.put("scroll", ""+scrollX+"/"+scrollY);

        writeEvent(newEvent, timestampEvent);
    }

    private void writeEvent(Map<String,String> newEvent, String timestampEvent){

        // Add a new document with timestamp as id
        db.collection(events).document(timestampEvent).set(newEvent).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("ON SUCCESS", "DocumentSnapshot added with ID: " + aVoid);
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("ON FAILURE", "Error adding document", e);
                    }
                });
    }


    private void takeScreenshot(String number){
        try{
            String command = "adb shell screencap /sdcard/" + number + ".png";
            Runtime.getRuntime().exec(command);
        }
        catch(Exception e){
            Log.d("ERROR", "Couldn't take screenshot");
            Log.d("ERROR", e.getStackTrace().toString());
        }

    }






}
