package com.archivouni.guiauniversitaria

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.Exception

class MapsActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener{

    companion object {
        private const val TAG = "MapsActivity"

        private const val PERMISSION_FINE_LOCATION_REQUEST_CODE = 1
        private const val REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates"

        private const val REQUEST_LOCATION_UPDATE_MIN_INTERVAL = 5L
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 20f
    }

    //region Activity Variables
    private lateinit var mMap: GoogleMap

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mGoogleApiClient: GoogleApiClient
    private var mLastKnownLocation: Location? = null
    private var mLastKnownLatLng: LatLng? = null
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mLocationManager: LocationManager
    private var mIsGpsEnabled = false
    private var mIsNetworkEnabled = false
    private var mCanGetLocation = false
    private var mRequestingLocationUpdates = false
    private var mLocationPermissionDenied = false

    private lateinit var mListView: View
    private lateinit var mListViewBehavior: BottomSheetBehavior<*>

    private lateinit var mInfoView: View
    private lateinit var mInfoViewBehavior: BottomSheetBehavior<*>

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RecyclerView.Adapter<*>
    private lateinit var mViewManager: RecyclerView.LayoutManager

    private lateinit var mInfoRouteButton: ImageButton

    private lateinit var mData: Array<Marker?>
    //endregion

    //region Activity Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        updateValuesFromBundle(savedInstanceState)
        //region Map Logic
        createLocationCallback()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //endregion

        /*****************BOTTOM_SHEET BEGINS**********************/
        //region POI List View
        mListView = findViewById(R.id.list_view)
        mListViewBehavior = BottomSheetBehavior.from(mListView).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = Util.LIST_VIEW_PEEK_HEIGHT
        }
        //endregion

        mInfoView = findViewById(R.id.info_view)
        mInfoViewBehavior = BottomSheetBehavior.from(mInfoView).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = Util.INFO_VIEW_PEEK_HEIGHT
            setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(view: View, slideOffset: Float) {
                    mMap.setPadding(0,0,0,0)
                }

                @SuppressLint("SwitchIntDef")
                override fun onStateChanged(view: View, newState: Int) {

//                    when (newState) {
//                        BottomSheetBehavior.STATE_HIDDEN -> {
//                            mMap.setPadding(0, 0, 0, 0)
//                            if (Util.focusedMarker != null) {
//                                Util.focusedMarker!!.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
//                            }
//                        }
//                    }
                }

            })
        }

        /*****************SEARCH_BUTTON BEGINS**********************/
        //region Search Button
        val searchButton = findViewById<View>(R.id.search_button)
        searchButton.setOnClickListener {
            // Open list view on click
            mListViewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        //endregion

        /*****************SEARCH_BAR BEGINS**********************/
        //region Search Logic
        val searchBar = findViewById<SearchView>(R.id.search_bar)
        // TODO: Implement search logic here
        //endregion

    }

    override fun onStart() {
        mGoogleApiClient.connect()
        super.onStart()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        outState?.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onResume() {
        super.onResume()
        if (mRequestingLocationUpdates && !mLocationPermissionDenied)
            startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
    //endregion

    //region Map Functions
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mData = Response(resources.openRawResource(R.raw.poi).bufferedReader().use { it.readText() }).data.map { poi ->
            if (poi.latLng != null)
                mMap.addMarker(MarkerOptions().position(poi.latLng)).apply { tag = poi }
            else
                null
        }.toTypedArray()
        Log.d(TAG, "POIs read from json: ${mData.size}")

        /*****************RECYCLER_VIEW BEGINS**********************/
        /**
         * Recycler view is initialized here because ListAdapter requires that the map be
         * initialized in order to bind list items to their position on the map.
         */
        mViewManager = LinearLayoutManager(this)
        mViewAdapter = ListAdapter(mData, mMap, mInfoView, mListViewBehavior, mInfoViewBehavior)
        mRecyclerView = findViewById<RecyclerView>(R.id.recycler_view_list).apply {
            // Recycler view options
            setHasFixedSize(true)
            layoutManager = mViewManager
            adapter = mViewAdapter
        }

        /*****************MY_LOCATION LOGIC BEGINS**********************/
//        TODO: Change from get_last_location to get_location_updates

        enableMyLocation()
//        getLocation()

        /*****************MAP_OPTIONS BEGINS**********************/
        mMap.uiSettings.isMapToolbarEnabled = false
        //region All options for map go here
        // Start with empty map
//        mMap.mapType = GoogleMap.MAP_TYPE_NONE
        // Add tile overlay
//        mMap.addTileOverlay(TileOverlayOptions().tileProvider(GoogleMapsTileProvider(resources.assets)))
        // Set bounds for camera
        val uprBounds = LatLngBounds(LatLng(Util.UPR_BOUND_S, Util.UPR_BOUND_W), LatLng(Util.UPR_BOUND_N, Util.UPR_BOUND_E))
        mMap.setLatLngBoundsForCameraTarget(uprBounds)
        // Open camera at LatLng specified by upr
        val upr = LatLng(Util.DEFAULT_LATITUDE, Util.DEFAULT_LONGITUDE)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(upr, Util.DEFAULT_ZOOM))
        // Limit zoom
        mMap.setMinZoomPreference(Util.MIN_ZOOM)
        mMap.setMaxZoomPreference(Util.MAX_ZOOM)

        mMap.setOnMarkerClickListener(this)
        //endregion
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        Log.d(TAG, "Focused marker: ${if (Util.focusedMarker != null) (Util.focusedMarker!!.tag as PointOfInterest).name else "none"}")
        if (Util.focusedMarker != null) {
            Util.focusedMarker!!.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        }
        marker!!.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

        val poi = marker.tag as PointOfInterest

        if (mCanGetLocation) {
            mInfoRouteButton = findViewById(R.id.info_route_button)
            if (mLastKnownLatLng != null) {
                mInfoRouteButton.setOnClickListener {
                    bindRouteToButton(it as ImageButton, mLastKnownLatLng!!, poi.latLng!!)
                }
            }
        }
        Util.bindInfoToView(marker.tag as PointOfInterest, mInfoView, mMap)
        Util.setPaddingAfterLayout(mInfoView, mMap, marker.position)



        mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        mInfoViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        Util.focusedMarker = marker
        return true
    }
    //endregion

    //region Permission Functions
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_FINE_LOCATION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION))
                    enableMyLocation()
                else
                    mLocationPermissionDenied = true
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (mLocationPermissionDenied) {
            showMissingPermissionError()
            mLocationPermissionDenied = false
        }
    }

    private fun showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog().newInstance(true)
                .show(supportFragmentManager, "dialog")
    }
    //endregion

    //region Location functions
    @SuppressLint("MissingPermission")
    private fun getLocation(): Location? {
        try {
            mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // getting GPS status
            mIsGpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            // getting network status
            mIsNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            // no network provider is enabled
            if (!mIsGpsEnabled && !mIsNetworkEnabled) {
                // TODO: Ask to turn on location services
            } else {
                mCanGetLocation = true
                if (mIsNetworkEnabled) {
                    mLastKnownLocation = null
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            REQUEST_LOCATION_UPDATE_MIN_INTERVAL * 1000L,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            this)
                    Log.d("Network", "Network Enabled")
                    if (mLocationManager != null) {
                        mLastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (mLastKnownLocation != null) {
                            mLastKnownLatLng = LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude)
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (mIsGpsEnabled) {
                    if (mLastKnownLocation == null) {
                        mLocationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                REQUEST_LOCATION_UPDATE_MIN_INTERVAL,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES,
                                this)
                        Log.d("GPS", "GPS Enabled")
                        if (mLocationManager != null) {
                            mLastKnownLocation = mLocationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            if (mLastKnownLocation != null) {
                                mLastKnownLatLng = LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude)
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return mLastKnownLocation
    }

    /**
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, PERMISSION_FINE_LOCATION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true)
        } else {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                locationResult.locations.forEach {
                    mLastKnownLocation = it
                }
            }
        }
    }

    private fun startLocationUpdates() {
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    Looper.myLooper())
        } catch (e: SecurityException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        savedInstanceState ?: return

        if (savedInstanceState.containsKey(REQUESTING_LOCATION_UPDATES_KEY)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY)
        }
    }
    //endregion

    fun bindRouteToButton(view: ImageButton, origin: LatLng, dest: LatLng) {
        val url = getDirectionsUrl(origin, dest)
        view.setOnClickListener {
            Util.DownloadTask(mMap).execute(url)
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val strOrigin = "origin=${origin.latitude},${origin.longitude}"
        val strDest = "destination=${dest.latitude},${dest.longitude}"
        val mode = "mode=walking"
        val params = "$strOrigin&$strDest&$mode"
        val output = "json"
        return "${Util.GOOGLE_API_URL}$output?$params&key=" +
                packageManager.getApplicationInfo(packageName,
                        PackageManager.GET_META_DATA)
                        .metaData
                        .getString("com.google.android.geo.API_KEY")
    }




//    inner class LastLocation: AsyncTask<Void, Void, Location>() {
//        override fun doInBackground(vararg params: Void?): Location {
//        }
//    }

    override fun onStop() {
        mGoogleApiClient.disconnect()
        super.onStop()
    }

    override fun onProviderDisabled(provider: String?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onLocationChanged(p0: Location?) {
        if (p0 != null) {
            mLastKnownLocation = p0
            mLastKnownLatLng = LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude)
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnected(p0: Bundle?) {
        getLocation()
    }
}