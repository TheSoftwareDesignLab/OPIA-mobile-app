package com.lanabeji.opia.AppList;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lanabeji.opia.AppDetail.AppDetailActivity;
import com.lanabeji.opia.Main.MainActivity;
import com.lanabeji.opia.R;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by lanabeji on 17/03/19.
 * Recycler view adapter to show installed apps
 */

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    List<AppItem> appList;
    Context context;
    SharedPreferences sharedPref;

    //Name of variable to save in S.P when accessibility api is recording a sequence of events
    public final static String RECORDING = "IS_RECORDING";

    public AppListAdapter(List<AppItem>AppList)
    {
        this.appList = AppList;
    }

    // ADAPTER METHODS

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        context = parent.getContext();

        sharedPref = context.getSharedPreferences(MainActivity.APP, MODE_PRIVATE);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final AppItem appItem = appList.get(position);

        holder.textApp.setText(appItem.getName());
        holder.imgApp.setImageDrawable(appItem.getImg());
        holder.cv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                writeSelected(appItem.getPackageName(), true);

                Bitmap bitmap = ((BitmapDrawable)appItem.getImg()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] b = baos.toByteArray();

                //Start a detail activity, with information of the selected app
                Intent intent = new Intent(context, AppDetailActivity.class);
                intent.putExtra(AppDetailActivity.APP_NAME, appItem.getName());
                intent.putExtra(AppDetailActivity.APP_IMAGE, b);
                intent.putExtra(AppDetailActivity.APP_PACKAGE, appItem.getPackageName());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }


    // METHODS

   /*
   * Write in shared preferences the selected app to record and replay
   */
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
