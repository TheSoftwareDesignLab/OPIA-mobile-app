package com.lanabeji.opia;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OpiaAccessibility extends AccessibilityService {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static String events = "events";
    private static String[] labels = new String[]{"packageName", "className", "elementId", "text", "childCount", "contentDescription", "isClickable", "deviceId", "executionTime", "eventType", "eventTime"};
    private String executionTime = String.valueOf(String.valueOf(System.currentTimeMillis()));
    String device = MainActivity.DEVICE;
    boolean test = false;

    FrameLayout mLayout;
    Button powerButton;
    ArrayList<String> texts = new ArrayList<>();
    ArrayList<String> ids = new ArrayList<>();

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
                | AccessibilityEvent.TYPE_VIEW_SCROLLED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 1000;

        info.flags = AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME | AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;

        setServiceInfo(info);

        configureStopButton();
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        SharedPreferences preferences = getSharedPreferences(MainActivity.APP, MODE_PRIVATE);
        String packageSelected = preferences.getString(MainActivity.PACKAGE, "com.whatsapp");
        boolean isRecording = preferences.getBoolean(AppListAdapter.RECORDING, true);


        if (String.valueOf(event.getPackageName()).equals(packageSelected)){

            if(isRecording){
                powerButton.setVisibility(View.VISIBLE);
                Log.d("ON EVENT", String.valueOf(event.getEventType()));

                String timestampEvent = String.valueOf(System.currentTimeMillis());
                //List<AccessibilityNodeInfo> encontrado = getRootInActiveWindow().findAccessibilityNodeInfosByText("+57 319 367197");

                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                    handleTextEvent(event.getSource(), timestampEvent);
                }
/*            else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){

                AccessibilityNodeInfo source = event.getSource();
                if (source == null) {
                    return;
                }
                handleWindowEvent(source, timestampEvent);
            }*/
                else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){

                    AccessibilityNodeInfo source = event.getSource();
                    if (source == null) {
                        return;
                    }

/*                    if(test){
                        //readAllNodes(source);
                        findNodeText("Trabajo", getRootInActiveWindow());
                    }*/


                    handleWindowEvent(source, timestampEvent);
                }
                else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED ){
                    event.getSource().refresh();
                    //test = true;
                    //readAllNodes(event.getSource());
                    handleClickEvent(event.getSource(), timestampEvent);
                }
                else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED){
                    handleScrollEvent(event.getSource(), timestampEvent);
                }
            }
            else { // Replaying

                Collections.sort(texts);
                Collections.sort(ids);

                System.out.println(texts);
                System.out.println(ids);

                AccessibilityNodeInfo current = getRootInActiveWindow();
                for(int i = 0; i < texts.size(); i++){
                    String text = texts.get(i).split("//")[1];
                    String id = ids.get(i).split("//")[1];

                    AccessibilityNodeInfo found = findNodeText(text, getRootInActiveWindow());
                    if(found != null){
                        found.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        //found.setText(); PARA LAS DE TEXTO
                        //found.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD); 8192 PARA SABER HACIA DONDE FUE EL SCROLL
                        //found.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD); 4096 PARA SABER HACIA DONDE FUE EL SCROLL
                    }
                }
                //Sigue repitiendo las acciones
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
            if (!packageInfo.packageName.startsWith("com.android") && launchActivity != null && !packageInfo.packageName.equals("com.lanabeji.opia")){
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

                readEvent();
                powerButton.setVisibility(View.INVISIBLE);
                Intent dialogIntent = new Intent(getBaseContext(), ListActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
            }
        });
    }

    // EVENT'S METHODS

    private void handleTextEvent(AccessibilityNodeInfo source, String timestampEvent){

        String packageName = String.valueOf(source.getPackageName());
        String className = String.valueOf(source.getClassName());
        String elementId = source.getViewIdResourceName();

        String text = "";
        if (!source.isPassword()) {
            text = source.getText().toString();
        }

        String childCount = String.valueOf(source.getChildCount());
        String contentDescription = String.valueOf(source.getContentDescription());
        String isClickable = String.valueOf(source.isClickable());

        String eventType = "text";
        String[] values = new String[]{packageName, className, elementId, text, childCount, contentDescription, isClickable, device, executionTime, eventType};

        writeEvent(values, timestampEvent);

    }

    private void handleWindowEvent(AccessibilityNodeInfo source, String timestampWindow) {
        if (source == null) {
            return;
        }
        if (("android.widget.TextView").equals(source.getClassName()) || ("android.widget.EditText")
                .equals(source.getClassName())) {

            String timestampEvent = String.valueOf(System.currentTimeMillis());

            String packageName = String.valueOf(source.getPackageName());
            String className = String.valueOf(source.getClassName());
            String elementId = source.getViewIdResourceName();
            String text = String.valueOf(source.getText());

            String childCount = String.valueOf(source.getChildCount());
            String contentDescription = String.valueOf(source.getContentDescription());
            String isClickable = String.valueOf(source.isClickable());

            String eventType = "window";
            String eventTime = timestampWindow;
            String[] values = new String[]{packageName, className, elementId, text, childCount, contentDescription, isClickable, device, executionTime, eventType, eventTime};

            writeEvent(values, timestampEvent);
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

        String timestampEvent = String.valueOf(System.currentTimeMillis());

        String packageName = String.valueOf(source.getPackageName());
        String className = String.valueOf(source.getClassName());
        String elementId = source.getViewIdResourceName();
        String text = String.valueOf(source.getText());

        String childCount = String.valueOf(source.getChildCount());
        String contentDescription = String.valueOf(source.getContentDescription());
        String isClickable = String.valueOf(source.isClickable());

        String eventType = "click";
        String eventTime = timestampClick;
        String[] values = new String[]{packageName, className, elementId, text, childCount, contentDescription, isClickable, device, executionTime, eventType, eventTime};

        writeEvent(values, timestampEvent);
        writeEventDevice(timestampEvent, eventType);
    }


    private void handleScrollEvent(AccessibilityNodeInfo source, String timestampEvent){

        String packageName = String.valueOf(source.getPackageName());
        String className = String.valueOf(source.getClassName());
        String elementId = source.getViewIdResourceName();
        String text = String.valueOf(source.getText());

        String childCount = String.valueOf(source.getChildCount());
        String contentDescription = String.valueOf(source.getContentDescription());
        String isClickable = String.valueOf(source.isClickable());

        String eventType = "scroll";
        String[] values = new String[]{packageName, className, elementId, text, childCount, contentDescription, isClickable, device, executionTime, eventType};

        writeEvent(values, timestampEvent);
        writeEventDevice(timestampEvent, eventType);
    }

    private void writeEvent(String[] values, String timestampEvent){


        Map<String, String> newEvent = new HashMap<>();

        for(int i = 0; i < values.length; i++){
            newEvent.put(labels[i], values[i]);
        }

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


    private void readAllNodes(AccessibilityNodeInfo source){

        AccessibilityNodeInfo labeledBy = source.getLabeledBy();
        AccessibilityNodeInfo labeledFor = source.getLabelFor();

        if(source.getChildCount() == 0){
            System.out.println("ID HIJO: "+source.getViewIdResourceName()+" TEXT "+ source.getText());
        }
        else{
            for(int i = 0; i < source.getChildCount(); i++){
                source.refresh();
                source.performAction(AccessibilityNodeInfo.ACTION_SELECT);
                AccessibilityNodeInfo hijo = source.getChild(i);
                readAllNodes(hijo);
                hijo.recycle();
            }
        }
    }

    private AccessibilityNodeInfo findNodeId(String id, AccessibilityNodeInfo source){

        AccessibilityNodeInfo ans = null;
        if(source.getViewIdResourceName() != null && source.getViewIdResourceName().equals(id)){
            ans = source;
            return ans;
        }
        else{
            if(source.getChildCount() > 0){
                for(int i = 0; i < source.getChildCount(); i++){
                    if(findNodeId(id, source.getChild(i)) !=null){
                        return findNodeId(id, source.getChild(i));
                    }
                }
            }
        }

        return ans;
    }

    private AccessibilityNodeInfo findNodeText(String text, AccessibilityNodeInfo source){

        AccessibilityNodeInfo ans = null;
        if(source.getText() != null && source.getText().equals(text)){
            ans = source;
            return ans;
        }
        else{
            if(source.getChildCount() > 0){
                //source.
                for(int i = 0; i < source.getChildCount(); i++){
                    if(findNodeText(text, source.getChild(i)) !=null){
                        return findNodeText(text, source.getChild(i));
                    }
                }
            }
        }

        return ans;
    }


    private void writeEventDevice(String eventId, String eventType){

        Map<String, String> event = new HashMap<>();
        event.put(eventId, eventType);

        db.collection(device).document(executionTime)
                .set(event, SetOptions.merge());
    }

    private void readEvent(){

        Query first = db.collection(device)
                .limit(1);

        first.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {

                        // Get the last visible document
                        DocumentSnapshot lastVisible = documentSnapshots.getDocuments()
                                .get(documentSnapshots.size() -1);

                        ArrayList<String> sortedKeys =
                                new ArrayList<String>(lastVisible.getData().keySet());

                        Collections.sort(sortedKeys);

                        for(int i = 0; i < sortedKeys.size(); i++){

                            DocumentReference docRef = db.collection(events).document(sortedKeys.get(i));

                            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {

                                            texts.add(document.getId()+"//"+document.get(labels[3]));
                                            ids.add(document.getId()+"//"+document.get(labels[2]));
                                        } else {
                                            Log.d("NO DOCUMENT", "No such document");
                                        }
                                    } else {
                                        Log.d("FAILURE READING", "get failed with ", task.getException());
                                    }
                                }
                            });
                        }
                    }
                });
    }
}
