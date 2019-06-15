package com.lanabeji.opia.Service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.AsyncTask;
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
import com.lanabeji.opia.AppList.AppListAdapter;
import com.lanabeji.opia.AppList.ListActivity;
import com.lanabeji.opia.Main.MainActivity;
import com.lanabeji.opia.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/*
Accessibility service to record, replay and inject
*/
public class OpiaAccessibility extends AccessibilityService {

    //Storage
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //Constants
    public static String events = "events";
    public static String injectionEvents = "injectionEvents";
    //Names of the fields of the events
    public static String[] labels = new String[]{"packageName", "className", "elementId", "text", "bounds",
            "childCount", "contentDescription", "isClickable", "deviceId", "executionTime", "eventType", "eventTime"};

    //General info
    private String executionTime = String.valueOf(String.valueOf(System.currentTimeMillis()));
    private String device = MainActivity.DEVICE;
    boolean isInsideApp = false;
    private String packageSelected = "";
    private Random rand = new Random();

    //Variables to manage replay and injection
    static boolean oneTime = false;
    static String injection = "";
    static int counterInjection = 1;
    static ArrayList<String> injectionStrings = new ArrayList<>();
    public static ArrayList<String> seqEvents = new ArrayList<>();
    public static String execution = "";

    //UI Elements
    private FrameLayout mLayout;
    private Button powerButton;

    //Variables to get the answer from the server
    boolean logBool = false;
    String logAnswer = "Empty";

    public OpiaAccessibility() {
    }

    // LIFECYCLE

    @Override
    protected void onServiceConnected() {

        //setup the accessibility service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        //adds all the apps installed on the phone, because all of them can be observed
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

        //adds a button (the eye) to stop the record
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
                String timestampEvent = String.valueOf(System.currentTimeMillis());

                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                    handleEvent(event.getSource(), timestampEvent, "text");
                }
                else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED ){
                    event.getSource().refresh();
                    handleEvent(event.getSource(), timestampEvent, "click");
                }
                else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED){
                    handleEvent(event.getSource(), timestampEvent, "scroll");
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

    /*
    Lists all the apps installed on the phone except android system apps and Opia itself. All the apps
    can be observed and recorded.
    */
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

    /*
    Setups the stop button
    */
    private void configureStopButton(){

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

        powerButton = mLayout.findViewById(R.id.stop);
        powerButton.setVisibility(View.GONE);
        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                writeEventDevice("packageName", packageSelected);
                executionTime = "EMPTY";
                powerButton.setVisibility(View.GONE);
                Intent dialogIntent = new Intent(getBaseContext(), ListActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
            }
        });
    }

    // EVENT'S METHODS

    /*
    Reads an event, the node(ui element) touched, changed or scrolled and the time to save
    that information as an event in the sequence of events
    */
    private void handleEvent(AccessibilityNodeInfo source, String timestampEvent, String eventType){

        String packageName = String.valueOf(source.getPackageName());
        String className = String.valueOf(source.getClassName());
        String elementId = String.valueOf(source.getViewIdResourceName());
        String text = "";

        if(eventType.equals("text")){ //verifies if the node is a text field because a password field cannot be read
            if (!source.isPassword()) {
                if(source.getText() != null){
                    text = String.valueOf(source.getText());
                }
            }
            else{
                text = "isPassword";
            }
        }
        else{
            text = String.valueOf(source.getText());
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

        String[] values = new String[]{packageName, className, elementId, text, bounds, childCount, contentDescription, isClickable, device, executionTime, eventType};

        //write the event in a list of events
        writeEvent(values, timestampEvent);
        //write the event on sequence of events
        writeEventDevice(timestampEvent, eventType);

        String urlServer = getSharedPreferences(MainActivity.APP, MODE_PRIVATE).getString(MainActivity.SERVER, "http://localhost:5000");
        new ADBCommand().execute(urlServer+"/log/"+MainActivity.DEVICE+"/"+executionTime+"/"+packageSelected);
    }

    /*
    Reads a keyevent a volume up, volume down, recents, back and home
    */
    private void handleKeyEvent(int code, String timestampEvent){

        Map<String, String> newEvent = new HashMap<>();

        newEvent.put("code", String.valueOf(code));
        newEvent.put("eventType", "keyevent");

        // Add a new document with timestamp as id
        db.collection(events).document(timestampEvent).set(newEvent).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });

        writeEventDevice(timestampEvent, "keyevent");
    }

    //METHODS TO HANDLE REPLAYS

    /*
    Write the current sequence of events to execute, depending of the selection on app detail
    */
    public static void replaceSeqEvents(ArrayList<String> newSeq, String exec){
        seqEvents = newSeq;
        execution = exec;
    }

    /*
    Indicates that it should not repeat infinitely the sequence of events
    */
    public static void changeOneTime(){
        oneTime = false;
    }

    /*
    Starts again the counter of injection events
    */
    public static void changeInjection(String inj){injection = inj; counterInjection = 3;}

    /*
    Writes the list of string to inject
    */
    public static void changeInjectionStrings(ArrayList<String> strings){
        injectionStrings = strings;
    }

    /*
    Replay the sequence of events depending if it is injection or replay
    */
    private void replayEvents(){

        oneTime = true;
        Collections.sort(seqEvents);
        String urlServer = getSharedPreferences(MainActivity.APP, MODE_PRIVATE).getString(MainActivity.SERVER, "http://localhost:5000");

        if(injection.equals("")){ //it is simply replay
            replaySeq();
        }
        else{
            while(counterInjection > 0){

                if(counterInjection != 3){

                    if(logAnswer.equals("OK")){ //if the app didn't crash
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageSelected);
                        if (launchIntent != null) {
                            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                            startActivity(launchIntent);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    logAnswer = "Empty";
                    logBool = false;
                }
                replaySeq();
                counterInjection--;
            }

            //open Opia after 3 replays/injections
            injection = "";
            Intent dialogIntent = new Intent(getBaseContext(), ListActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
        }

    }

    public void replaySeq(){

        String classname = "";
        String text = "";
        String bounds = "";
        String eventType = "";
        String code = "";
        AccessibilityNodeInfo found;
        String urlServer = getSharedPreferences(MainActivity.APP, MODE_PRIVATE).getString(MainActivity.SERVER, "http://localhost:5000");

        for (int i = 0; i < seqEvents.size(); i++) {

            //id, classname, elementid, text, bounds, eventtype, code
            String[] event = seqEvents.get(i).split("//");
            classname = event[1];
            text = event[3];
            bounds = event[4];
            eventType = event[5];
            code = event[6];

            switch (eventType) {

                case "click":

                    found = findNode(text, bounds, classname, getRootInActiveWindow());

                    if (found != null) {
                        found.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case "scroll":

                    found = findNodeScroll(classname, getRootInActiveWindow());

                    if (found != null) {
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

                    if (found != null) {

                        if (!injection.equals("")) {

                            int n = rand.nextInt(injectionStrings.size());
                            text = injectionStrings.get(n);
                        }

                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo
                                .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);

                        found.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                        try {
                            Thread.sleep(700);
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

            new ADBCommand().execute(urlServer+"/log/"+MainActivity.DEVICE+"/"+execution+"/"+packageSelected);
            while(!logBool){
                //Espere a que llegue la respuesta
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        new ADBCommand().execute(urlServer+"/clear/");
        if(injection.equals("")){ //es solo replay, abra de una vez opia
            Intent dialogIntent = new Intent(getBaseContext(), ListActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
        }
        else{
            try { //dele tiempito mientras abre la app
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //METHODS TO FIND NODES

    /*
    Finds a node with a combination of text, bounds and classname
    */
    private AccessibilityNodeInfo findNode(String text, String bounds, String classname, AccessibilityNodeInfo source){

        AccessibilityNodeInfo ans = null;
        if(source != null) {
            source.refresh();

            boolean srcTxt = String.valueOf(source.getText()).equals(text);

            Rect outBoundsParent = new Rect();
            source.getBoundsInParent(outBoundsParent);
            String boundsInParent = outBoundsParent.toString();

            Rect outBoundsScreen = new Rect();
            source.getBoundsInScreen(outBoundsScreen);
            String boundsInScreen = outBoundsScreen.toString();

            String boundsCurrent = boundsInParent + "!!" + boundsInScreen;

            boolean srcBounds = boundsCurrent.equals(bounds);
            boolean srcClass = String.valueOf(source.getClassName()).equals(classname);

            if (srcTxt && srcBounds && srcClass) {
                ans = source;
                return ans;
            } else {
                if (source.getChildCount() > 0) {
                    AccessibilityNodeInfo child;
                    AccessibilityNodeInfo newChild;
                    for (int i = 0; i < source.getChildCount(); i++) {
                        child = source.getChild(i);
                        newChild = findNode(text, bounds, classname, child);
                        if (newChild != null) {
                            return newChild;
                        }
                        if(child != null){
                            child.recycle();
                        }
                    }
                }
            }
        }

        return ans;
    }

    /*
    If the event was a text event, it searches the node only with bounds and classname
    */
    private AccessibilityNodeInfo findNode(String bounds, String classname, AccessibilityNodeInfo source){

        AccessibilityNodeInfo ans = null;
        if(source != null) {

            Rect outBoundsParent = new Rect();
            source.getBoundsInParent(outBoundsParent);
            String boundsInParent = outBoundsParent.toString();

            Rect outBoundsScreen = new Rect();
            source.getBoundsInScreen(outBoundsScreen);
            String boundsInScreen = outBoundsScreen.toString();

            String boundsCurrent = boundsInParent + "!!" + boundsInScreen;

            boolean srcBounds = boundsCurrent.equals(bounds);
            boolean srcClass = String.valueOf(source.getClassName()).equals(classname);

            if (srcBounds && srcClass) {
                ans = source;
                return ans;
            } else {
                if (source.getChildCount() > 0) {
                    AccessibilityNodeInfo child;
                    AccessibilityNodeInfo newChild;
                    for (int i = 0; i < source.getChildCount(); i++) {
                        child = source.getChild(i);
                        newChild = findNode(bounds, classname, child);
                        if ( newChild != null) {
                            return newChild;
                        }
                        if(child != null){
                            child.recycle();
                        }
                    }
                }
            }
        }

        return ans;
    }

    /*
    Searches the node only with classname, useful for scroll events
    */
    private AccessibilityNodeInfo findNodeScroll(String classname, AccessibilityNodeInfo source){

        AccessibilityNodeInfo ans = null;

        if(source != null) {

            Rect b = new Rect();
            source.getBoundsInParent(b);
            boolean srcClass = String.valueOf(source.getClassName()).equals(classname);

            if (srcClass) {
                ans = source;
                return ans;
            } else {
                if (source.getChildCount() > 0) {
                    AccessibilityNodeInfo child;
                    AccessibilityNodeInfo newChild;
                    for (int i = 0; i < source.getChildCount(); i++) {
                        child = source.getChild(i);
                        newChild = findNodeScroll(classname, child);
                        if (newChild != null) {
                            return newChild;
                        }
                        if(child != null){
                            child.recycle();
                        }
                    }
                }
            }
        }

        return ans;
    }

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

    // FIREBASE METHODS

    /*
    Writes a new event in firebase with all the fields specified before in the constant 'labels'
    */
    private void writeEvent(String[] values, String timestampEvent){

        Map<String, String> newEvent = new HashMap<>();

        for(int i = 0; i < values.length; i++){
            newEvent.put(labels[i], values[i]);
        }

        // Add a new document with timestamp as id
        db.collection(events).document(timestampEvent).set(newEvent).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }

    /*
    Search on firebase the document related with the device and adds a new attribute with the id of the event and the type of event
    */
    private void writeEventDevice(String eventId, String eventType){

        Map<String, String> event = new HashMap<>();
        event.put(eventId, eventType);

        db.collection(device).document(executionTime)
                .set(event, SetOptions.merge());
    }

    // ASYNC TASKS

    /*
    Async task that indicates the server to execute adb commands
    */
    public class ADBCommand extends AsyncTask<String , Void ,String> {
        String server_response;

        @Override
        protected String doInBackground(String... strings) {

            URL url;
            HttpURLConnection urlConnection = null;

            try {

                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                int responseCode = urlConnection.getResponseCode();

                if(strings[0].contains("clear")){
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        server_response = readStream(urlConnection.getInputStream());
                    }
                }
                else{
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        server_response = readStream(urlConnection.getInputStream());
                        logAnswer = server_response;
                        logBool = true;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }

}
