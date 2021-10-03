
package com.example.rainmanv5;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity  {

    private static final String TAG = "ERRORRRRR";
    private FusedLocationProviderClient fusedLocationProviderClient;

    //initialising the global variables, they need to be accessible from all methods inside the class
    //there are very few to try and stay with convention
    private double lat = 0.0;
    private double lng = 0.0;
    private int ACCESS_FINE_LOCATION_CODE = 2;
    private CollectionReference mColRef = FirebaseFirestore.getInstance().collection("appData");
    public Double Temp;
    private RequestQueue mqueu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mqueu = Volley.newRequestQueue(this);
        //these are the methods in initialize the post (deleting the old ones) and the location
        //which is used to start up th location sensors
        initPosts();
        findLocation();
        getWeather();




    }


    //the method that deletes all the posts which are older than 2 hours.
    private void initPosts(){
        //it gets all of the documents from the database
        mColRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                //this is a for loop that goes through all retrieved documents
                for (QueryDocumentSnapshot document:task.getResult()){

                    //variable which get the difference between right now and the time on the document
                    long deltaTime = findDate()-document.getLong("time");
                    //boolean which is true when delta time in greater than 2 hours
                    boolean twoH = false;

                    //if statement that checks if deltatime is greater than 2 hours (9000000 is 2 hours)
                    if (9000000-deltaTime < 0){
                        twoH = true;
                    }

                    //if loop checking if twoH is true (post older than 2 hours)
                    if (twoH){
                        //gets the id of the current document and makes it a document reference
                        DocumentReference docToDel = mColRef.document(document.getId());
                        //it deletes the document with the given id (a document older than 2 hours)
                        docToDel.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //gives a log message of why the documents are deleted
                                Log.d(TAG, "onComplete: "+"document deleted because too old");
                            }
                        });
                    }

                }

            }
        });
    }

    //changes the activity to the maps activity
    public void nextActivity(View view){
        View mapsView = view.findViewById(R.id.mapsView);
        TextView tempText = mapsView.findViewById(R.id.temperature);
        double temp = getWeather();
        tempText.setText(Double.toString(temp));

        //changes intent to the maps class
        startActivity(new Intent(MainActivity.this, MapsActivity.class));
    }

    //it is the method which posts the weather data
    public void postWeather(View view){
        //calling on all the find classes to get the info from the censors and all the buttons
        //and text feilds
        String  roadID = findCheckedRoad();
        String weatherID = findCheckedWeather();
        String comment = findComment();
        double lat = findLocation()[0];
        double lng = findLocation()[1];
        long day = findDate();

        //checks that the location is valid, if not gives error message to user
        if (lat == 0.0 && lng == 0.0){
            toast("problem getting position, please try again");
            return;
        }

        //checks that all the feilds were selected if not it gives error message to user
        if (roadID == "-1" || weatherID == "-1"){
            toast("please fill both radio feilds");
            return;
        }

        //creates a hashMap of the data to post to the database
        Map<String, Object> dataToSave = new HashMap<>();
        //adds all the values retrieved from the get classes
        dataToSave.put("roadID", roadID);
        dataToSave.put("weatherID", weatherID);
        dataToSave.put("comment", comment);
        dataToSave.put("time", day);
        dataToSave.put("lat", lat);
        dataToSave.put("lng", lng);

        //it adds the hashMap to the database as a document
        mColRef.add(dataToSave).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                //sends a message to the user that the data was added successfully
                toast("weather data added succesfully");
                Log.d("error", "weather data was added");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //sends a message to the user that adding the data failed
                Log.d("error", "weather data was not added", e);
                toast("adding weather data failed");
            }
        });
    }

    //finds the current time and returns it as a long for the initPosts method to check the time
    //difference
    private long findDate(){
        Date date = new Date();

        long day = date.getTime();
        Log.d(TAG, "findDate: "+day);
        return day;

    }

    //finds the checked road condition from the data posting page
    private String findCheckedRoad(){
        //it gets the radioGroup field
        RadioGroup radioGroup = findViewById(R.id.Road);
        //it gets the checked button on the radio field
        int radioID = radioGroup.getCheckedRadioButtonId();

        //it gets the id of the button selected
        RadioButton radButt = findViewById(radioID);

        //it checks if no button is selected, if so it returns -1 so the other classes know that no
        //button is selected
        if (radButt == null){
            return "-1";
        }

        //it returns the string associated with the selected button
        return (String) radButt.getText();
    }

    //it retrieves the text in the comment field and returns it
    private String findComment(){
        EditText commentView = (EditText) findViewById(R.id.editText);
        String comment = commentView.getText().toString();
        return comment;
    }

    //it is very similar to the findCheckedRoad class and has the same conventions except for the
    //weather field
    private String findCheckedWeather(){
        RadioGroup radioGroup = findViewById(R.id.Weather);
        int radioID = radioGroup.getCheckedRadioButtonId();
        RadioButton radButt = findViewById(radioID);

        if (radButt == null){
            return "-1";
        }

        return (String) radButt.getText();
    }

    //it gets the permission to retrieve the location, which is necessary as it is a dangerous
    //permission according to google and if once it has permission it uses the phone sensors to get
    //the location of the device
    public double[] findLocation(){
        //creates an double array for the latitude and longitude of the location
        double[] loc = {0.0,0.0};
        //this statement checks if the app has the permission for the fine location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            //this statement checks if it should create a permission window to ask for permission,
            //this only gets called if the permission has not yet been granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                //this is an android studio class that creates a pop up window asking for permission,
                //i then set all the different values for the pop up in order to get the permission
                new AlertDialog.Builder(this)
                        .setTitle("Permission needed")
                        .setMessage("Your location is needed to use this app")
                        .setPositiveButton("give permission", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        //this creates and shows the pop up
                        .create().show();
            }
            else {
                //if the pop up doesn't work it still tries to get the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},ACCESS_FINE_LOCATION_CODE);
            }
        }
        //this only runs if the permission is already granted
        else {
            //this gets the last location read by the sensor on the phone
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        //sets the latitude and longitude to the values of the sensor
                        lat = location.getLatitude();
                        lng = location.getLongitude();
                    } else {
                        //fails to use the sensor so emits a message to the user
                        toast("unable to get location please try again");

                    }
                }
            });
        }
        //sets the location to what was found on the sensor
        loc[0] = lat;
        loc[1] = lng;
        return loc;
    }

    public double getWeather(){
        //this is the url for the api I use to get the temperature
        String API =
                "http://api.openweathermap.org/data/2.5/weather?%s&APPID=ec913753edff372cd4bfe1a2b191c09b&units=metric";

        //gets the location from the findLocation method
        double lat = findLocation()[0];
        double lng = findLocation()[1];

        if (lat == 0.0 && lng == 0.0){
            //returns an error if the location is 0.0 meaning there was a problem with the sensors
            //there is a possibility that a user would be at 0,0 but since there is no land there
            //this should not be a problem for this app
            toast("problem getting position, please try again");
        }

        //adds the location to the API url
        final String loc = "lat="+lat+"&lon="+lng;
        String url = String.format(API, loc);

        //creates the get request for the API
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            //this is the logic if the JSON object is returned from the API
                            JSONObject jason = response.getJSONObject("main");
                            double temp = jason.getDouble("temp");
                            Temp = temp;
                        } catch (JSONException e) {
                            //if the JSON object is not returned the stack trace is logged
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        //if no temperature is returned it returns 0.0 degrees as to not have the app crash, this
        //should cary no significance on the app
        if (Temp==null){
            return 0.0;
        }
        //this adds the request to the queue
        mqueu.add(request);
        return Temp;

        }

    //this is a method to display a small pop up meassage to tell the user things
    public void toast(String message){
        Toast toast = Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG);
        toast.show();
    }



}
