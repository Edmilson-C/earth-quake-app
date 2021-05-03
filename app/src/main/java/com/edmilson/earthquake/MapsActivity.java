package com.edmilson.earthquake;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.edmilson.earthquake.model.EarthQuake;
import com.edmilson.earthquake.ui.CustomInfoWindow;
import com.edmilson.earthquake.util.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {
    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RequestQueue queue;
    private float iconColor[];
    private Button butShowList;
    private ArrayList<String> placesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        placesList = new ArrayList<>();
        butShowList = findViewById(R.id.butShowList);
        queue = Volley.newRequestQueue(this);
        iconColor = new float[]{
                BitmapDescriptorFactory.HUE_AZURE,
                BitmapDescriptorFactory.HUE_BLUE,
                BitmapDescriptorFactory.HUE_CYAN,
                BitmapDescriptorFactory.HUE_GREEN,
                BitmapDescriptorFactory.HUE_MAGENTA,
                BitmapDescriptorFactory.HUE_ORANGE,
                BitmapDescriptorFactory.HUE_ROSE,
                BitmapDescriptorFactory.HUE_VIOLET,
                BitmapDescriptorFactory.HUE_YELLOW
        };

        butShowList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, QuakesListActivity.class);
                intent.putExtra("Lista", placesList);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);

        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if(ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getEarthQuakes();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) ==  PackageManager.PERMISSION_GRANTED) {
                getEarthQuakes();
            }
        }
    }

    public void getEarthQuakes() {
        final EarthQuake earthQuake = new EarthQuake();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray features = response.getJSONArray("features");
                            for(int i = 0; i < Constants.LIMIT; i++) {
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
                                JSONArray coordinates = features.getJSONObject(i).getJSONObject("geometry").getJSONArray("coordinates");
                                double longitude = coordinates.getDouble(0);
                                double latitude = coordinates.getDouble(1);

                                Random random = new Random();

                                earthQuake.setPlace(properties.getString("place"));
                                earthQuake.setMagnitude(properties.getDouble("mag"));
                                earthQuake.setTime(properties.getLong("time"));
                                earthQuake.setDetailLink(properties.getString("detail"));
                                earthQuake.setType(properties.getString("type"));
                                earthQuake.setLatitude(latitude);
                                earthQuake.setLongitude(longitude);

                                DateFormat df = DateFormat.getDateInstance();
                                String time = df.format(new Date(properties.getLong("time")).getTime());

                                LatLng latLng = new LatLng(earthQuake.getLatitude(), earthQuake.getLongitude());
                                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title(earthQuake.getPlace())
                                .icon(BitmapDescriptorFactory.defaultMarker(iconColor[random.nextInt(iconColor.length)]))
                                .snippet("Magnitude: " + earthQuake.getMagnitude() + "\nTime: " + time));
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                                marker.setTag(earthQuake.getDetailLink());

                                placesList.add(earthQuake.getPlace());

                                if(earthQuake.getMagnitude() >= 2.0) {
                                    CircleOptions circleOptions = new CircleOptions();
                                    circleOptions.center(latLng);
                                    circleOptions.radius(30000);
                                    circleOptions.strokeWidth(5.5f);
                                    circleOptions.fillColor(Color.RED);
                                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                    mMap.addCircle(circleOptions);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        getQuakeDetails(marker.getTag().toString());
//        Toast.makeText(this, marker.getTag().toString(), Toast.LENGTH_SHORT).show();
    }

    private void getQuakeDetails(String url) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String detailUrl;
                        try {
                            JSONObject properties = response.getJSONObject("properties");
                            JSONArray geoserve = properties.getJSONObject("products").getJSONArray("geoserve");
                            JSONObject geoserveJson = geoserve.getJSONObject(0).getJSONObject("contents").getJSONObject("geoserve.json");

                            detailUrl = geoserveJson.getString("url");
//                            Toast.makeText(MapsActivity.this, detailUrl, Toast.LENGTH_SHORT).show();
                            getMoreDetais(detailUrl);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
                queue.add(jsonObjectRequest);
    }

    public void getMoreDetais(String url) {
        final View view = getLayoutInflater().inflate(R.layout.popup, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        builder.setView(view);

        final AlertDialog dialog = builder.create();
        dialog.show();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        TextView tvList = view.findViewById(R.id.tvList);
                        Button butClose = view.findViewById(R.id.butClose);
                        Button butDismiss = view.findViewById(R.id.butDismiss);
                        WebView htmlWebview = view.findViewById(R.id.htmlWebview);
                        String s = "", text;
                        try {
                            if(response.has("tectonicSummary") && response.getJSONObject("tectonicSummary") != null &&
                               response.getJSONObject("tectonicSummary").has("text") && response.getJSONObject("tectonicSummary").getString("text") != null) {
                                text = response.getJSONObject("tectonicSummary").getString("text");
                                htmlWebview.loadData(text, "text/html", "UTF-8");
                            }

                            JSONArray cities = response.getJSONArray("cities");
                            for(int i = 0; i < cities.length(); i++) {
                                String name = cities.getJSONObject(i).getString("name");
                                int distance = cities.getJSONObject(i).getInt("distance");
                                int population = cities.getJSONObject(i).getInt("population");

                                s += "City: " + name + "\nDistance: " + distance + "\nPopulation: " + population + "\n\n";
//                                Toast.makeText(MapsActivity.this, s, Toast.LENGTH_SHORT).show();
                            }
                            tvList.setText(s);
                        } catch (JSONException e) {
                            Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }

                        butClose.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                        butDismiss.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }
}
