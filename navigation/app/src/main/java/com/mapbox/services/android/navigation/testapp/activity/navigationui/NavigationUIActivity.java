package com.mapbox.services.android.navigation.testapp.activity.navigationui;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.location.MockLocationEngine;
import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.testapp.Utils;
import com.mapbox.services.android.navigation.ui.v5.directions.DirectionsTopSheet;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.api.directions.v5.models.DirectionsResponse;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.utils.turf.TurfConstants;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.commons.models.Position;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class NavigationUIActivity extends AppCompatActivity implements OnMapReadyCallback,
  MapboxMap.OnMapClickListener, ProgressChangeListener {

  private static final int BEGIN_ROUTE_MILESTONE = 1001;

  // Map variables
  @BindView(R.id.mapView)
  MapView mapView;

  @BindView(R.id.cancelNavFab)
  FloatingActionButton cancelNavFab;

  @BindView(R.id.startRouteButton)
  Button startRouteButton;

  @BindView(R.id.directionsTopSheet)
  DirectionsTopSheet directionsTopSheet;

  private MapboxMap mapboxMap;
  private List<Marker> pathMarkers = new ArrayList<>();

  // Navigation related variables
  private LocationEngine locationEngine;
  private MapboxNavigation navigation;
  private DirectionsRoute route;
  private NavigationMapRoute navigationMapRoute;
  private LocationLayerPlugin locationLayerPlugin;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation_ui_layout);
    ButterKnife.bind(this);

    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);

    navigation = new MapboxNavigation(this, Mapbox.getAccessToken());
  }

  @OnClick(R.id.startRouteButton)
  public void onStartRouteClick() {
    if (navigation != null && route != null) {
      // Set the LocationLayerPlugin to Navigation mode
      locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.NAVIGATION);

      // Hide the start button
      startRouteButton.setVisibility(View.INVISIBLE);

      // Attach our navigation listeners
      navigation.addProgressChangeListener(NavigationUIActivity.this);
      navigation.addProgressChangeListener(directionsTopSheet);

      ((MockLocationEngine) locationEngine).setRoute(route);
      navigation.setLocationEngine(locationEngine);
      navigation.startNavigation(route);

      // Show Views
      cancelNavFab.show();
      directionsTopSheet.show();
    }
  }

  @OnClick(R.id.cancelNavFab)
  public void onCancelNavClick() {
    if (directionsTopSheet.getVisibility() == View.VISIBLE) {
      directionsTopSheet.hide();
    }
    resetNavigation();
    Snackbar.make(mapView, "Tap map to place waypoint", BaseTransientBottomBar.LENGTH_LONG).show();
    cancelNavFab.hide();
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;

    locationLayerPlugin = new LocationLayerPlugin(mapView, mapboxMap, null);
    navigationMapRoute = new NavigationMapRoute(navigation, mapView, mapboxMap);

    mapboxMap.setOnMapClickListener(this);
    Snackbar.make(mapView, "Tap map to place waypoint", BaseTransientBottomBar.LENGTH_LONG).show();

    locationEngine = new MockLocationEngine(1000, 30, true);
    mapboxMap.setLocationSource(locationEngine);

    newOrigin();
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    if (pathMarkers.size() >= 2) {
      Toast.makeText(this, "Only 2 waypoints supported", Toast.LENGTH_LONG).show();
      return;
    }
    Marker marker = mapboxMap.addMarker(new MarkerOptions().position(point));
    pathMarkers.add(marker);

    startRouteButton.setVisibility(View.VISIBLE);
    calculateRoute();
  }

  private void newOrigin() {
    if (mapboxMap != null) {
      LatLng latLng = Utils.getRandomLatLng(new double[] {-77.1825, 38.7825, -76.9790, 39.0157});
      ((MockLocationEngine) locationEngine).setLastLocation(
        Position.fromLngLat(latLng.getLongitude(), latLng.getLatitude())
      );
      mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
      mapboxMap.setMyLocationEnabled(true);
      mapboxMap.getTrackingSettings().setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
    }
  }

  private void calculateRoute() {
    Location userLocation = mapboxMap.getMyLocation();
    if (userLocation == null) {
      Timber.d("calculateRoute: User location is null, therefore, origin can't be set.");
      return;
    }

    Position origin = Position.fromCoordinates(userLocation.getLongitude(), userLocation.getLatitude());
    Position destination = Position.fromLngLat(
      pathMarkers.get(pathMarkers.size() - 1).getPosition().getLongitude(),
      pathMarkers.get(pathMarkers.size() - 1).getPosition().getLatitude()
    );
    if (TurfMeasurement.distance(origin, destination, TurfConstants.UNIT_METERS) < 50) {
      for (Marker marker : pathMarkers) {
        mapboxMap.removeMarker(marker);
      }
      startRouteButton.setVisibility(View.GONE);
      return;
    }

    List<Position> coordinates = new ArrayList<>();
    coordinates.add(origin);

    for (Marker marker : pathMarkers) {
      coordinates.add(Position.fromLngLat(marker.getPosition().getLongitude(), marker.getPosition().getLatitude()));
    }

    navigation.getRoute(coordinates, null, new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        Timber.d("Url: %s", call.request().url().toString());
        if (response.body() != null) {
          if (response.body().getRoutes().size() > 0) {
            DirectionsRoute route = response.body().getRoutes().get(0);
            NavigationUIActivity.this.route = route;
            navigationMapRoute.addRoute(route);
          }
        }
      }

      @Override
      public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
        Timber.e("onFailure: navigation.getRoute()", throwable);
      }
    });
  }

  private void resetNavigation() {
    locationEngine.deactivate();
    locationEngine.removeLocationUpdates();
    navigationMapRoute.removeRoute();
    navigation.endNavigation();
    for (Marker marker : pathMarkers) {
      mapboxMap.removeMarker(marker);
    }
    pathMarkers.clear();
    locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    locationLayerPlugin.forceLocationUpdate(location);
    Timber.d("onProgressChange: fraction of route traveled: %f", routeProgress.getFractionTraveled());
  }

  /*
   * Activity lifecycle methods
   */

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStart() {
    super.onStart();
    navigation.onStart();
    mapView.onStart();
    if (locationLayerPlugin != null) {
      locationLayerPlugin.onStart();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    navigation.onStop();
    mapView.onStop();
    locationLayerPlugin.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();

    // Remove all navigation listeners
    navigation.removeProgressChangeListener(this);
    navigation.removeProgressChangeListener(directionsTopSheet);
    navigation.onDestroy();

    // End the navigation session
    navigation.endNavigation();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }
}
