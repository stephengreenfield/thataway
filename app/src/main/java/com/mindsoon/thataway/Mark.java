package com.mindsoon.thataway;

import android.location.Address;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

class Mark {
    private String name, distanceUnits;
    private Point textPoint, imagePoint;
    private int distance, degrees, id, imageId;
    private double latitude, longitude;
    private Marker marker;

    // Constructor for user's Mark object
    public Mark(GoogleMap map) {
        marker = map.addMarker(new MarkerOptions()
                .position(new LatLng(0,0))
                .anchor(0.50f, 0.50f)
                .snippet("my location"));
    }

    // constructor if input is a marker pin
    public Mark(Marker queryMarker, Mark me, GoogleMap map){
        name = queryMarker.getTitle();
        latitude = queryMarker.getPosition().latitude;
        longitude = queryMarker.getPosition().longitude;
        initializeMarkValues(me);
        marker = map.addMarker(new MarkerOptions()
                 .title(name)
                 .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_pin_place))
                 .position(new LatLng(latitude, longitude))
                 .snippet("place"));
    }

    // constructor if input is a location
    public Mark(Address newAddress, Mark me, GoogleMap map){
        if (newAddress != null) {
            name = getLocationName(newAddress);
            latitude = newAddress.getLatitude();
            longitude = newAddress.getLongitude();
            initializeMarkValues(me);
            marker = map.addMarker(new MarkerOptions()
                    .title(name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_pin_place))
                    .position(new LatLng(latitude, longitude))
                    .snippet("place"));
        }
    }

    // constructor if input is a user
    public Mark(JSONObject json, Mark me, GoogleMap map) throws JSONException {
        if (json != null) {
            name = json.getString("sessionName");
            latitude = json.getDouble("latitude");
            longitude = json.getDouble("longitude");
            initializeMarkValues(me);
            marker = map.addMarker(new MarkerOptions()
                    .title(name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_pin_person))
                    .position(new LatLng(latitude, longitude))
                    .snippet("person"));
        }
    }

    // constructor if input is an artbitrary point
    public Mark(String newName, Mark me, GoogleMap map){
        name = newName;
        latitude = me.getLatitude();
        longitude = me.getLongitude();
        initializeMarkValues(me);
        distanceUnits = "yards";
        marker = map.addMarker(new MarkerOptions()
                .title(name)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_pin_place))
                .position(new LatLng(latitude, longitude))
                .snippet("place"));
    }

    // constructor if input is a query result
    public Mark(Address newAddress, GoogleMap map){
        if (newAddress != null) {
            name = getLocationName(newAddress);
            latitude = newAddress.getLatitude();
            longitude = newAddress.getLongitude();
            initializeMarkValues(null);
            marker = map.addMarker(new MarkerOptions()
                    .title(name)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.img_pin_query))
                    .position(new LatLng(latitude, longitude))
                    .snippet("search result"));
        }
    }

    // Initialize values for a new Mark object
    private void initializeMarkValues(Mark me){
        if (me != null){
            distance = calculateDistance(me.getLatitude(), me.getLongitude(), latitude, longitude);
            degrees = calculateDegrees(me.getLatitude(), me.getLongitude(), latitude, longitude);
        } else {
            distance = 0;
            degrees = 0;
        }
        id = Math.abs((name + latitude + longitude + distance).hashCode());
        imageId = Math.abs(("img" + id).hashCode());
        textPoint = new Point(-1000,-1000);
        imagePoint = new Point(-1000,-1000);
    }

    // if Mark is a location, get the best name from the Address object
    private String getLocationName(Address newAddress) {
        if (newAddress.getAddressLine(0) != null) {
            return newAddress.getAddressLine(0);
        } else if (newAddress.getFeatureName() != null) {
            return newAddress.getFeatureName();
        } else if (newAddress.getLocality() != null) {
            return newAddress.getLocality();
        } else {
            return null;
        }
    }

    // calculate degrees from current location with latitude & longitude input
    private int calculateDegrees(double mLat, double mLon, double newLat, double newLon) {
        float dy = (float) (newLat - mLat);
        float dx = (float) (Math.cos(Math.PI / 180 * mLat)*(newLon - mLon));
        double degrees = Math.toDegrees( Math.atan2(dy, dx) ) - 90;
        if ( degrees < 0 ) degrees += 360;
        return (int) degrees;
    }

    // calculate distance from current location with latitude & longitude input
    // then sets distanceUnits to yards or miles
    private int calculateDistance(double mLat, double mLon, double lat2, double lon2) {
        double earthRadiusMiles = 3958.75;
        double earthRadiusYards = 6967400;
        double dLat = Math.toRadians(lat2-mLat);
        double dLng = Math.toRadians(lon2-mLon);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(mLat)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        if ( c < (2/earthRadiusYards) ) {
            distanceUnits = "yard";
            return 1;
        } else if ( c < (1/earthRadiusMiles) ) {
            distanceUnits = "yards";
            return (int) (earthRadiusYards * c);
        } else if ( c < (2/earthRadiusMiles) ) {
            distanceUnits = "mile";
            return 1;
        } else {
            this.distanceUnits = "miles";
            return (int) (earthRadiusMiles * c);
        }
    }

    // update relative position of a Mark
    public void updatePosition(Mark me){
        distance = calculateDistance(me.getLatitude(), me.getLongitude(), latitude, longitude);
        degrees = calculateDegrees(me.getLatitude(), me.getLongitude(), latitude, longitude);
    }

    // calculate current location by averaging it with 3 most recent locations
    public void approximateCurrentLocation(ArrayList<History> recentHistory){
        double averageLatitude = 0;
        double averageLongitude = 0;
        for(History h : recentHistory) {
            averageLatitude += h.getLatitude();
            averageLongitude += h.getLongitude();
        }
        latitude =  averageLatitude / recentHistory.size();
        longitude = averageLongitude / recentHistory.size();
    }

    // getter methods
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getId() { return id; }
    public int getImageId() { return imageId; }
    public String getName() {
        return name;
    }
    public String getDistanceUnits() { return distanceUnits; }
    public Point getTextPoint() { return textPoint; }
    public Point getImagePoint() { return imagePoint; }
    public double getDistance() { return distance; }
    public int getDegrees() { return degrees; }

    // setter methods
    public void setName(String s) { name = s; }
    public void setLatitude(double d) { latitude = d; }
    public void setLongitude(double d) { longitude = d; }
    public void setMarkerRotation(float newAngle) { marker.setRotation(newAngle); }
    public void setMarkerPosition(double lat, double lon) { marker.setPosition(new LatLng(lat,lon)); }
    public void setMarkerIcon(BitmapDescriptor b) { marker.setIcon(b); }
    public void removeMarker() { marker.remove(); }
}