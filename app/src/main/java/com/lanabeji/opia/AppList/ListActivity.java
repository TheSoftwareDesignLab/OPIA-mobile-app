package com.lanabeji.opia.AppList;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.lanabeji.opia.AppDetail.AppDetailActivity;
import com.lanabeji.opia.Main.MainActivity;
import com.lanabeji.opia.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
List all apps installed on the device with a recycler view
*/
public class ListActivity extends AppCompatActivity {

    //Recycler view variables
    RecyclerView recyclerView;
    AppListAdapter appListAdapter;
    ArrayList<AppItem> apps;

    ActionBar actionBar;

    //Storage
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        preferences = getSharedPreferences(MainActivity.APP, MODE_PRIVATE);

        setupActionBar();
        getApps();
    }

    /*
    Creates and adds a toolbar
    */
    private void setupActionBar(){
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    //METHODS

    /*
    Lists all the app and initializes the recycler view
    */
    private void getApps(){
        apps = new ArrayList<AppItem>();
        final PackageManager pm = getPackageManager();

        //Get a list of installed apps
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Intent launchActivity;
        AppItem currentApp;

        for (ApplicationInfo packageInfo : packages) {

            launchActivity = pm.getLaunchIntentForPackage(packageInfo.packageName);

            //Shows all the apps except android system apps and opia
            if (!packageInfo.packageName.startsWith("com.android") && launchActivity != null && !packageInfo.packageName.equals("com.lanabeji.opia")){

                currentApp = new AppItem();
                currentApp.setPackageName(packageInfo.packageName);
                currentApp.setImg(packageInfo.loadIcon(getPackageManager()));
                currentApp.setName(packageInfo.loadLabel(getPackageManager()).toString());

                apps.add(currentApp);
            }
        }

        //Sort app to show them in alphabetical order
        Collections.sort(apps, new Comparator<AppItem>() {
            public int compare(AppItem o1, AppItem o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        appListAdapter = new AppListAdapter(apps);

        recyclerView = (RecyclerView)findViewById(R.id.rvApps);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(appListAdapter);
    }

    /*
    Shows alert to modify server IP Address
    */
    public void showAlert(String current){

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("IP Address");
        alert.setMessage("Enter the IP address where the server is running");
        final EditText input = new EditText(this);

        if(current.equals("EMPTY")){ //if there isn't an ip address saved, show a hint
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

                if(validateIPAddress(value)){ //validates if user input is an ip
                    String serverUrl = "http://"+value+":5000";
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(MainActivity.IP_SERVER,value);
                    editor.putString(MainActivity.SERVER, serverUrl);
                    editor.commit();
                }
                else{
                    Toast.makeText(ListActivity.this, "Please enter a valid IP Address",Toast.LENGTH_SHORT).show();
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
        Pattern IPv4_PATTERN = Pattern.compile(AppDetailActivity.IPv4_REGEX);

        if (ip == null) {
            return false;
        }
        Matcher matcher = IPv4_PATTERN.matcher(ip);
        return matcher.matches();
    }

    // ACTION BAR METHODS

    @Nullable
    @Override
    public ActionBar getSupportActionBar() {
        return super.getSupportActionBar();
    }

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

        if (id == R.id.action_http) { //show alert to modify ip address
            String currentServer = preferences.getString(MainActivity.IP_SERVER, "EMPTY");
            showAlert(currentServer);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
