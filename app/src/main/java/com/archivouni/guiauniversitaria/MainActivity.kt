package com.archivouni.guiauniversitaria

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import me.relex.circleindicator.CircleIndicator
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
        const val MIN_ZOOM = 10f
        const val MAX_ZOOM = 19f
        const val FOCUS_ZOOM = 17.5f

        const val DEFAULT_LATITUDE = 18.404123
        const val DEFAULT_LONGITUDE = -66.048714

        const val PR_BOUND_S = 17.902972
        const val PR_BOUND_W = -67.203095
        const val PR_BOUND_N = 18.496732
        const val PR_BOUND_E = -65.284971

        const val UPR_BOUND_S = 18.39926710
        const val UPR_BOUND_W = -66.05599693
        const val UPR_BOUND_N = 18.41188018
        const val UPR_BOUND_E = -66.03826031

        private const val IMAGE_WIDTH = 700
        private const val IMAGE_HEIGHT = 500
        private const val IMAGE_FIT_TO_VIEW = false

        private const val INFO_VIEW_PEEK_HEIGHT = 1000
        private const val LIST_VIEW_PEEK_HEIGHT = 800
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

    private lateinit var mViewPager: ViewPager
    private lateinit var mPagerAdapter: SlidingImageAdapter
    private lateinit var mCirclePageIndicator: CircleIndicator

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RecyclerView.Adapter<*>
    private lateinit var mViewManager: RecyclerView.LayoutManager

    private lateinit var mSettingsButton: ImageButton
    private lateinit var mCloseRouteButton: Button

    private lateinit var mInfoRouteButton: ImageButton

    private lateinit var mData: Array<Marker?>

    private var mFocusedMarker: Marker? = null
    private var mRouteDestinationMarker: Marker? = null

    private var autoComplete: HashMap<Pair<Int, String>,Marker> = HashMap()
    //endregion

    //region Public functions
    //endregion

    //region Activity Lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateValuesFromBundle(savedInstanceState)
        createLocationCallback()

        // User_Story #1
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
        // User_Story#1 end


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //region Set Views
        mListView = findViewById(R.id.list_view)
        mListViewBehavior = BottomSheetBehavior.from(mListView).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = LIST_VIEW_PEEK_HEIGHT
        }

        mInfoView = findViewById(R.id.info_view)
        mInfoViewBehavior = BottomSheetBehavior.from(mInfoView).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
            peekHeight = INFO_VIEW_PEEK_HEIGHT
            setBottomSheetCallback(InfoViewBottomSheetCallback())
        }

        mViewPager = findViewById(R.id.info_view_pager)
        mCirclePageIndicator = findViewById(R.id.info_circle_page_indicator)

        mInfoRouteButton = findViewById(R.id.info_route_button)
        mCloseRouteButton = findViewById(R.id.button_close_route)
        mCloseRouteButton.setOnClickListener {
            Util.currentRoutes.forEach { polyline ->
                polyline?.remove()
            }
            Util.currentRoutes.clear()
            if (mInfoViewBehavior.state == BottomSheetBehavior.STATE_HIDDEN)
                unfocusMarker()
            mCloseRouteButton.visibility = View.GONE
        }
    //User_Story#5
        mListViewButton = findViewById(R.id.button_open_list)
        mListViewButton.setOnClickListener {
            // Open list view on click
            mInfoViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            mListViewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    //User_Story#5 end

        mSettingsButton = findViewById(R.id.button_open_settings)
        mSettingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
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

    override fun onStop() {
        mGoogleApiClient.disconnect()
        super.onStop()
    }
    //endregion

    // User_Story#1
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

        // Read data from json
        mData = Response(resources.openRawResource(R.raw.poi)
                .bufferedReader().use { br ->
                    br.readText()
                })
                .data.map { poi ->
                    if (poi.latLng != null)
                        mMap.addMarker(MarkerOptions().position(poi.latLng)
                                .title(poi.acronym)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon)))
                                .apply {
                                    tag = poi
                                }
                    else
                        null
                }.toTypedArray()

        Log.d(TAG, "POIs read from json: ${mData.size}")
    //User_Story#1 end


        /*****************RECYCLER_VIEW BEGINS**********************/
        /**
         * Recycler view is initialized here because ListAdapter requires that the map be
         * initialized in order to bind list items to their position on the map.
         */
        mViewManager = LinearLayoutManager(this)
        mViewAdapter = ListAdapter(mData)
        mRecyclerView = findViewById<RecyclerView>(R.id.recycler_view).apply {
            // Recycler view options
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(this@apply.context, DividerItemDecoration.HORIZONTAL).apply{
                setDrawable(this@MainActivity.getDrawable(R.drawable.line_divider)!!)
            })
            layoutManager = mViewManager
            adapter = mViewAdapter
        }
        preloadAutocomplete()
        val searchBar: SearchView = findViewById(R.id.search_bar)
        searchBar.setOnQueryTextListener(SearchBarOnQueryTextListener())


        //User_Story#1
        /*****************MY_LOCATION LOGIC BEGINS**********************/
        enableMyLocation()

        /*****************MAP OPTIONS BEGIN**********************/
        //region Map Options
        // Set style to custom style
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        // Disable google maps toolbar
        mMap.uiSettings.isMapToolbarEnabled = false
        // Set bounds for camera
        // val uprBounds = LatLngBounds(LatLng(UPR_BOUND_S, UPR_BOUND_W), LatLng(UPR_BOUND_N, UPR_BOUND_E))
        val prBounds = LatLngBounds(LatLng(PR_BOUND_S, PR_BOUND_W), LatLng(PR_BOUND_N, PR_BOUND_E))
        mMap.setLatLngBoundsForCameraTarget(prBounds)
        // Open camera at LatLng specified by upr
        val upr = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(upr, DEFAULT_ZOOM))
        // Limit zoom
        mMap.setMinZoomPreference(MIN_ZOOM)
        mMap.setMaxZoomPreference(MAX_ZOOM)

        mMap.setOnMarkerClickListener(this)
        //endregion
    }//User_Story#1 end

    //endregion

    //region UI Functions
    //User_Story#4
    private fun bindInfoToView(poi: PointOfInterest) {

        Util.bindTextToView(poi.name, findViewById(R.id.info_name))
        // Util.bindTextToView(poi.acronym, findViewById(R.id.info_acronym))
        Util.bindTextToView(poi.description, findViewById(R.id.info_description))
     //User_Story#4 end

        //User_Story#16
        if (poi.images != null) {
            mViewPager.visibility = View.VISIBLE
            mCirclePageIndicator.visibility = View.VISIBLE
            val imageUrls = poi.images.map { path ->
                Util.IMAGE_SERVER_URL + path
            }.toTypedArray()

            mPagerAdapter = SlidingImageAdapter(this, imageUrls)
            mViewPager.adapter = mPagerAdapter
            mCirclePageIndicator.setViewPager(mViewPager)
        } else {
            mViewPager.visibility = View.GONE
            mCirclePageIndicator.visibility = View.GONE
        }

        if (mCanGetLocation) {
            bindRouteToButton(mInfoRouteButton, mLastKnownLatLng!!, poi.latLng!!)
        }
        mMap.setPadding(0, 0, 0, INFO_VIEW_PEEK_HEIGHT)
    }
    //User_Story#16 end
    //User_Story#4
    inner class ListAdapter(val data: Array<Marker?>) : RecyclerView.Adapter<ListAdapter.POIViewHolder>() {
        // Class defining list item view holder defined in list_item
        inner class POIViewHolder(poiView: View): RecyclerView.ViewHolder(poiView) {
            var nameView = poiView.findViewById<TextView?>(R.id.poi_name)
            var acronymView = poiView.findViewById<TextView?>(R.id.poi_acronym)
        }

        // Number of items in dataset
        override fun getItemCount(): Int {
            return data.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val listItem = layoutInflater.inflate(R.layout.list_item, parent, false)
            return POIViewHolder(listItem)
        }

        // Binds data to view when it becomes available
        override fun onBindViewHolder(viewHolder: POIViewHolder, pos: Int) {
            if (data[pos] == null) {
                Log.e("ListAdapter", "Marker at index $pos is null")
                return
            }

            val poi = data[pos]?.tag as PointOfInterest
            // Bind data in list item
            Util.bindTextToView(poi.name, viewHolder.nameView)
            Util.bindTextToView(poi.acronym, viewHolder.acronymView)

            if (poi.latLng != null) {
                viewHolder.itemView.setOnClickListener(ListItemOnClickListener(data[pos]))
            }
            Log.d(TAG, "Successfully bound ${poi.name} to pos: $pos")
        }
    }   //User_Story#4 end
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

    //region Event Listeners
    //User_Story#28
    override fun onMarkerClick(marker: Marker?): Boolean {
        unfocusMarker()
        focusMarker(marker)

        val poi = marker?.tag as PointOfInterest

        if (mCanGetLocation && mLastKnownLatLng != null) {
            mInfoRouteButton.setOnClickListener { button ->
                bindRouteToButton(button as ImageButton, mLastKnownLatLng!!, poi.latLng!!)
            }
        }
        bindInfoToView(marker.tag as PointOfInterest)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, FOCUS_ZOOM))
        mMap.setPadding(0, 0, 0, 0)

        mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        mInfoViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        Log.d(TAG, "Focused marker: ${(mFocusedMarker?.tag as PointOfInterest).name ?: "none"}")
        return true
    }
    //End User_Story#28

    /**
     * # Listener for autocompleting search in list view
     *
     * When text changes in the SearchView, filters data and creates a new adapter
     * with the filtered and sorted data, then changes the RecyclerView adapter for
     * the new one.
     *
     * @see ListAdapter
     */
    //User_Story#12
    private inner class SearchBarOnQueryTextListener: SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            if (newText!!.isNotEmpty()) {
                //newRecycler = mRecyclerView
                val query = newText.toLowerCase()
                val filteredData = MutableList<Marker?>(0) { null }
                autoComplete.forEach {(key, value) ->
                    if (key.second.toLowerCase().contains(query)) {
                        filteredData.add(value)
                    }
                }
                filteredData.toTypedArray().apply {
                    sortBy {
                        (it?.tag as PointOfInterest).name
                    }
                }
                mViewAdapter = ListAdapter(filteredData.toTypedArray().apply {
                    sortBy {
                        (it?.tag as PointOfInterest).name ?: ""
                    }
                })
                mRecyclerView.adapter = mViewAdapter
            } else {
                mRecyclerView.adapter = ListAdapter(mData)
            }
            return true
        }
    }
    //User_story#12 end

    /**
     * # Listener for changes in info view bottom sheet
     *
     * On sliding the bottom sheet, padding is removed from map.
     * When bottom sheet is hidden, removes focus from current marker
     * if it's not the destination of the current route.
     */
    private inner class InfoViewBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(view: View, slideOffset: Float) {
            mMap.setPadding(0, 0, 0, 0)
        }

        @SuppressLint("SwitchIntDef")
        override fun onStateChanged(view: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_HIDDEN -> {
                    if (mFocusedMarker != mRouteDestinationMarker)
                        unfocusMarker()
                }
            }
        }
    }

    /**
     * # Listener for clicking items in list view
     *
     * When an item in the list is clicked:
     * 1. Removes focus from currently focused marker if there is one
     * 2. Focus on marker corresponding to item
     * 3. Binds necessary data to info view of item
     * 4. Center map camera on item's marker
     * 5. Hide list view
     * 6. Show item info view
     *
     * @property marker Marker corresponding to clicked item
     */
    private inner class ListItemOnClickListener(val marker: Marker?): View.OnClickListener {
        override fun onClick(p0: View?) {
            val poi = marker?.tag as PointOfInterest
            unfocusMarker()
            focusMarker(marker)

            // Bind data to info view
            bindInfoToView(poi)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, MainActivity.FOCUS_ZOOM))
            mMap.setPadding(0, 0, 0, 0)

            // Hide list view and show info view
            mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            mInfoViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    //endregion

    //region Private Helper Functions

    /**
     * Load data into hash table with:
     * - key = pair of id and name or acronym
     * - value = marker on map
     */
    private fun preloadAutocomplete() {
        mData.forEach { marker ->
            val poi = marker?.tag as PointOfInterest
            if (poi.name != null)
                autoComplete[Pair(poi.id, poi.name)] = marker
            if (poi.acronym != null)
                autoComplete[Pair(poi.id, poi.acronym)] = marker
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

    // Sets listener route button to display route on click
    private fun bindRouteToButton(view: ImageButton, origin: LatLng, dest: LatLng) {
        val url = getDirectionsUrl(origin, dest)
        view.setOnClickListener {
            Toast.makeText(this, R.string.calculating_route_toast, Toast.LENGTH_SHORT).show()
            Util.currentRoutes.forEach { polyline ->
                polyline?.remove()
            }
            Util.currentRoutes.clear()
            Util.DownloadTask(mMap).execute(url)
            mRouteDestinationMarker = mFocusedMarker
            Log.d(TAG, "Current destination: ${(mRouteDestinationMarker?.tag as PointOfInterest).name}")
            mCloseRouteButton.visibility = View.VISIBLE
        }
    }

    private fun focusMarker(marker: Marker?) {
        mFocusedMarker = marker?.apply {
            setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon_focus))
        }
    }

    private fun unfocusMarker() {
        mFocusedMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
        mFocusedMarker = null
    }
    //endregion
}