package com.mapbox.services.android.navigation.testapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.api.utils.turf.TurfGrids;
import com.mapbox.services.api.utils.turf.TurfInvariant;
import com.mapbox.services.api.utils.turf.TurfJoins;
import com.mapbox.services.api.utils.turf.TurfMeasurement;
import com.mapbox.services.api.utils.turf.TurfMisc;
import com.mapbox.services.commons.geojson.Feature;
import com.mapbox.services.commons.geojson.FeatureCollection;
import com.mapbox.services.commons.geojson.Point;
import com.mapbox.services.commons.geojson.Polygon;
import com.mapbox.services.commons.models.Position;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static android.content.ContentValues.TAG;

public class Utils {

  /**
   * <p>
   * Returns the Mapbox access token set in the app resources.
   * </p>
   * It will first search for a token in the Mapbox object. If not found it
   * will then attempt to load the access token from the
   * {@code res/values/dev.xml} development file.
   *
   * @param context The {@link Context} of the {@link android.app.Activity} or {@link android.app.Fragment}.
   * @return The Mapbox access token or null if not found.
   */
  public static String getMapboxAccessToken(@NonNull Context context) {
    try {
      // Read out AndroidManifest
      String token = Mapbox.getAccessToken();
      if (token == null || token.isEmpty()) {
        throw new IllegalArgumentException();
      }
      return token;
    } catch (Exception exception) {
      // Use fallback on string resource, used for development
      int tokenResId = context.getResources()
        .getIdentifier("mapbox_access_token", "string", context.getPackageName());
      return tokenResId != 0 ? context.getString(tokenResId) : null;
    }
  }

  /**
   * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
   */
  public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id) {
    Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
    Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
      vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);
    return IconFactory.getInstance(context).fromBitmap(bitmap);
  }

  public static LatLng getRandomLatLng(Context context) {
    FeatureCollection cities = FeatureCollection.fromJson(loadJsonFromAsset("cities.json", context));

    Random r = new Random();

    Polygon polygon = (Polygon) cities.getFeatures().get(r.nextInt(cities.getFeatures().size())).getGeometry();
    double[] bbox = TurfMeasurement.bbox(polygon);

    double randomLat = bbox[1] + (bbox[3] - bbox[1]) * r.nextDouble();
    double randomLon = bbox[0] + (bbox[2] - bbox[0]) * r.nextDouble();
    Position randomPosition = Position.fromCoordinates(randomLon, randomLat);
    boolean isInside = TurfJoins.inside(Point.fromCoordinates(randomPosition), polygon);

    if (isInside) {
      LatLng latLng = new LatLng(randomLat, randomLon);
      Log.d(TAG, String.format(Locale.US, "getRandomLatLng: %s", latLng.toString()));
      return latLng;
    } else {
      getRandomLatLng(context);
    }
    return new LatLng(0, 0);
  }

  private static String loadJsonFromAsset(String filename, Context context) {
    // Using this method to load in GeoJSON files from the assets folder.

    try {
      InputStream is = context.getAssets().open(filename);
      int size = is.available();
      byte[] buffer = new byte[size];
      is.read(buffer);
      is.close();
      return new String(buffer, "UTF-8");

    } catch (IOException ex) {
      ex.printStackTrace();
      return null;
    }
  }
}
