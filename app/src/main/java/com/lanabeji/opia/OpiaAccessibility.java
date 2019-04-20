package com.lanabeji.opia;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
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
import com.google.firebase.firestore.SetOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpiaAccessibility extends AccessibilityService {

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    public static String events = "events";
    public static String injectionEvents = "injectionEvents";
    public static String[] labels = new String[]{"packageName", "className", "elementId", "text", "bounds", "childCount", "contentDescription", "isClickable", "deviceId", "executionTime", "eventType", "eventTime"};
    private String executionTime = String.valueOf(String.valueOf(System.currentTimeMillis()));
    String device = MainActivity.DEVICE;
    boolean isInsideApp = false;
    static boolean oneTime = false;
    static String injection = "";
    String packageSelected = "";

    FrameLayout mLayout;
    Button powerButton;
    public static ArrayList<String> seqEvents = new ArrayList<>();

    public OpiaAccessibility() {
    }

    // LIFECYCLE

    @Override
    protected void onServiceConnected() {
        Log.d("ACCESSIBILITY", "ON SERVICE");

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.packageNames = addPackages();

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_VIEW_CLICKED
                | AccessibilityEvent.TYPE_VIEW_SCROLLED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
        info.notificationTimeout = 1000;

        info.flags = AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME
                | AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;

        setServiceInfo(info);

        configureStopButton();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        SharedPreferences preferences = getSharedPreferences(MainActivity.APP, MODE_PRIVATE);
        packageSelected = preferences.getString(MainActivity.PACKAGE, "com.whatsapp");
        boolean isRecording = preferences.getBoolean(AppListAdapter.RECORDING, true);

        isInsideApp = false;

        if (String.valueOf(event.getPackageName()).equals(packageSelected)){

            isInsideApp = true;

            if(executionTime.equals("EMPTY")){
                executionTime = String.valueOf(String.valueOf(System.currentTimeMillis()));
            }

            if(isRecording){
                powerButton.setVisibility(View.VISIBLE);
                Log.d("ON EVENT", String.valueOf(event.getEventType()));

                String timestampEvent = String.valueOf(System.currentTimeMillis());

                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                    handleTextEvent(event.getSource(), timestampEvent);
                }
                else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED ){
                    event.getSource().refresh();
                    handleClickEvent(event.getSource(), timestampEvent);
                }
                else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED){
                    //hacer solo el scroll forward, encontrar el nodo
                    handleScrollEvent(event.getSource(), timestampEvent);
                }
            }
            else { // Replaying

                if(!oneTime){
                    replayEvents();
                }
            }
        }
    }


    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        if(isInsideApp && event.getAction() == KeyEvent.ACTION_DOWN){
            String timestampEvent = String.valueOf(System.currentTimeMillis());
            int code = event.getKeyCode();

            if(code == 3 || code == 4 || code == 3 || code == 24 || code == 25 || code == 284){
                handleKeyEvent(code, timestampEvent);
            }
        }

        return super.onKeyEvent(event);
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

                writeEventDevice("packageName", packageSelected);
                executionTime = "EMPTY";
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
        String elementId = String.valueOf(source.getViewIdResourceName());

        String text = "";
        if (!source.isPassword()) {

            if(source.getText() != null){
                text = source.getText().toString();
            }
        }

        Rect outBoundsParent = new Rect();
        source.getBoundsInParent(outBoundsParent);
        String boundsInParent = outBoundsParent.toString();

        Rect outBoundsScreen = new Rect();
        source.getBoundsInScreen(outBoundsScreen);
        String boundsInScreen = outBoundsScreen.toString();

        String bounds = boundsInParent + "!!"+ boundsInScreen;

        String childCount = String.valueOf(source.getChildCount());
        String contentDescription = String.valueOf(source.getContentDescription());
        String isClickable = String.valueOf(source.isClickable());

        String eventType = "text";
        String[] values = new String[]{packageName, className, elementId, text, bounds, childCount, contentDescription, isClickable, device, executionTime, eventType};

        writeEvent(values, timestampEvent);
        writeEventDevice(timestampEvent, eventType);
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
            String elementId = String.valueOf(source.getViewIdResourceName());
            String text = String.valueOf(source.getText());

            Rect outBoundsParent = new Rect();
            source.getBoundsInParent(outBoundsParent);
            String boundsInParent = outBoundsParent.toString();

            Rect outBoundsScreen = new Rect();
            source.getBoundsInScreen(outBoundsScreen);
            String boundsInScreen = outBoundsScreen.toString();

            String bounds = boundsInParent + "!!"+ boundsInScreen;

            String childCount = String.valueOf(source.getChildCount());
            String contentDescription = String.valueOf(source.getContentDescription());
            String isClickable = String.valueOf(source.isClickable());

            String eventType = "window";
            String eventTime = timestampWindow;
            String[] values = new String[]{packageName, className, elementId, text, bounds, childCount, contentDescription, isClickable, device, executionTime, eventType, eventTime};

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

        Rect outBoundsParent = new Rect();
        source.getBoundsInParent(outBoundsParent);
        String boundsInParent = outBoundsParent.toString();

        Rect outBoundsScreen = new Rect();
        source.getBoundsInScreen(outBoundsScreen);
        String boundsInScreen = outBoundsScreen.toString();

        String bounds = boundsInParent + "!!"+ boundsInScreen;

        String timestampEvent = String.valueOf(System.currentTimeMillis());

        String packageName = String.valueOf(source.getPackageName());
        String className = String.valueOf(source.getClassName());
        String elementId = String.valueOf(source.getViewIdResourceName());
        String text = String.valueOf(source.getText());

        String childCount = String.valueOf(source.getChildCount());
        String contentDescription = String.valueOf(source.getContentDescription());
        String isClickable = String.valueOf(source.isClickable());

        String eventType = "click";
        String eventTime = timestampClick;
        String[] values = new String[]{packageName, className, elementId, text, bounds, childCount, contentDescription, isClickable, device, executionTime, eventType, eventTime};

        writeEvent(values, timestampEvent);
        writeEventDevice(timestampEvent, eventType);
    }


    private void handleScrollEvent(AccessibilityNodeInfo source, String timestampEvent){

        String packageName = String.valueOf(source.getPackageName());
        String className = String.valueOf(source.getClassName());
        String elementId = String.valueOf(source.getViewIdResourceName());
        String text = String.valueOf(source.getText());

        Rect outBoundsParent = new Rect();
        source.getBoundsInParent(outBoundsParent);
        String boundsInParent = outBoundsParent.toString();

        Rect outBoundsScreen = new Rect();
        source.getBoundsInScreen(outBoundsScreen);
        String boundsInScreen = outBoundsScreen.toString();

        String bounds = boundsInParent + "!!"+ boundsInScreen;

        String childCount = String.valueOf(source.getChildCount());
        String contentDescription = String.valueOf(source.getContentDescription());
        String isClickable = String.valueOf(source.isClickable());

        String eventType = "scroll";
        String[] values = new String[]{packageName, className, elementId, text, bounds, childCount, contentDescription, isClickable, device, executionTime, eventType};


        writeEvent(values, timestampEvent);
        writeEventDevice(timestampEvent, eventType);
    }

    private void handleKeyEvent(int code, String timestampEvent){

        Map<String, String> newEvent = new HashMap<>();

        newEvent.put("code", String.valueOf(code));
        newEvent.put("eventType", "keyevent");

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

        writeEventDevice(timestampEvent, "keyevent");
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


    private AccessibilityNodeInfo findNode(String text, String bounds, String classname, AccessibilityNodeInfo source){

        source.refresh();
        AccessibilityNodeInfo ans = null;
        boolean srcTxt = String.valueOf(source.getText()).equals(text);

        Rect outBoundsParent = new Rect();
        source.getBoundsInParent(outBoundsParent);
        String boundsInParent = outBoundsParent.toString();

        Rect outBoundsScreen = new Rect();
        source.getBoundsInScreen(outBoundsScreen);
        String boundsInScreen = outBoundsScreen.toString();

        String boundsCurrent = boundsInParent + "!!"+ boundsInScreen;

        boolean srcBounds = boundsCurrent.equals(bounds);
        boolean srcClass = String.valueOf(source.getClassName()).equals(classname);

        if(srcTxt && srcBounds && srcClass){
            ans = source;
            return ans;
        }
        else{
            if(source.getChildCount() > 0){
                for(int i = 0; i < source.getChildCount(); i++){
                    AccessibilityNodeInfo child = source.getChild(i);
                    if(findNode(text, bounds, classname, child) !=null){
                        return findNode(text, bounds, classname, source.getChild(i));
                    }
                    child.recycle();
                }
            }
        }

        return ans;
    }

    private AccessibilityNodeInfo findNode(String bounds, String classname, AccessibilityNodeInfo source){

        AccessibilityNodeInfo ans = null;

        Rect outBoundsParent = new Rect();
        source.getBoundsInParent(outBoundsParent);
        String boundsInParent = outBoundsParent.toString();

        Rect outBoundsScreen = new Rect();
        source.getBoundsInScreen(outBoundsScreen);
        String boundsInScreen = outBoundsScreen.toString();

        String boundsCurrent = boundsInParent + "!!"+ boundsInScreen;

        boolean srcBounds = boundsCurrent.equals(bounds);
        boolean srcClass = String.valueOf(source.getClassName()).equals(classname);

        if(srcBounds && srcClass){
            ans = source;
            return ans;
        }
        else{
            if(source.getChildCount() > 0){
                for(int i = 0; i < source.getChildCount(); i++){
                    AccessibilityNodeInfo child = source.getChild(i);
                    if(findNode(bounds, classname, source.getChild(i)) !=null){
                        return findNode(bounds, classname, source.getChild(i));
                    }
                    child.recycle();
                }
            }
        }

        return ans;
    }

    private AccessibilityNodeInfo findNodeScroll(String classname, AccessibilityNodeInfo source){

        AccessibilityNodeInfo ans = null;

        Rect b = new Rect();
        source.getBoundsInParent(b);
        boolean srcClass = String.valueOf(source.getClassName()).equals(classname);

        if(srcClass){
            ans = source;
            return ans;
        }
        else{
            if(source.getChildCount() > 0){
                for(int i = 0; i < source.getChildCount(); i++){
                    AccessibilityNodeInfo child = source.getChild(i);
                    if(findNodeScroll(classname, source.getChild(i)) !=null){
                        return findNodeScroll(classname, source.getChild(i));
                    }
                    child.recycle();
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

    private void writeInjectionEvent(String eventId, String log, String nodeInfo){

        Map<String, String> event = new HashMap<>();
        event.put("log", log);
        event.put("nodeInfo", nodeInfo);

        db.collection(injectionEvents).document(eventId).set(event).addOnSuccessListener(new OnSuccessListener<Void>() {
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

    public static void replaceSeqEvents(ArrayList<String> newSeq){
        seqEvents = newSeq;
    }

    private void replayEvents(){

        oneTime = true;
        Collections.sort(seqEvents);

        String classname = "";
        String id = "";
        String text = "";
        String bounds = "";
        String eventType = "";
        String code = "";

        AccessibilityNodeInfo found;

        for(int i = 0; i < seqEvents.size(); i++){

            //id, classname, elementid, text, bounds, eventtype, code
            String[] event = seqEvents.get(i).split("//");
            classname = event[1];
            id = event[2];
            text = event[3];
            bounds = event[4];
            eventType = event[5];
            code = event[6];

            switch (eventType){

                case "click":

                    System.out.println("CLICK");

                    found = findNode(text, bounds, classname, getRootInActiveWindow());

                    if(found != null){
                        found.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "scroll":

                    System.out.println("SCROLL");

                    found = findNodeScroll(classname, getRootInActiveWindow());

                    if(found != null){
                        found.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "text":

                    found = findNode(bounds, classname, getRootInActiveWindow());

                    if(found != null){

                        if(!injection.equals("")){
                            text = "' OR '1'='1";
                            //cleans the log, so you can see the real change after writing
                            //clearLog();
                        }

                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo
                                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                        found.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                        try {
                            Thread.sleep(700);

                            if(!injection.equals("")){
                                String eventId = String.valueOf(System.currentTimeMillis());
                                String log = getLog();
                                String nodeInfo = seqEvents.get(i);

                                writeInjectionEvent(eventId, log, nodeInfo);
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    break;
                case "keyevent":
                    int codeEvent = Integer.parseInt(code);
                    executeKeyevent(codeEvent);
                    try {
                        Thread.sleep(700);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }

        Intent dialogIntent = new Intent(getBaseContext(), ListActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }

    public static void changeOneTime(){
        oneTime = false;
    }
    public static void changeInjection(String inj){injection = inj;}

    private void executeKeyevent(int code){
        switch (code){
            case 3: //Home
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case 4: //Back
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case 24: //Volume up

                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                break;
            case 25: //Volume down

                AudioManager audioManagerDown = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManagerDown.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                break;
            case 284: //Recents
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
            default:
                break;
        }
    }

    public void clearLog(){
        try {
            Process process = Runtime.getRuntime().exec("logcat -c");
        }
        catch (IOException e) {}
    }

    public String getLog(){

        String ans = "";
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            StringBuilder log=new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
            }
            ans = log.toString();
        }
        catch (IOException e) {}

        return ans;
    }
}
