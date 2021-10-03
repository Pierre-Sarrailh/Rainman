package com.example.rainmanv5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class customInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    //creates variables for the window and the context
    private final View mWindow;
    private Context mContext;

    //sets the context and window variables to those passed in
    public customInfoWindowAdapter(Context context){
        mContext = context;
        mWindow = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
    }


    private void renderWindowText(Marker marker, View view){
        //creates variables for the different part of the context window
        String title = marker.getTitle();
        TextView tvTitle =(TextView) view.findViewById(R.id.title);
        //sets those variables
        if (!title.equals("")){
            tvTitle.setText(title);
        }

        String snippet = marker.getSnippet();
        TextView tvSnippet =(TextView) view.findViewById(R.id.snippet);
        if (!snippet.equals("")){
            tvSnippet.setText(snippet);
        }
    }

    @Override
    //sets info window to the window context file i made
    public View getInfoWindow(Marker marker) {
        renderWindowText(marker,mWindow);
        return mWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        renderWindowText(marker,mWindow);
        return mWindow;
    }
}
