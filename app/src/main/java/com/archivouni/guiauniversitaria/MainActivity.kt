package com.archivouni.guiauniversitaria

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.PersistableBundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class MainActivity : AppCompatActivity(),
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener {

    companion object {
        private const val TAG = "MainActivity"

        private const val PERMISSION_FINE_LOCATION_REQUEST_CODE = 1
        private const val REQUESTING_LOCATION_UPDATES_KEY = "requesting_location_updates"

        private const val REQUEST_LOCATION_UPDATE_MIN_INTERVAL = 5L
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 20f

        const val DEFAULT_ZOOM = 16.15f
        const val MIN_ZOOM = 16.15f
        const val MAX_ZOOM = 19f
        const val FOCUS_ZOOM = 17.5f

        const val DEFAULT_LATITUDE = 18.404123
        const val DEFAULT_LONGITUDE = -66.048714

        const val UPR_BOUND_S = 18.39926710
        const val UPR_BOUND_W = -66.05599693
        const val UPR_BOUND_N = 18.41188018
        const val UPR_BOUND_E = -66.03826031

        private const val IMAGE_WIDTH = 700
        private const val IMAGE_HEIGHT = 500
        private const val IMAGE_FIT_TO_VIEW = false

        private const val INFO_VIEW_PEEK_HEIGHT = 900
        private const val LIST_VIEW_PEEK_HEIGHT = 600
    }

    //region Activity Variables
    private lateinit var mMap: GoogleMap

    private var mapBounds = LatLngBounds(LatLng(UPR_BOUND_S, UPR_BOUND_W),
            LatLng(UPR_BOUND_N, UPR_BOUND_E))

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
    private lateinit var mListViewButton: Button

    private lateinit var mInfoView: View
    private lateinit var mInfoViewBehavior: BottomSheetBehavior<*>
    private var mInfoViewHeight = 0

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RecyclerView.Adapter<*>
    private lateinit var mViewManager: RecyclerView.LayoutManager

    private lateinit var mSettingsButton: FloatingActionButton

    private lateinit var mInfoRouteButton: ImageButton

    private lateinit var mData: Array<Marker?>

    private var mFocusedMarker: Marker? = null
    //endregion

    //region Public functions
    fun bindRouteToButton(view: ImageButton, origin: LatLng, dest: LatLng) {
        val url = getDirectionsUrl(origin, dest)
        view.setOnClickListener {
            Util.DownloadTask(mMap).execute(url)
        }
    }
    //endregion

    //region Activity Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateValuesFromBundle(savedInstanceState)
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

        /*****************LIST VIEW BEGINS**********************/
        mListView = findViewById(R.id.list_view)
        mListViewBehavior = BottomSheetBehavior.from(mListView).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = LIST_VIEW_PEEK_HEIGHT
        }

        /*****************INFO VIEW BEINS***********************/
        mInfoView = findViewById(R.id.info_view)
        mInfoViewBehavior = BottomSheetBehavior.from(mInfoView).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = INFO_VIEW_PEEK_HEIGHT
            setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(view: View, slideOffset: Float) {
                    mMap.setPadding(0,0,0,0)
                }

                @SuppressLint("SwitchIntDef")
                override fun onStateChanged(view: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            mFocusedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_icon))
                        }
                    }
                }

            })
        }

        mInfoRouteButton = findViewById(R.id.info_route_button)

        /*****************SEARCH_BUTTON BEGINS**********************/
        mListViewButton = findViewById(R.id.button_open_list)
        mListViewButton.setOnClickListener {
            // Open list view on click
            mInfoViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            mListViewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        mSettingsButton = findViewById(R.id.button_open_settings)

        /*****************SEARCH_BAR BEGINS**********************/
//        val  = findViewById<SearchView>(R.id.search_bar)
        // TODO: Implement search logic here

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

    override fun onStop() {
        mGoogleApiClient.disconnect()
        super.onStop()
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

        // Read data from
        mData = Response(resources.openRawResource(R.raw.poi)
                .bufferedReader().use { br ->
                    br.readText()
                })
                .data
                .map { poi ->
                    if (poi.latLng != null)
                        mMap.addMarker(MarkerOptions().position(poi.latLng)
                                .title(poi.acronym)
                                // https://www.flaticon.com/free-icon/placeholder_126470
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_icon)))
                                .apply { tag = poi }
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
        mViewAdapter = ListAdapter()
        mRecyclerView = findViewById<RecyclerView>(R.id.recycler_view).apply {
            // Recycler view options
            setHasFixedSize(true)
            layoutManager = mViewManager
            adapter = mViewAdapter
        }

        /*****************MY_LOCATION LOGIC BEGINS**********************/
        enableMyLocation()
//        getLocation()

        /*****************MAP OPTIONS BEGIN**********************/
        //region Map Options
        // Disable google maps toolbar
        mMap.uiSettings.isMapToolbarEnabled = false
        // Set bounds for camera
        val uprBounds = LatLngBounds(LatLng(UPR_BOUND_S, UPR_BOUND_W), LatLng(UPR_BOUND_N, UPR_BOUND_E))
        mMap.setLatLngBoundsForCameraTarget(uprBounds)
        // Open camera at LatLng specified by upr
        val upr = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(upr, DEFAULT_ZOOM))
        // Limit zoom
        mMap.setMinZoomPreference(MIN_ZOOM)
        mMap.setMaxZoomPreference(MAX_ZOOM)

        mMap.setOnMarkerClickListener(this)
        //endregion
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        mFocusedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_icon))
        marker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_icon_focus))

        val poi = marker?.tag as PointOfInterest

        if (mCanGetLocation) {
            if (mLastKnownLatLng != null)
                mInfoRouteButton.setOnClickListener {button ->
                    bindRouteToButton(button as ImageButton, mLastKnownLatLng!!, poi.latLng!!)
                }
        }
        bindInfoToView(marker.tag as PointOfInterest)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, FOCUS_ZOOM))
        mMap.setPadding(0,0,0,0)

        mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        mInfoViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        mFocusedMarker = marker
        Log.d(TAG, "Focused marker: ${(mFocusedMarker?.tag as PointOfInterest).name ?: "none"}")
        return true
    }
    //endregion

    //region UI Functions
    private fun bindInfoToView(poi: PointOfInterest) {
        val imageView = findViewById<ImageView>(R.id.info_image)
        if (poi.images != null) {
            imageView.visibility = View.VISIBLE
            // TODO: Use image slider for all images instead of single image
            Util.loadImageIntoView(Util.IMAGE_SERVER_URL + poi.images[0],
                    imageView,
                    IMAGE_FIT_TO_VIEW,
                    IMAGE_WIDTH,
                    IMAGE_HEIGHT)
        } else {
            imageView.visibility = View.GONE
        }
        Util.bindTextToView(poi.name, findViewById(R.id.info_name))
//        bindTextToView(poi.acronym, view.findViewById(R.id.info_acronym))
        Util.bindTextToView(poi.description, findViewById(R.id.info_description))
        bindRouteToButton(mInfoRouteButton, mLastKnownLatLng!!, poi.latLng!!)
        mMap.setPadding(0,0,0, INFO_VIEW_PEEK_HEIGHT)
    }

    fun setPaddingAfterLayout(view: View) {
        view.apply {
            viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    mInfoViewHeight = minOf(this@apply.height, INFO_VIEW_PEEK_HEIGHT)
//                    Log.d(TAG, "Padding: " + mInfoViewHeight.toString())
                    mMap.setPadding(0,0,0,mInfoViewHeight)
                }
            })
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val strOrigin = "origin=${origin.latitude},${origin.longitude}"
        val strDest = "destination=${dest.latitude},${dest.longitude}"
        val mode = if (mapBounds.contains(origin)) "mode=walking&avoid=highways" else "mode=driving"
        val params = "$strOrigin&$strDest&$mode"
        val output = "json"
        return "${Util.GOOGLE_API_URL}$output?$params&key=" +
                packageManager.getApplicationInfo(packageName,
                        PackageManager.GET_META_DATA)
                        .metaData
                        .getString("com.google.android.geo.API_KEY")
    }

    inner class ListAdapter: RecyclerView.Adapter<POIViewHolder>() {
        // Number of items in dataset
        override fun getItemCount(): Int {
            return mData.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val listItem = layoutInflater.inflate(R.layout.list_item_layout, parent, false)
            return POIViewHolder(listItem)
        }

        // Binds data to view when it becomes available
        override fun onBindViewHolder(viewHolder: POIViewHolder, pos: Int) {
            if (mData[pos] == null) {
                Log.e("ListAdapter", "Marker at index $pos is null")
                return
            }

            val poi = mData[pos]?.tag as PointOfInterest
            // Bind data in list item
            Util.bindTextToView(poi.name, viewHolder.nameView)
            Util.bindTextToView(poi.acronym, viewHolder.acronymView)

            if (poi.latLng != null) {
                viewHolder.itemView.setOnClickListener {
                    // Change color of focused marker back to normal
                    mFocusedMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    // Update focused marker to clicked marker
                    mFocusedMarker = mData[pos]
                    // Change color of focused marker
                    mFocusedMarker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

                    // Bind data to info view
                    bindInfoToView(poi)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, MainActivity.FOCUS_ZOOM))
                    mMap.setPadding(0,0,0,0)

                    // Hide list view and show info view
                    mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    mInfoViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            Log.d(TAG, "Successfully bound ${poi.name} to pos: $pos")
        }
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
                    mLastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (mLastKnownLocation != null) {
                        mLastKnownLatLng = LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude)
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
                        mLastKnownLocation = mLocationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (mLastKnownLocation != null) {
                            mLastKnownLatLng = LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mLastKnownLocation
    }

    override fun onProviderDisabled(provider: String?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            mLastKnownLocation = location
            mLastKnownLatLng = LatLng(mLastKnownLocation!!.latitude, mLastKnownLocation!!.longitude)
            mCanGetLocation = true
        } else {
            mCanGetLocation = false
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnected(p0: Bundle?) {
        getLocation()
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
}

// Class defining list item view holder defined in list_item_layout.xml
class POIViewHolder(poiView: View): RecyclerView.ViewHolder(poiView) {
    var nameView = poiView.findViewById<TextView?>(R.id.poi_name)
    var acronymView = poiView.findViewById<TextView?>(R.id.poi_acronym)
}