package com.lanabeji.opia.AppDetail;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lanabeji.opia.Main.MainActivity;
import com.lanabeji.opia.Service.OpiaAccessibility;
import com.lanabeji.opia.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;
import static com.lanabeji.opia.AppList.AppListAdapter.RECORDING;
import static com.lanabeji.opia.Service.OpiaAccessibility.events;
import static com.lanabeji.opia.Service.OpiaAccessibility.labels;

/**
 * Created by lanabeji on 17/04/19.
 * Recycler view adapter to show previous records
 */

public class ExecutionListAdapter extends RecyclerView.Adapter<ExecutionListAdapter.ViewHolder>{

    //executions shown on the list
    private List<String> mExecutions;

    //storage
    private FirebaseFirestore db;
    private SharedPreferences sharedPref;

    //general info
    private String device;
    private ArrayList<String> seqEvents;
    private Context context;
    private String packageSelected;
    private String tables;

    public ExecutionListAdapter(List<String> executions) {
        mExecutions = executions;
    }

    @Override
    public ExecutionListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        sharedPref = context.getSharedPreferences(MainActivity.APP, MODE_PRIVATE);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.execution_item, parent, false);

        db = FirebaseFirestore.getInstance();

        device = MainActivity.DEVICE;
        seqEvents = new ArrayList<>();
        packageSelected = sharedPref.getString(MainActivity.PACKAGE, "com.whatsapp");
        tables = sharedPref.getString(packageSelected+AppDetailActivity.TABLES, "[]");

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ExecutionListAdapter.ViewHolder viewHolder, int position) {
        final String current = mExecutions.get(position);

        //indicates that the replay it is not an injection event
        OpiaAccessibility.changeInjection("");

        TextView textView = viewHolder.executionName;

        long stamp = Long.parseLong(current);
        Date result = new Date(stamp);
        textView.setText(result.toString());

        viewHolder.replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replay(current);
            }
        });

        viewHolder.injectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(sharedPref.contains(packageSelected+ AppDetailActivity.TABLES)){
                    inject(current);
                }
            }
        });
    }

    // METHODS

    /*
    Replays a previous sequence of events
    */
    public void replay(String current){
        //get the sequence of events
        readEvent(current);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //save the package selected but indicates accessibility api that a replay event will happen, so it should not record
        writeSelected(packageSelected, false);

        //write the sequence of events related with the selected timestamp
        OpiaAccessibility.replaceSeqEvents(seqEvents);

        //it executes the replay only one time
        OpiaAccessibility.changeOneTime();

        //start the app
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageSelected);
        if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launchIntent);
        }
    }

    /*
    Generates a list of strings to inject and executes a previous sequence of events replacing text inputs with strings to inject
    */
    public void inject(String current){

        //generate a list of strings to inject
        String[] listTables = tables.replace("[","").replace("]","").replace("\"","").split(", ");
        ArrayList<String> injectionStrings = new ArrayList<>();

        injectionStrings.add("' OR '1'='1';--");
        injectionStrings.add("0 OR '1'='1;--");
        injectionStrings.add(";");

        String currentTable = "";
        for(int i = 0; i < listTables.length; i++){
            currentTable = listTables[i];
            if(!currentTable.equals("android_metadata") && !currentTable.equals("room_master_table")){
                injectionStrings.add("'; DROP TABLE "+ currentTable+";--");
                injectionStrings.add("0; DROP TABLE "+ currentTable+";--");
            }
        }

        //get the sequence of events
        readEvent(current);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //save the package selected but indicates accessibility api that a replay event will happen, so it should not record
        writeSelected(packageSelected, false);

        //write the sequence of events related with the selected timestamp
        OpiaAccessibility.replaceSeqEvents(seqEvents);

        //it executes the replay only one time
        OpiaAccessibility.changeOneTime();

        //indicates the replay should do injection
        OpiaAccessibility.changeInjection("injection");

        //write the strings to inject
        OpiaAccessibility.changeInjectionStrings(injectionStrings);

        //start the app
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageSelected);
        if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launchIntent);
        }
    }

    /*
    Writes on shared preferences which app to test
    */
    public void writeSelected(String packageSelected, boolean isRecording){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(MainActivity.PACKAGE, packageSelected);
        editor.putBoolean(RECORDING, isRecording);
        editor.commit();
    }

    /*
    Gets from firebase the events of the execution with the id(timestamp)
    */
    private void readEvent(String id){

        DocumentReference docRef = db.collection(device).document(id);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        ArrayList<String> sortedKeys =
                                new ArrayList<>(document.getData().keySet());

                        Collections.sort(sortedKeys);

                        for(int i = 0; i < sortedKeys.size(); i++){

                            DocumentReference docRef = db.collection(events).document(sortedKeys.get(i));

                            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (document.exists()) {

                                            //classname, elementid, text, bounds,eventtype, code
                                            String event = document.getId()+"//"+document.get(labels[1])+"//"+document.get(labels[2])+"//"+document.get(labels[3])+"//"+document.get(labels[4])+"//"+document.get(labels[10])+"//"+document.get("code");
                                            seqEvents.add(event);

                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mExecutions.size();
    }

    /*
    Updates the execution list
    */
    public void updateList(List<String> data) {
        mExecutions = data;
        notifyDataSetChanged();
    }

    /*
    View holder to manage the content of each row in the recycler view
    */
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView executionName;
        public Button replayButton;
        public Button injectionButton;

        public ViewHolder(View itemView) {
            super(itemView);

            executionName = itemView.findViewById(R.id.executionTime);
            replayButton = itemView.findViewById(R.id.playButton);
            injectionButton = itemView.findViewById(R.id.injectionButton);

            if(tables.equals("[]")){
                injectionButton.setVisibility(View.INVISIBLE);
            }else{
                injectionButton.setVisibility(View.VISIBLE);
            }
        }
    }
}
