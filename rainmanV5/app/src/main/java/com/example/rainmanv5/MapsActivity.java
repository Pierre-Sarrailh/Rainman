package com.example.rainmanv5;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //creates the googlemap and the colection reference global variables
    private GoogleMap mMap;
    private CollectionReference mColRef = FirebaseFirestore.getInstance().collection("appData");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    //changes activity to the input activity
    public void nextActivity(View view){
        startActivity(new Intent(MapsActivity.this, MainActivity.class));
    }

    //this is where the map is initialised and all the markers are added
    @Override
    public void onMapReady(GoogleMap googleMap) {

        //gets the googlemap fragment reference and sets it to the global variable and then
        //sets the info window adapter to the one I created for the app
        mMap = googleMap;
        mMap.setInfoWindowAdapter(new customInfoWindowAdapter(MapsActivity.this));

        //gets all of the weather data and it creates all of the markers
        mColRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    int i =0;
                    //it sets the camera to China as this is the geographical location of my client
                    LatLng China = new LatLng(31, 121);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(China));

                    //it loops through all the weather data
                    for (QueryDocumentSnapshot document:task.getResult()){

                        //creates variables to store the location of the data and creates variables
                        //for the weather and road conditions
                        double lat = document.getDouble("lat");
                        double lng = document.getDouble("lng");
                        LatLng loc = new LatLng(lat,lng);
                        String wid = document.getString("weatherID");
                        String rid = document.getString("roadID");

                        //it creates the marker with the variables that were set above with relation
                        //to the custom info window I created
                        mMap.addMarker(new MarkerOptions().position(loc).title("weather is:"+wid+" "+"road is:"+rid).snippet(document.getString("comment")));
                        i++;
                    }
                }
                else{
                    //logs the error if the pins dont work
                    Log.d("ERROR", "onComplete: error getting docs", task.getException());
                }
            }
        });
    }




    //method to create a small pop up for errors for the client.
    public void toast(String message){
        Toast toast = Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG);
        toast.show();
    }

}


