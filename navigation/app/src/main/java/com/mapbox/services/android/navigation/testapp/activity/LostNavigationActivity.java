package com.mapbox.services.android.navigation.testapp.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;

public class LostNavigationActivity extends AppCompatActivity implements OnMapReadyCallback {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_lost_navigation);

    // Create supportMapFragment
    SupportMapFragment mapFragment;
    if (savedInstanceState == null) {

      // Create fragment
      final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

      // Build mapboxMap
      MapboxMapOptions options = new MapboxMapOptions();
      options.styleUrl(Style.MAPBOX_STREETS);
//      options.camera(new CameraPosition.Builder()
//        .target(target)
//        .zoom(9)
//        .build());

      // Create map fragment
      mapFragment = SupportMapFragment.newInstance(options);

      // Add map fragment to parent container
      transaction.add(R.id.container, mapFragment, "com.mapbox.map");
      transaction.commit();
    } else {
      mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentByTag("com.mapbox.map");
    }

    mapFragment.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {

    // Adjust location engine to force a gps reading every second. This isn't required but gives an overall
    // better navigation experience for users. The updating only occurs if the user moves 3 meters or further
    // from the last update.
//    locationEngine.setInterval(0);
//    locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
//    locationEngine.setFastestInterval(1000);
//    locationEngine.activate();

  }
}
