package com.lanabeji.opia;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppDetailActivity extends AppCompatActivity {


    public static final String APP_NAME = "appName";
    public static final String APP_IMAGE = "appImage";
    public static final String APP_PACKAGE = "appPackage";
    public static final String EXEC_LIST = "executionList";
    ImageView appImage;
    TextView appName;
    FirebaseFirestore db;
    String device;
    SharedPreferences preferences;
    String packageSelected;
    ArrayList<String> executions;
    RecyclerView rvExecutions;
    ExecutionListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        appName = findViewById(R.id.appName);
        appImage = findViewById(R.id.appImage);

        Bundle extras = getIntent().getExtras();
        packageSelected = extras.getString(APP_PACKAGE);
        String name = extras.getString(APP_NAME);
        byte[] b = extras.getByteArray(APP_IMAGE);
        Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);

        appName.setText(name);
        appImage.setImageBitmap(bmp);

        db = FirebaseFirestore.getInstance();
        device = MainActivity.DEVICE;
        preferences = getSharedPreferences(MainActivity.APP, MODE_PRIVATE);

        rvExecutions = (RecyclerView) findViewById(R.id.rvExecutions);
        HashSet<String> pruebas = new HashSet<String>();
        executions = new ArrayList<>(preferences.getStringSet(EXEC_LIST, pruebas));
        adapter = new ExecutionListAdapter(executions);
        rvExecutions.setAdapter(adapter);
        rvExecutions.setLayoutManager(new LinearLayoutManager(this));

    }

    public void record(View v)
    {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageSelected);
        if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadExecutions().execute();
    }


    private class LoadExecutions extends AsyncTask<String,Void,Void> {

        SharedPreferences.Editor editor = preferences.edit();

        @Override
        protected Void doInBackground(String... strings) {

            db.collection(device).whereEqualTo("packageName", packageSelected)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Set<String> execs = preferences.getStringSet(EXEC_LIST, new HashSet<String>());

                                if(task.getResult().isEmpty()){
                                    adapter.updateList(new ArrayList<String>());
                                }
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d("READ EXECUTION", document.getId());

                                    execs.add(document.getId());

                                    adapter.updateList(new ArrayList<String>(execs));
                                    editor.putStringSet(EXEC_LIST,execs);
                                    editor.commit();
                                }
                            } else {
                                Log.d("NOT READ EXECUTION", "Error getting documents: ", task.getException());
                            }
                        }
                    });
            return null;
        }

    }

}
