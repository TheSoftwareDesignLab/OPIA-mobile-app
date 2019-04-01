package com.lanabeji.opia;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lanabeji on 17/03/19.
 */

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    List<AppItem> appList;
    Context context;
    SharedPreferences sharedPref;
    public final static String RECORDING = "IS_RECORDING";

    public AppListAdapter(List<AppItem>AppList)
    {
        this.appList = AppList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        context = parent.getContext();

        sharedPref = context.getSharedPreferences(MainActivity.APP, Context.MODE_PRIVATE);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final AppItem appItem = appList.get(position);

        holder.textApp.setText(appItem.getName());
        holder.imgApp.setImageDrawable(appItem.getImg());
        holder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                String message = "Do you want to select " + appItem.getName() + "?";
                builder.setMessage(message);
                builder.setPositiveButton("Record", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Record button
                        Log.d("Record", "USER CLICKED RECORD");

                        writeSelected(appItem.getPackageName(), true);
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appItem.getPackageName());
                        if (launchIntent != null) {
                            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            context.startActivity(launchIntent);
                        }
                    }
                });
                builder.setNeutralButton("Replay", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Replay button
                        Log.d("Replay", "USER CLICKED Replay");

                        writeSelected(appItem.getPackageName(), false);
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(appItem.getPackageName());
                        if (launchIntent != null) {
                            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            context.startActivity(launchIntent);
                        }
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public void writeSelected(String packageSelected, boolean isRecording){
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(MainActivity.PACKAGE, packageSelected);
        editor.putBoolean(RECORDING, isRecording);
        editor.commit();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        ImageView imgApp;
        TextView textApp;
        CardView cv;

        public ViewHolder(View itemView)
        {
            super(itemView);
            imgApp = (ImageView)itemView.findViewById(R.id.imgApp);
            textApp = (TextView)itemView.findViewById(R.id.textApp);
            cv = (CardView)itemView.findViewById(R.id.cv);
        }
    }


}
