package com.mindsoon.thataway;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import android.hardware.SensorManager;
import android.location.Location;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

public class MainActivity extends FragmentActivity implements OnClickListener,
        SensorEventListener,
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private GoogleMap map;
    private String theQuery;
    private String sessionId="";
    private Button themeToggle;
    private ImageButton compass;
    private ImageView mapToggle;
    private ImageView settingsToggle;
    private ImageView visibilityToggle;
    private ImageView open_x;
    private InputMethodManager imm;
    private SensorManager mSensorManager;
    private LinearLayout enterAddress;
    private LinearLayout spinner;
    private LinearLayout addressResults;
    private LinearLayout settingsBox;
    private LinearLayout aboutBox;
    private LinearLayout splashScreen;
    private LinearLayout hudButtons;
    private RelativeLayout background;
    private RelativeLayout appBackground;
    private RelativeLayout mapBackground;
    private EditText addressInput;
    private ProgressBar visibilityToggleSpinner;
    private float currentDegree = 0f;
    private int compassBackgroundHeight = 0;
    private int compassBackgroundTopMargin = 0;
    private int compassBackgroundBottomMargin = 0;
    private boolean mUpdatesRequested = false;
    private boolean blackTheme = true;
    private boolean isVisible = false;
    private boolean updateWhileMinimized = false;
    private Mark me;
    private final ArrayList<Integer> buttonTracker = new ArrayList<Integer>();
    private final ArrayList<Mark> queryTracker = new ArrayList<Mark>();
    private final ArrayList<Mark> places = new ArrayList<Mark>();
    private final ArrayList<Mark> people = new ArrayList<Mark>();
    private final ArrayList<Mark> hiddenPeople = new ArrayList<Mark>();
    private final ArrayList<History> recentHistory = new ArrayList<History>();
    private LocationRequest mLocationRequest;
    private LocationClient mLocationClient;
    private PendingIntent pi;
    private BroadcastReceiver br;
    private AlarmManager am;
    private long lastAlarm;
    public enum Options { SETTINGS, HUD }
    public enum Hud { COMPASS, MAP }
    public enum Status { OFF, ON, PAUSED }
    private Options options = Options.HUD;
    private Hud hud = Hud.COMPASS;
    private Status alarmStatus = Status.OFF;
    // constants
    private static final int ARROW_IMAGE_BUFFER = 40;
    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 2000;
    private static final int FAST_INTERVAL_CEILING_IN_MILLISECONDS = 1000;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getActionBar().hide();
        setupLocation();
        setLayout();

        background.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                appBackground.getWindowVisibleDisplayFrame(r);
                compassBackgroundTopMargin = r.top;
                compassBackgroundBottomMargin = r.bottom;
                int heightDifference = appBackground.getRootView().getHeight() -
                        compassBackgroundBottomMargin - compassBackgroundTopMargin;
                if (heightDifference > 300){
                    background.setBottom(compassBackgroundBottomMargin-compassBackgroundTopMargin-enterAddress.getHeight());
                    int heightOfKeyboard = appBackground.getBottom() - background.getBottom();
                    map.setPadding(0, 0, 0, heightOfKeyboard);
                } else if (isKeyboardHidden()) {
                    background.setBottom(background.getHeight());
                    map.setPadding(0, 0, 0, 0);
                    open_x.setVisibility(View.VISIBLE);
                }
                compassBackgroundHeight = background.getHeight();
                putCompassInMiddle(compassBackgroundHeight);
            }
        });

        addressInput.setOnClickListener(this);
        addressInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                hideAboutBox();
                if (event.getAction() != 0) {
                    return false;
                } else if ((addressInput.length() > 0) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    theQuery = addressInput.getText().toString();
                    showAddressResults();
                }
                return false;
            }
        });
    }

    // Initialize values for googlemaps location updates
    void setupLocation(){
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        mLocationClient = new LocationClient(this, this, this);
        mUpdatesRequested = false;
    }

    // If the background and appBackground are different, it means the keyboard is visible
    boolean isKeyboardHidden(){
        return background.getHeight() == appBackground.getHeight();
    }

    // Relocate position of compass, address bar, and hud buttons when keyboard shown
    public void putCompassInMiddle(int height){
        compass.setTop((height/2) - 200 );
        compass.setBottom((height / 2) + 200);
        enterAddress.setTop(height);
        enterAddress.setBottom(height + 128);
        if (isKeyboardHidden()) height -= hudButtons.getHeight();
        hudButtons.setTop(height);
        hudButtons.setBottom(height + hudButtons.getHeight());
        resizeAddressResults();
    }

    // Reset Top and Bottom of addressResults bar
    void resizeAddressResults(){
        addressResults.setTop(enterAddress.getTop() - addressResults.getHeight());
        addressResults.setBottom(enterAddress.getTop());
    }

    // Lock orientation and assign java objects to layout references
    void setLayout(){
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        background = (RelativeLayout) findViewById(R.id.compassBackground);
        appBackground = (RelativeLayout) findViewById(R.id.appBackground);
        mapBackground = (RelativeLayout) findViewById(R.id.mapBackground);
        spinner = (LinearLayout) findViewById(R.id.spinner);
        splashScreen = (LinearLayout) findViewById(R.id.splashScreen);
        visibilityToggleSpinner = (ProgressBar) findViewById(R.id.visibilityToggleSpinner);
        compass = (ImageButton) findViewById(R.id.compass);
        mapToggle = (ImageView) findViewById(R.id.mapToggle);
        themeToggle = (Button) findViewById(R.id.themeToggle);
        addressResults = (LinearLayout) findViewById(R.id.addressResults);
        aboutBox = (LinearLayout) findViewById(R.id.aboutBox);
        settingsBox = (LinearLayout) findViewById(R.id.settings);
        hudButtons = (LinearLayout) findViewById(R.id.hudButtons);
        settingsToggle = (ImageView) findViewById(R.id.settingsToggle);
        visibilityToggle = (ImageView) findViewById(R.id.visibilityToggle);
        open_x = (ImageView) findViewById(R.id.open_x);
        mapToggle.setBackgroundResource(R.drawable.img_pin_place);
        settingsToggle.setBackgroundResource(R.drawable.img_info_gray);
        visibilityToggle.setBackgroundResource(R.drawable.img_triangle_gray);
        enterAddress = (LinearLayout) findViewById(R.id.enterAddressLayout);
        addressInput = (EditText) findViewById(R.id.addressInput);
        compass.requestFocus();
        adjustInterfaceForSmallScreenSizes();
        setUpMapIfNeeded();
        me = new Mark(map);
    }

    // Reduce size of screen objects for small screen widths
    void adjustInterfaceForSmallScreenSizes(){
        Point size = new Point(0,0);
        getWindowManager().getDefaultDisplay().getSize(size);
        if (size.x < 1000){
            float redux = 0.75f;
            shrinkImage((ImageView)findViewById(R.id.closeAddressInputButton), redux);
            shrinkImage(visibilityToggle, redux);
            shrinkImage(mapToggle, redux);
            shrinkImage(settingsToggle, redux);
            shrinkImage(open_x, redux);
            findViewById(R.id.visibilityToggleLayout).setPadding(0, 0, 0, 0);
            findViewById(R.id.mapToggleLayout).setPadding(0, 0, 0, 0);
            findViewById(R.id.settingsToggleLayout).setPadding(0, 0, 0, 0);
            findViewById(R.id.closeAddressInput).setPadding(6, 6, 6, 6);
            addressInput.setTextSize(14);
        }
    }

    // Reduce an image to specified percentage
    void shrinkImage(ImageView i, float shrinkPercentage){
        i.setScaleX(shrinkPercentage);
        i.setScaleY(shrinkPercentage);
    }

    // Called when Activity is restarted, even before it becomes visible.
    @Override
    public void onStart(){
        super.onStart();
        mLocationClient.connect();
    }

    //Called when Activity is no longer visible to stop updates and disconnect.
    @Override
    public void onStop(){
        if (!updateWhileMinimized){
            if (mLocationClient.isConnected()) stopPeriodicUpdates();
            mLocationClient.disconnect();
        }
        super.onStop();
    }

    // Called when system detects that this Activity is now visible.
    @Override
    protected void onResume(){
        super.onResume();
        if (!updateWhileMinimized){
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                    SensorManager.SENSOR_DELAY_GAME);
            startMindsoonTimer(alarmStatus);
        }
        setUpMapIfNeeded();
    }

    // Called when system is paused/minimized/exited
    @Override
    protected void onPause(){
        super.onPause();
        if (!updateWhileMinimized){
            stopUpdates();
            mSensorManager.unregisterListener(this);
            stopMindsoonTimer(Status.PAUSED);
        }
    }

    // Called when alarm manager object ends
    @Override
    protected void onDestroy(){
        stopMindsoonTimer(Status.OFF);
        super.onDestroy();
    }

    // Only called once and when we are sure that {@link #mMap} is not null.
    private void setUpMap() {
        map.getUiSettings().setZoomControlsEnabled(false);
    }

    private void setUpMapIfNeeded() {
        if (map == null) {
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            if (map != null) setUpMap();
        }
    }

    // Toggle between map and compass view
    public void toggleMap(View v){
        if (hud.equals(Hud.COMPASS)){
            setHud(Hud.MAP);
            mapToggle.setBackgroundResource(R.drawable.img_compass_small);
            setUpMapIfNeeded();
            showMap();
        } else {
            setHud(Hud.COMPASS);
            mapToggle.setBackgroundResource(R.drawable.img_pin_place);
            showHud();
        }
    }

    // Cycle through Mark objects and show each one on screen
    void plotMarksOnBackground(float compassAngle, ArrayList<Mark> markList){
        for(final Mark aMark : markList){
            float plotDegree = (compassAngle + aMark.getDegrees()) % 360;
            updateMarkTextView(aMark, plotDegree);
            updateMarkImageView(aMark, plotDegree, compassAngle, aMark.getDegrees());
        }
    }

    // Rotate image by degrees
    void rotateImage(ImageView image, float compassAngle, float offset){
        RotateAnimation ra = new RotateAnimation(
                ( currentDegree - offset ) % 360,
                ( -compassAngle - offset ) % 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        ra.setDuration(210);
        ra.setFillAfter(true);
        image.startAnimation(ra);
    }

    // Update arrow position and rotation within boundaries of background
    void updateMarkImageView(Mark m, float plotDegree, float compassAngle, float offset) throws NullPointerException{
        m.getImagePoint().plotLocation (plotDegree, 0, 0,
                background.getWidth() - ARROW_IMAGE_BUFFER,
                compassBackgroundHeight - ARROW_IMAGE_BUFFER );
        ImageView image = (ImageView) findViewById(m.getImageId());
        RelativeLayout.LayoutParams imParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT );
        imParams.setMargins(m.getImagePoint().x, m.getImagePoint().y, 0, 0);
        image.setLayoutParams(imParams);
        rotateImage(image, compassAngle, offset);
    }

    // Update arrow position within boundaries of background
    void updateMarkTextView(Mark m, float plotDegree){
        m.getTextPoint().plotLocation (plotDegree, ARROW_IMAGE_BUFFER, ARROW_IMAGE_BUFFER,
                background.getWidth() - ARROW_IMAGE_BUFFER - ((findViewById(m.getId()))).getWidth(),
                compassBackgroundHeight - ARROW_IMAGE_BUFFER - ((findViewById(m.getId()))).getHeight() );
        TextView tv = (TextView) findViewById(m.getId());
        RelativeLayout.LayoutParams tvParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT );
        tvParams.setMargins(m.getTextPoint().x, m.getTextPoint().y, 0, 0);
        tv.setLayoutParams(tvParams);
        tv.setText(m.getName() + "\n" + (int) m.getDistance() + " " + m.getDistanceUnits());
    }

    // Async task that checks geocode and creates menu of query buttons
    void showAddressResults(){
        class getAddresses extends AsyncTask<String, Void, List<Address>> {
            String queryStatus;

            @Override
            protected void onPreExecute(){
                showSpinner();
            }

            @Override
            protected List<Address> doInBackground(String... urls){
                int maxSizeOfQuery = 5;
                List<Address> addressQuery = new ArrayList<Address>();
                if (isLocationServicesEnabled()){
                    try {
                        addressQuery = getGeocodeAddresses(maxSizeOfQuery);
                    } catch (IOException e){
                        queryStatus=getString(R.string.check_internet_connection);
                    }
                } else {
                    queryStatus=getString(R.string.enable_location_services);
                }
                return addressQuery;
            }

            protected void onPostExecute(List<Address> addressQuery){
                spinner.setVisibility(View.GONE);
                if (addressQuery.size() == 0){
                    if (queryStatus==null) queryStatus=getString(R.string.location_not_found);
                    if (buttonTracker.size()==0) addressResults.setVisibility(View.GONE);
                } else if (hud.equals(Hud.COMPASS)) {
                    for (Address anAddressQuery : addressQuery){
                        createQueryButton(anAddressQuery);
                    }
                } else {
                    for (Address anAddressQuery : addressQuery){
                        createQueryPin(anAddressQuery);
                    }
                }
                if (queryStatus!=null) showToast(queryStatus);
            }
        }
        getAddresses getQuery = new getAddresses();
        getQuery.execute();
    }

    // Creates map pin for each address
    void createQueryPin(final Address address){
        Mark newMark = new Mark(address, map);
        queryTracker.add(newMark);
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.getSnippet().equals(getString(R.string.search_result))){
                    ViewGroup v = (ViewGroup) background.getParent();
                    addNewPlace(v, marker);
                }
                return false;
            }
        });
        zoomToIncludeAllMarkers();
    }

    // Creates query button for each address
    void createQueryButton(final Address address){
        if (!queryButtonAreadyExists(address)){
            Button newButton = new Button(this);
            Integer id = address.hashCode();
            buttonTracker.add(id);
            newButton.setId(id);
            newButton.setTextColor(Color.parseColor("#2E9AFE"));
            newButton.setText(formatQueryButton(address));
            newButton.setBackgroundColor(Color.parseColor("#dddddd"));
            newButton.setPadding(20, 10, 20, 10);
            newButton.setWidth(background.getWidth());
            newButton.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
            newButton.setOnClickListener(new OnClickListener(){
                public void onClick(View v){
                    addNewPlace(v, address);
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT );
            newButton.setLayoutParams(params);
            addressResults.addView(newButton);
            resizeAddressResults();
        }
    }

    boolean queryButtonAreadyExists(Address a){
        String newButtonName = formatQueryButton(a);
        for(Integer i : buttonTracker){
            Button b = (Button) findViewById(i);
            if (newButtonName.equals(b.getText())) return true;
        }
        return false;
    }

    // Formats query button text depending on info in Address object
    private String formatQueryButton(Address address){
        if ( (address.getAddressLine(0) != null)
                && (address.getAddressLine(1) != null)
                && (address.getAddressLine(2) != null) )
            return address.getAddressLine(0) + ", "
                    + address.getAddressLine(1) + ", "
                    + address.getAddressLine(2);
        else if ( (address.getAddressLine(0) != null)
                && (address.getAddressLine(1) != null) )
            return address.getAddressLine(0) + ", "
                    + address.getAddressLine(1);
        else if ( address.getAddressLine(0) != null )
            return address.getAddressLine(0);
        else if ( (address.getFeatureName() != null) )
            return address.getFeatureName();
        else if ( (address.getLocality() != null) )
            return address.getLocality();
        else if ( (address.getCountryName() != null) )
            return address.getCountryName();
        else return getString(R.string.no_name_found);
    }

    // Remove all query buttons -- This is called when a query is chosen
    private void removeAllQueryButtons(){
        for(Integer i : buttonTracker){
            Button b = (Button) findViewById(i);
            ViewGroup layout = (ViewGroup) b.getParent();
            if(null!=layout) layout.removeView(b);
        }
        buttonTracker.clear();
        removeAllQueryPins();
    }

    // Remove all blue query pins on map
    private void removeAllQueryPins(){
        for(Mark m:queryTracker){
            m.removeMarker();
        }
        queryTracker.clear();
        zoomToIncludeAllMarkers();
    }

    // Create new Mark to track the chosen location from query menu
    void addNewPlace(View v, Marker marker){
        if (addressIsAlreadyTracked(marker.getPosition())) {
            showToast(getString(R.string.place_already_tracked));
        } else {
            findLocation();
            Mark newMark = new Mark(marker, me, map);
            newMarkVisualElements(newMark, false);
            places.add(newMark);
            hideEnterAddress(v);
            removeAllQueryButtons();
            if (hud.equals(Hud.MAP)) {
                zoomToIncludeAllMarkers();
            }
        }
    }

    // Create new Mark to track the chosen location from query menu
    void addNewPlace(View v, Address address){
        if (addressIsAlreadyTracked(address)) {
            showToast(getString(R.string.place_already_tracked));
        } else {
            findLocation();
            Mark newMark = new Mark(address, me, map);
            newMarkVisualElements(newMark, false);
            places.add(newMark);
            hideEnterAddress(v);
            removeAllQueryButtons();
            if (hud.equals(Hud.MAP)) {
                zoomToIncludeAllMarkers();
            }
        }
    }

    // Zoom to include user and all markers
    void zoomToIncludeAllMarkers(){
        double swLat = me.getLatitude(),
               neLat = me.getLatitude(),
               swLon = me.getLongitude(),
               neLon = me.getLongitude();
        ArrayList<Mark> allMarks = new ArrayList<Mark>();
        allMarks.addAll(places);
        allMarks.addAll(people);
        allMarks.addAll(queryTracker);
        for(Mark m:allMarks){
            swLat = Math.min(m.getLatitude(), swLat);
            swLon = Math.min(m.getLongitude(), swLon);
            neLat = Math.max(m.getLatitude(), neLat);
            neLon = Math.max(m.getLongitude(), neLon);
        }
        double latPad = Math.abs(swLat - neLat) * 0.15;
        double lonPad = Math.abs(swLon - neLon) * 0.15;
        LatLngBounds newBounds = new LatLngBounds(
                new LatLng(swLat - latPad, swLon - lonPad),
                new LatLng(neLat + latPad, neLon + lonPad));
        if ( (places.size()==0) && (people.size()==0) ) {
            updateMapCamera(me.getLatitude(), me.getLongitude(), map.getCameraPosition().zoom);
        } else {
            updateMapCamera(me.getLatitude(), me.getLongitude(), map.getCameraPosition().zoom);
            Point size = new Point(0,0);
            getWindowManager().getDefaultDisplay().getSize(size);
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(newBounds, size.x, size.y, 0));
        }
    }

    // Check if an address is in the ArrayList places -- Address input
    boolean addressIsAlreadyTracked(Address a){
        for(Mark m:places){
            if ((m.getLatitude()==a.getLatitude()) && (m.getLongitude()==a.getLongitude()))
                return true;
        }
        return false;
    }

    // Check if an address is in the ArrayList places -- LatLng input
    boolean addressIsAlreadyTracked(LatLng latLng){
        for(Mark m:places){
            if ((latLng.latitude==m.getLatitude()) && (latLng.longitude==m.getLongitude()))
                return true;
        }
        return false;
    }

    // Create visual elements for new Mark (whether place or person)
    void newMarkVisualElements(final Mark newMark, boolean isUser){
        TextView tv = new TextView(this);
        tv.setId(newMark.getId());
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        tv.setMinWidth(100);
        tv.setMaxWidth(270);
        tv.setMinHeight(70);
        tv.setMaxHeight(100);
        tv.setTextColor(trueForBlackFalseForWhite(!blackTheme));
        ImageView im = new ImageView(this);
        im.setId(newMark.getImageId());

        if (isUser){
            im.setBackgroundResource(getArrow(getString(R.string.person)));
            tv.setOnClickListener(new OnClickListener(){
                public void onClick(View v){
                    clickMark(newMark, people);
                }
            });
        } else {
            im.setBackgroundResource(getArrow(getString(R.string.place)));
            tv.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    clickMark(newMark, places);
                }
            });
        }

        background.addView(im);
        background.addView(tv);
    }

    // Create dialog box when a Mark object is tapped to remove it or remove all other objects
    void clickMark(final Mark aMark, final ArrayList<Mark> markList){
        TextView myMsg = new TextView(this);
        myMsg.setText("\n" + aMark.getName().replace("\n", " ") + " is " +
                +(int) aMark.getDistance() + " " + aMark.getDistanceUnits() + " away\n");
        myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
        myMsg.setTextSize(20);
        myMsg.setTypeface(myMsg.getTypeface(), Typeface.BOLD);
        new AlertDialog.Builder(this)
                .setView(myMsg)
                .setNegativeButton(R.string.stop_tracking, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeVisualsOfThisMark(aMark);
                        markList.remove(markList.indexOf(aMark));
                        hiddenPeople.add(aMark);
                    }
                })
                .setNeutralButton(R.string.only_track, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton){
                        removeAllMarksButThisOne(aMark, markList);
                    }
                })
                .setPositiveButton(getString(R.string.OK), null).show();
    }

    // Remove visuals of a specific Mark
    void removeVisualsOfThisMark(Mark aMark){
        TextView tv = (TextView) findViewById(aMark.getId());
        ((RelativeLayout) tv.getParent()).removeView(tv);
        ImageView im = (ImageView) findViewById(aMark.getImageId());
        ((RelativeLayout) im.getParent()).removeView(im);
        aMark.removeMarker();
    }

    // Remove all Marks except for a specified one (if person, move to hiddenPeople list)
    void removeAllMarksButThisOne(Mark aMark, ArrayList<Mark> markList){
        for(Mark thisMark : people)
            if (!thisMark.equals(aMark)){
                removeVisualsOfThisMark(thisMark);
                hiddenPeople.add(thisMark);
            }
        for(Mark thisMark : places)
            if (!thisMark.equals(aMark)){
                removeVisualsOfThisMark(thisMark);
            }
        people.clear();
        places.clear();
        markList.add(aMark);
    }

    // Triggered when button is toggled to make device visible and start updating from mindsoon
    public void toggleVisible(View v){
        if (!isVisible){
            startUpdates();
            getMindsoonUpdates("newSession");
        } else {
            openVisibleOptionsDialog();
        }
        hideAboutBox();
    }

    void openVisibleOptionsDialog(){
        TextView myMsg = new TextView(this);
        myMsg.setText(getString(R.string.you_are_visible_as) + me.getName().replace("\n", " ") + "\n");
        myMsg.setGravity(Gravity.CENTER_HORIZONTAL);
        myMsg.setTextSize(20);
        myMsg.setTypeface(myMsg.getTypeface(), Typeface.BOLD);
        new AlertDialog.Builder(this)
                .setView(myMsg)
                .setNegativeButton(R.string.go_invisible, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeAllUsers();
                        stopMindsoonTimer(Status.OFF);
                        getMindsoonUpdates("endSession");
                    }
                })
                .setNeutralButton(R.string.change_name, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton){
                        inputNewName();
                    }
                })
                .setPositiveButton(getString(R.string.OK), null).show();
    }

    void inputNewName(){
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setTextSize(20);
        input.setTypeface(input.getTypeface(), Typeface.BOLD);
        input.setText(me.getName());
        input.setSelection(input.length());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setOnKeyListener (new DialogInterface.OnKeyListener () {
            @ Override
            public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event)
            {
                if ( (input.length() > 0) && (keyCode == KeyEvent.KEYCODE_ENTER) ){
                    submitNewName(input.getText().toString());
                    dialog.dismiss();
                    return true;
                }
                else return false;
            }
        })
                .setView(input)
                .setPositiveButton(R.string.change_name, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        submitNewName(input.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, null).show();
    }

    void submitNewName(String newName){
        try {
            newName = URLEncoder.encode(newName, "UTF-8");
            getMindsoonUpdates("newName&newName=" + newName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // Setup timer to trigger mindsoon update
    private void setupAlarm(){
        if (!alarmStatus.equals(Status.OFF)){
            br = new BroadcastReceiver(){
                @Override
                public void onReceive(Context c, Intent i){
                    if (isVisible) getMindsoonUpdates("getUpdate");
                }
            };
            registerReceiver(br, new IntentFilter("mindsoonUpdate") );
            pi = PendingIntent.getBroadcast(this, 0, new Intent("mindsoonUpdate"), 0 );
            am = (AlarmManager)(this.getSystemService( Context.ALARM_SERVICE ));
        }
    }

    // Start timer that triggers next mindsoon update
    void startMindsoonTimer(Status newStatus){
        if ( newStatus.equals(Status.PAUSED) || newStatus.equals(Status.ON) ) {
            alarmStatus = Status.ON;
        } else {
            alarmStatus = Status.OFF;
        }
        if (isVisible){
            long interval = 2000;     // milliseconds
            setupAlarm();
            lastAlarm = System.currentTimeMillis();
            am.cancel(pi);
            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + interval, interval, pi);
        }
    }

    // Stop timer that triggers next mindsoon update
    void stopMindsoonTimer(Status newStatus){
        if ( ( alarmStatus.equals(Status.ON) ) && (br != null) ){
            unregisterReceiver(br);
            am.cancel(pi);
        }
        if ( !alarmStatus.equals(Status.OFF) ) {
            alarmStatus = newStatus;
        }
    }

    // Async task that sends GET request to server, then processes JSON response
    void getMindsoonUpdates(final String req){

        class HttpGetAsyncTask extends AsyncTask<String, Void, JSONObject> {

            @Override
            protected void onPreExecute(){
                if (!req.equals("getUpdate")){
                    visibilityToggle.setVisibility(View.GONE);
                    visibilityToggleSpinner.setVisibility(View.VISIBLE);
                }
            }

            @Override
            protected JSONObject doInBackground(String... strings){
                findLocation();
                String url = getString(R.string.base_url) +
                        "&req=" + req +
                        "&lat=" + me.getLatitude() +
                        "&lon=" + me.getLongitude() +
                        "&sessionId=" + sessionId;
                Log.d("(last request "+(System.currentTimeMillis()-lastAlarm) + " milliseconds ago)","sent: "+url);
                lastAlarm = System.currentTimeMillis();
                JSONObject returnedData = new JSONObject();
                try {
                    HttpClient client = new DefaultHttpClient();
                    final HttpParams httpParameters = client.getParams();
                    HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
                    HttpConnectionParams.setSoTimeout(httpParameters, 10000);
                    HttpGet get = new HttpGet(url);
                    HttpResponse responseGet = client.execute(get);
                    HttpEntity resEntityGet = responseGet.getEntity();
                    if (resEntityGet != null){
                        String response = EntityUtils.toString(resEntityGet);
                        returnedData = new JSONObject(response);
                    }
                } catch (SocketTimeoutException e){
                    if (req.equals("newSession")) showToast(getString(R.string.long_time_no_response));
                } catch (IOException e){
                    if (req.equals("newSession")) showToast(getString(R.string.long_time_no_response));
                } catch (Exception e){
                    e.printStackTrace();
                }
                return returnedData;
            }

            protected void onPostExecute(JSONObject serverResponse){
                Log.d("(package took "+(System.currentTimeMillis()-lastAlarm) + " milliseconds)","received: "+serverResponse.toString());
                try {
                    if (serverResponse.getString("status").equals("sessionUpdated")) {
                        updateUsers(serverResponse.getJSONArray("people"));
                    } else if (serverResponse.getString("status").equals("nameUpdated")){
                        me.setName(serverResponse.getString("sessionName"));
                    } else if (serverResponse.getString("status").equals("sessionCreated")){
                        isVisible = true;
                        sessionId = serverResponse.getString("sessionId");
                        me.setName(serverResponse.getString("sessionName"));
                        visibilityToggle.setBackgroundResource(R.drawable.img_triangle_red);
                        newUsers(serverResponse.getJSONArray("people"));
                        startMindsoonTimer(MainActivity.Status.ON);
                        openVisibleOptionsDialog();
                    } else if (serverResponse.getString("status").equals("sessionRemoved")){
                        isVisible = false;
                        sessionId=null;
                        me.setName(null);
                        visibilityToggle.setBackgroundResource(R.drawable.img_triangle_gray);
                    } else if (serverResponse.getString("status").equals("nameTaken")){
                        showToast(getString(R.string.name_taken));
                    }
                } catch (JSONException e){
                    showToast(getString(R.string.check_internet_connection));
                }
                visibilityToggle.setVisibility(View.VISIBLE);
                visibilityToggleSpinner.setVisibility(View.GONE);
            }
        }

        if (servicesConnected()) {
            HttpGetAsyncTask aTask = new HttpGetAsyncTask();
            aTask.execute();
        }
    }

    // Clear existing people, then create Mark objects & visual elements from list of people from JSON response
    void newUsers(JSONArray jsonArray) throws JSONException {
        for(Mark thisMark : people) removeVisualsOfThisMark(thisMark);
        people.clear();
        if (jsonArray.length() > 0){
            for (int i = 0; i < jsonArray.length(); i++){
                addThisUser(jsonArray.getJSONObject(i));
            }
        }
    }

    // Update Mark objects per JSON server response
    void updateUsers(JSONArray jsonArray) throws JSONException {
        if (jsonArray.length() > 0){
            for (int i = 0; i < jsonArray.length(); i++){
                JSONObject json = jsonArray.getJSONObject(i);
                boolean isFound = updateIfUserAlreadyTracked(json);
                if (!isFound) isFound = checkIfUserIsHidden(json);
                if (!isFound) addThisUser(json);
            }
        }
        deleteUsersNotInJsonArray(jsonArray);
    }

    // If JSON object already exists in Arraylist people, update its latitude/longitude and return true
    boolean updateIfUserAlreadyTracked(JSONObject json) throws JSONException {
        boolean isFound = false;
        for(Mark m : people){
            if (json.getString("sessionName").equals(m.getName())){
                m.setLatitude(json.getDouble("latitude"));
                m.setLongitude(json.getDouble("longitude"));
                m.setMarkerPosition(m.getLatitude(),m.getLongitude());
                isFound = true;
                break;
            }
        }
        return isFound;
    }

    // If JSON object already exists in Arraylist hiddenUsers, return true
    boolean checkIfUserIsHidden(JSONObject json) throws JSONException {
        boolean isFound = false;
        for (Mark m : hiddenPeople)
            if (json.getString("sessionName").equals(m.getName())){
                isFound = true;
                break;
            }
        return isFound;
    }

    // Remove all Marks from people list
    void removeAllUsers(){
        for(Mark m : people) removeVisualsOfThisMark(m);
        people.clear();
    }

    // Create new Mark per JSON input
    void addThisUser(JSONObject json) throws JSONException {
        Mark m = new Mark(json, me, map);
        newMarkVisualElements(m, true);
        people.add(m);
    }

    // Delete any Marks in people Arraylist if they're not in the JSON input
    void deleteUsersNotInJsonArray(JSONArray jsonArray) throws JSONException {
        ArrayList<Mark> toDelete = new ArrayList<Mark>();
        for(Mark m : people){
            boolean isFound = false;
            if (jsonArray.length() > 0){
                for (int i = 0; i < jsonArray.length(); i++){
                    if (jsonArray.getJSONObject(i).getString("sessionName").equals(m.getName())){
                        isFound = true;
                    }
                }
            }
            if (!isFound) toDelete.add(m);
        }
        for(Mark m : toDelete){
            removeVisualsOfThisMark(m);
            people.remove(people.indexOf(m));
        }
    }

    // Hide elements when background clicked
    public void backgroundClicked(View v){
        if (options.equals(Options.SETTINGS)) {
            showHud();
        } else {
            hideAboutBox();
            hideEnterAddress(v);
            hideSplash(v);
        }
    }

    // Show input components: keyboard and edittext
    public void compassClicked(View v){
        if (isKeyboardHidden()){
            showKeyboard();
            enterAddress.setVisibility(View.VISIBLE);
            startUpdates();
        } else {
            hideEnterAddress(v);
        }
        hideSplash(v);
    }

    public void hideSplash(View v){
        splashScreen.setVisibility(View.GONE);
    }

    // Called when settings button is tapped
    public void toggleSettings(View v) {
        if (options.equals(Options.HUD)){
            showSettingsBox();
        } else {
            showHud();
        }
    }

    public void aboutClicked(View v){
        aboutBox.setVisibility(View.GONE);
        showSettingsBox();
    }

    void showSettingsBox(){
        options = Options.SETTINGS;
        background.setVisibility(View.GONE);
        addressResults.setVisibility(View.GONE);
        int topPadding = (int) (background.getHeight() * .15 );
        int lrPadding = (background.getWidth() - 720) / 2;
        settingsBox.setPadding(lrPadding,topPadding,lrPadding,20);
        settingsBox.setVisibility(View.VISIBLE);
    }

    void setHud(Hud newHud){
        hud = newHud;
        options = Options.HUD;
    }

    void showHud(){
        if (hud.equals(Hud.COMPASS)){
            HideHudElementsExceptThis(background);
        } else {
            HideHudElementsExceptThis(mapBackground);
        }
        setHud(hud);
    }

    void showMap(){
        setHud(Hud.MAP);
        HideHudElementsExceptThis(mapBackground);
        findLocation();
        me.setMarkerPosition(me.getLatitude(), me.getLongitude());
        me.setMarkerRotation(currentDegree);
        me.setMarkerIcon(BitmapDescriptorFactory.fromResource(R.drawable.img_map_arrow));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(me.getLatitude(), me.getLongitude()), 16f));
        zoomToIncludeAllMarkers();
    }

    // Hide all major hud elements except the input Layout
    void HideHudElementsExceptThis(RelativeLayout keepThisVisible){
        background.setVisibility(View.GONE);
        aboutBox.setVisibility(View.GONE);
        settingsBox.setVisibility(View.GONE);
        mapBackground.setVisibility(View.GONE);
        keepThisVisible.setVisibility(View.VISIBLE);
    }

    // Show about box and hide background
    public void showAboutBox(View v){
        background.setVisibility(View.GONE);
        addressResults.setVisibility(View.GONE);
        int topPadding = (int) (background.getHeight() * .15 );
        int lrPadding = (background.getWidth() - 720) / 2;
        aboutBox.setPadding(lrPadding, topPadding, lrPadding, 20);
        aboutBox.setVisibility(View.VISIBLE);
    }

    // Hide about box and show background
    void hideAboutBox(){
        background.setVisibility(View.VISIBLE);
        aboutBox.setVisibility(View.GONE);
    }

    // Clear and show keyboard, also double-request focus to ensure cursor appears
    void showKeyboard(){
        hideAboutBox();
        open_x.setVisibility(View.GONE);
        addressInput.requestFocus();
        addressInput.requestFocus();
        imm.showSoftInput(addressInput, InputMethodManager.SHOW_IMPLICIT);
        if ( addressInput.getText().length() > 0 ){
            addressInput.setText("");
        }
    }

    // Show full compass by hiding input components
    void hideEnterAddress(View v){
        hideAboutBox();
        enterAddress.setVisibility(View.GONE);
        addressResults.setVisibility(View.GONE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        open_x.setVisibility(View.VISIBLE);
    }

    // Hide compass, show spinner
    void showSpinner(){
        addressResults.setVisibility(View.VISIBLE);
        compass.setVisibility(View.GONE);
        spinner.setVisibility(View.VISIBLE);
    }

    // Generic toast display.
    void showToast(final String toast){
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void updateWhileMinimized(View v){
        updateWhileMinimized = !updateWhileMinimized;
    }

    // Swap between white and black theme
    public void changeTheme(View v){
        blackTheme = !blackTheme;
        changeArrowsToTheme();
        background.setBackgroundColor(trueForBlackFalseForWhite(blackTheme));
        settingsBox.setBackgroundColor(trueForBlackFalseForWhite(blackTheme));
        if (blackTheme) themeToggle.setText(getString(R.string.black_theme_text));
        else themeToggle.setText(getString(R.string.white_theme_text));
    }

    // Returns arrow image: RED for user, GRAY for place, also themed white or black
    int getArrow(String thisObjectIsWhat){
        if      ( (blackTheme) && (thisObjectIsWhat.equals(getString(R.string.person))) )  return R.drawable.img_arrow_person;
        else if ( (blackTheme) && (thisObjectIsWhat.equals(getString(R.string.place))) )   return R.drawable.img_arrow_place;
        else if ( (!blackTheme) && (thisObjectIsWhat.equals(getString(R.string.person))) ) return R.drawable.img_arrow_person_white;
        else if ( (!blackTheme) && (thisObjectIsWhat.equals(getString(R.string.place))) )  return R.drawable.img_arrow_place_white;
        return 0;
    }

    // Return parsed color int for white or black
    int trueForBlackFalseForWhite(Boolean b){
        if (b) return Color.parseColor("#000000");
        else return Color.parseColor("#ffffff");
    }

    // Swap coloration for white or black color theme
    void changeArrowsToTheme(){
        for (Mark m:people) {
            ImageView im = (ImageView) findViewById(m.getImageId());
            im.setBackgroundResource(getArrow(getString(R.string.person)));
            TextView tv = (TextView) findViewById(m.getId());
            tv.setTextColor(trueForBlackFalseForWhite(!blackTheme));
        }
        for (Mark m:places) {
            ImageView im = (ImageView) findViewById(m.getImageId());
            im.setBackgroundResource(getArrow(getString(R.string.place)));
            TextView tv = (TextView) findViewById(m.getId());
            tv.setTextColor(trueForBlackFalseForWhite(!blackTheme));
        }
    }

    public void trackCurrentLocation(View v){
        findLocation();
        nameTheNewLocation();
    }

    void nameTheNewLocation(){
        final EditText input = new EditText(this);
        input.setSingleLine();
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setTextSize(20);
        input.setTypeface(input.getTypeface(), Typeface.BOLD);
        input.setSelection(input.length());
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setOnKeyListener (new DialogInterface.OnKeyListener () {
            @ Override
            public boolean onKey (DialogInterface dialog, int keyCode, KeyEvent event)
            {
                if ( (input.length() > 0) && (keyCode == KeyEvent.KEYCODE_ENTER) ){
                    createNewLocation(input.getText().toString());
                    dialog.dismiss();
                    return true;
                }
                else return false;
            }
        })
                .setTitle(R.string.enter_location_name)
                .setView(input)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        createNewLocation(input.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        hideEnterAddress(findViewById(android.R.id.content));
                    }
                })
                .show();
    }

    void createNewLocation(String newLocationName){
        Mark m = new Mark(newLocationName,me,map);
        newMarkVisualElements(m, false);
        places.add(m);
        showHud();
    }

    // Check if device has location services enabled
    boolean isLocationServicesEnabled(){
        LocationManager manager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return (isGpsEnabled) || (isNetworkEnabled);
    }

    // Update the Mark object called me with current latitude/longitude
    void findLocation(){
        if (servicesConnected()){
            storeLocation(mLocationClient.getLastLocation());
        } else {
            startUpdates();
        }
    }

    // Update angle of compass and location of people/place screen objects when device is turned.
    @Override
    public void onSensorChanged(SensorEvent event) throws NullPointerException {
        float compassAngle = Math.round(event.values[0]);
        if (hud.equals(Hud.COMPASS)){
            plotMarksOnBackground(compassAngle, places);
            plotMarksOnBackground(compassAngle, people);
            rotateImage(compass,compassAngle,0);
            currentDegree = -compassAngle;
        } else {
            me.setMarkerPosition(me.getLatitude(), me.getLongitude());
            me.setMarkerRotation(compassAngle);
        }
    }

    // Query Geocode and get a list of Address objects
    List<Address> getGeocodeAddresses(int numberOfEntries) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = geocoder.getFromLocationName(theQuery, numberOfEntries);
        if ( (servicesConnected()) && (addresses != null) ){
            return addresses;
        } else {
            return null;
        }
    }

    // If services are connected and requested, start regular updates
    void startUpdates(){
        mUpdatesRequested = true;
        if (servicesConnected()){
            startPeriodicUpdates();
        }
    }

    // If services are connected, stop regular updates
    void stopUpdates(){
        mUpdatesRequested = false;
        if (servicesConnected()){
            stopPeriodicUpdates();
        }
    }

    // Add current location to arraylist of locations and increment index by 1
    void storeLocation(Location location){
        int timeStamp = recentHistory.size();
        int historySize = 2;
        if (recentHistory.size() == historySize) {
            timeStamp = recentHistory.get(historySize -1).getTime() + 1;
            recentHistory.remove(recentHistory.get(0));
        }
        History history = new History(location.getLatitude(), location.getLongitude(), timeStamp);
        recentHistory.add(history);
        me.approximateCurrentLocation(recentHistory);
    }

    // If sensor detects latitude/longitude change, update calculations for distance/degrees
    @Override
    public void onLocationChanged(Location location){
        storeLocation(location);
        for(Mark aMark : people) aMark.updatePosition(me);
        for(Mark aMark : places) aMark.updatePosition(me);
    }

    void updateMapCamera(double lat, double lon, float zoom){
        me.setMarkerPosition(lat, lon);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), zoom));
    }

    // In response to a request to start updates, send a request to Location Services
    private void startPeriodicUpdates(){
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    // In response to a request to stop updates, send a request to Location Services
    private void stopPeriodicUpdates(){
        mLocationClient.removeLocationUpdates(this);
    }

    // Handle results returned by other Activities started with startActivityForResult() such as onConnectionFailed()
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){

        switch (requestCode){
            // If the request code matches the code sent in onConnectionFailed
            case CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode){
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:
                        Log.d("gmaps", getString(R.string.resolved));
                        break;
                    // If any other result was returned by Google Play services
                    default:
                        Log.d("gmaps", getString(R.string.no_resolution));
                        break;
                }

                // If any other request code was received
            default:
                Log.d("gmaps", getString(R.string.unknown_activity_request_code, requestCode));
                break;
        }
    }

    // Verify that Google Play services is available
    private boolean servicesConnected(){

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode){
            Log.d("gmaps", getString(R.string.play_services_available));
            return true;
            // Google Play services was not available for some reason
        } else {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null){
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), "gmaps");
            }
            return false;
        }
    }

    // Called by Location Services when the request to connect the client finishes successfully.
    @Override
    public void onConnected(Bundle bundle){
        if (mUpdatesRequested){
            startPeriodicUpdates();
        }
    }

    // Called by Location Services if the attempt to Location Services fails.
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult){

        // Google Play services resolves the errors it detects
        if (connectionResult.hasResolution()){
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);

                // Thrown if Google Play services canceled the original PendingIntent
            } catch (IntentSender.SendIntentException e){
                e.printStackTrace();
            }
        } else {
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    // Show a dialog returned by Google Play services for the connection error code
    private void showErrorDialog(int errorCode){

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this,
                CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null){
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            errorFragment.setDialog(errorDialog);
            errorFragment.show(getSupportFragmentManager(), "gmaps");
        }
    }

    @Override       // not in use
    public void onAccuracyChanged(Sensor sensor, int accuracy){}

    @Override      // not in use
    public void onDisconnected(){}

    @Override      // not in use
    public void onClick(View v){
    }
}