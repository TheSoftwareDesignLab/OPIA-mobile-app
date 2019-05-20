package com.lanabeji.opia.AppDetail;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.lanabeji.opia.Main.MainActivity;
import com.lanabeji.opia.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
* Activity that shows the selected app, with her name, icon and available actions
*/
public class AppDetailActivity extends AppCompatActivity {

    //Names of variables saved in shared preferences and intents
    public static final String APP_NAME = "appName";
    public static final String APP_IMAGE = "appImage";
    public static final String APP_PACKAGE = "appPackage";
    public static final String EXEC_LIST = "executionList";
    public static final String TABLES = "-tables";

    //Regex to validate ip addresses
    public static final String IPv4_REGEX = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

    //UI Elements
    ImageView appImage;
    TextView appName;

    //Storage variables
    FirebaseFirestore db;
    SharedPreferences preferences;

    //General info
    String device;
    String packageSelected;

    //List of previous executions
    ArrayList<String> executions;
    RecyclerView rvExecutions;
    ExecutionListAdapter adapter;

    ActionBar actionBar;

    // LIFE CYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_detail);

        setupAppInfo();

        db = FirebaseFirestore.getInstance();
        device = MainActivity.DEVICE;
        preferences = getSharedPreferences(MainActivity.APP, MODE_PRIVATE);

        setupRecyclerView();
        setupActionBar();

        String currentServer = preferences.getString(MainActivity.IP_SERVER, "EMPTY");
        if(currentServer.equals("EMPTY")){
            showAlert(currentServer);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new LoadExecutions().execute();
    }

    //SETUP METHODS

    private void setupAppInfo(){
        appName = findViewById(R.id.appName);
        appImage = findViewById(R.id.appImage);

        Bundle extras = getIntent().getExtras();
        packageSelected = extras.getString(APP_PACKAGE, "com.lanabeji.notsecure");
        String name = extras.getString(APP_NAME, "Not Secure");
        byte[] b = extras.getByteArray(APP_IMAGE);
        Bitmap bmp = BitmapFactory.decodeByteArray(b, 0, b.length);

        appName.setText(name);
        appImage.setImageBitmap(bmp);
    }

    private void setupRecyclerView(){
        rvExecutions = findViewById(R.id.rvExecutions);
        executions = new ArrayList<>();
        adapter = new ExecutionListAdapter(executions);
        rvExecutions.setAdapter(adapter);
        rvExecutions.setLayoutManager(new LinearLayoutManager(this));

        new LoadExecutions().execute();
    }

    private void setupActionBar(){
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    //METHODS

    /*
    Opens the app and starts a record
    */
    public void record(View v)
    {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageSelected);
        if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(launchIntent);
        }
    }

    /*
    Gets tables names from server
    */
    public void testIntegrity(View v){

        String currentIP = preferences.getString(MainActivity.IP_SERVER, "EMPTY");
        if(currentIP.equals("EMPTY")){
            showAlert(currentIP);
        }
        else{
            String currentServer = preferences.getString(MainActivity.SERVER, "localhost");
            String url = currentServer + "/app/" + packageSelected;
            new GetTables().execute(url);
        }
    }

    /*
    Shows an alert to modify server ip address
    */
    public void showAlert(String current){

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("IP Address");
        alert.setMessage("Enter the IP address where the server is running");
        final EditText input = new EditText(this);

        if(current.equals("EMPTY")){ //if there isn't an ip address saved, shows a hint
            current = "192.168.100.5";
            input.setHint(current);
        }
        else{ //else shows current address
            input.setText(current);
        }

        alert.setView(input);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();

                if(validateIPAddress(value)){
                    String serverUrl = "http://"+value+":5000";
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(MainActivity.IP_SERVER,value);
                    editor.putString(MainActivity.SERVER, serverUrl);
                    editor.commit();
                }
                else{
                    Toast.makeText(AppDetailActivity.this, "Please enter a valid IP Address",Toast.LENGTH_SHORT).show();
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    /*
    Validates if the parameter is a valid ip address with a regular expression
    */
    public boolean validateIPAddress(String ip){
        Pattern IPv4_PATTERN = Pattern.compile(IPv4_REGEX);

        if (ip == null) {
            return false;
        }

        Matcher matcher = IPv4_PATTERN.matcher(ip);
        return matcher.matches();
    }

    // ACTION BAR METHODS

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_http) {
            String currentServer = preferences.getString(MainActivity.IP_SERVER, "EMPTY");
            System.out.println("HIZO CLICK");
            showAlert(currentServer);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    Async task to get tables from the server
    */
    public class GetTables extends AsyncTask<String , Void ,String> {
        String server_response;
        SharedPreferences.Editor editor = preferences.edit();

        @Override
        protected String doInBackground(String... strings) {

            URL url;
            HttpURLConnection urlConnection = null;

            try {
                //establishes a connection with the server
                url = new URL(strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                int responseCode = urlConnection.getResponseCode();

                if(responseCode == HttpURLConnection.HTTP_OK){
                    server_response = readStream(urlConnection.getInputStream());

                    //saves the table names in shared preferences
                    editor.putString(packageSelected+TABLES,server_response);
                    editor.commit();
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
            finish();
        }
    }

    /*
    Reads the answer from the server and converts it into a string
    */
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

    /*
    Async task to consult from firebase previous executions of the selected app
    */
    private class LoadExecutions extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... strings) {

            db.collection(device).whereEqualTo("packageName", packageSelected)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                ArrayList<String> execs = new ArrayList<>();

                                if(task.getResult().isEmpty()){
                                    adapter.updateList(new ArrayList<String>());
                                }
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    execs.add(document.getId());
                                }
                                Collections.sort(execs);
                                adapter.updateList(execs);
                            }
                        }
                    });
            return null;
        }
    }
}
