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

    private List<String> mExecutions;
    private FirebaseFirestore db;
    private String device;
    private ArrayList<String> seqEvents;
    private Context context;
    private SharedPreferences sharedPref;
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
        OpiaAccessibility.changeInjection("");

        TextView textView = viewHolder.executionName;

        long stamp = Long.parseLong(current);
        Date result = new Date(stamp);
        textView.setText(result.toString());

        viewHolder.replayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                readEvent(current);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                writeSelected(packageSelected, false);
                OpiaAccessibility.replaceSeqEvents(seqEvents);
                OpiaAccessibility.changeOneTime();
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageSelected);
                if (launchIntent != null) {
                    launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(launchIntent);
                }
            }
        });

        viewHolder.injectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(sharedPref.contains(packageSelected+ AppDetailActivity.TABLES)){

                    String[] listTables = tables.replace("[","").replace("]","").replace("\"","").split(", ");
                    ArrayList<String> injectionStrings = new ArrayList<>();

                    injectionStrings.add("' OR '1'='1;--");
                    injectionStrings.add("0 OR '1'='1;--");
                    injectionStrings.add(";");

                    for(int i = 0; i < listTables.length; i++){
                        injectionStrings.add("'; DROP TABLE "+ listTables[i]+";--");
                        injectionStrings.add("0; DROP TABLE "+ listTables[i]+";--");
                    }

                    readEvent(current);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    writeSelected(packageSelected, false);
                    OpiaAccessibility.replaceSeqEvents(seqEvents);
                    OpiaAccessibility.changeOneTime();
                    OpiaAccessibility.changeInjection("injection");
                    OpiaAccessibility.changeInjectionStrings(injectionStrings);
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageSelected);
                    if (launchIntent != null) {
                        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        context.startActivity(launchIntent);
                    }

                }
            }
        });
    }

    public void writeSelected(String packageSelected, boolean isRecording){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(MainActivity.PACKAGE, packageSelected);
        editor.putBoolean(RECORDING, isRecording);
        editor.commit();
    }

    private void readEvent(String id){

        DocumentReference docRef = db.collection(device).document(id);

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        ArrayList<String> sortedKeys =
                                new ArrayList<String>(document.getData().keySet());

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

    public void updateList(List<String> data) {
        mExecutions = data;
        notifyDataSetChanged();
    }

    public void updateListInjection() {
        notifyDataSetChanged();
        
    }


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
