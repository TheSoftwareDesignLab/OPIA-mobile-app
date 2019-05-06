package com.lanabeji.opia;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
    public static final String TABLES = "-tables";
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

    public void testIntegrity(View v){

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("IP Address");
        alert.setMessage("Enter the IP address where the server is running");
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                System.out.println(value);
                String serverUrl = "http://"+value+":5000";

                String url = serverUrl + "/app/" + packageSelected;

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(MainActivity.SERVER, serverUrl);
                editor.commit();
                new GetTables().execute(url);
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadExecutions().execute();
    }

    public class GetTables extends AsyncTask<String , Void ,String> {
        String server_response;
        SharedPreferences.Editor editor = preferences.edit();

        @Override
        protected String doInBackground(String... strings) {

            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();


                int responseCode = urlConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK){
                    server_response = readStream(urlConnection.getInputStream());

                    editor.putString(packageSelected+TABLES,server_response);
                    editor.commit();

                }
                else{
                    Log.d("NO", "LEYOOOOO");
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.e("Response", "" + server_response);


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
