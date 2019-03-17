package com.lanabeji.opia;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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

    public OpiaAccessibility() {
    }

    @Override
    protected void onServiceConnected() {
        Log.d("ACCESSIBILITY", "ON SERVICE");

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        
        info.packageNames = addPackages();

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 1000;

        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
        Log.d("ACCESSIBILITY", "AFTER SETS");

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        SharedPreferences preferences = getSharedPreferences(MainActivity.APP, MODE_PRIVATE);
        String packageSelected = preferences.getString(MainActivity.PACKAGE, "com.whatsapp");

        if (String.valueOf(event.getPackageName()).equals(packageSelected)){

            Log.d("ON EVENT", String.valueOf(event.getEventType()));
            String timestampEvent = String.valueOf(System.currentTimeMillis());

            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {

                String text = "";
                if (!event.getSource().isPassword()) {
                    text = event.getSource().getText().toString();
                }

                String elementId = event.getSource().getViewIdResourceName();
                String className = String.valueOf(event.getSource().getClassName());

                Log.d("TEXT_CHANGED", text);

                Map<String, String> newEvent = new HashMap<>();
                newEvent.put("device", device);
                newEvent.put("executionTime", executionTime);
                newEvent.put("eventType", "text");
                newEvent.put("elementId", elementId);
                newEvent.put("text", text);
                newEvent.put("className", className);

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
            else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){

                AccessibilityNodeInfo source = event.getSource();
                if (source == null) {
                    return;
                }

                printAllText(source, timestampEvent);

            }
        }
    }

    private void printAllText(AccessibilityNodeInfo source, String timestampWindow) {
        if (source == null) {
            return;
        }
        if (("android.widget.TextView").equals(source.getClassName()) || ("android.widget.EditText")
                .equals(source.getClassName())) {
            String id = source.getViewIdResourceName();
            if (id != null) {
                id = id.split("/")[1];
            }
            String eventData = "id: " + id + ", text:" + source.getText();
            String text = String.valueOf(source.getText());
            String className = String.valueOf(source.getClassName());
            String timestampEvent = String.valueOf(System.currentTimeMillis());

            Log.d("ACTIVITY WHATSAPP", eventData);

            Map<String, Object> newEvent = new HashMap<>();
            newEvent.put("device", device);
            newEvent.put("executionTime", executionTime);
            newEvent.put("eventType", "window");
            newEvent.put("elementId", id);
            newEvent.put("text", text);
            newEvent.put("className", className);
            newEvent.put("windowTime", timestampWindow);


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
        for (int i = 0; i < source.getChildCount(); i++) {
            AccessibilityNodeInfo child = source.getChild(i);
            if (child != null) {
                printAllText(child, timestampWindow);
                child.recycle();
            }
        }
    }

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

    @Override
    public void onInterrupt() {

    }
}
