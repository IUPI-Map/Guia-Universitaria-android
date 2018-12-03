package com.archivouni.guiauniversitaria

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MapsActivity"

        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mLocationPermissionGranted = false

    private lateinit var mLastKnownLocation: Location

    private lateinit var mListView: View
    private lateinit var mListViewBehavior: BottomSheetBehavior<*>

    private lateinit var mInfoView: View
    private lateinit var mInfoViewBehavior: BottomSheetBehavior<*>

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: RecyclerView.Adapter<*>
    private lateinit var mViewManager: RecyclerView.LayoutManager

    private var focusedMarker: Marker? = null
    private lateinit var mData: Array<Marker?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        //region Map Logic
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
            setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(view: View, slideOffset: Float) {
                }

                override fun onStateChanged(view: View, newState: Int) {

                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            mMap.setPadding(0,0,0,0)
                            if (focusedMarker != null) {
                                focusedMarker!!.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            }
                        }
                        else -> 0
                    }
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

        mData = Response(resources.openRawResource(R.raw.poi).bufferedReader().use { it.readText() }).data.map {poi ->
            if (poi.latLng != null) {
                mMap.addMarker(MarkerOptions().position(poi.latLng)).apply { tag = poi }
            } else {
                null
            }
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
//        getLocationPermission()
//        updateLocationUI()
//        getDeviceLocation()

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
        val upr = LatLng(18.404123, -66.048714)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(upr, Util.DEFAULT_ZOOM))
        // Limit zoom
        mMap.setMinZoomPreference(Util.MIN_ZOOM)
        mMap.setMaxZoomPreference(Util.MAX_ZOOM)

        mMap.setOnMarkerClickListener { marker ->
            Log.d(TAG, "Focused marker: ${if (focusedMarker != null) (focusedMarker!!.tag as PointOfInterest).name else "none"}")
            if (focusedMarker != null) {
                focusedMarker!!.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }

            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))



            val poi = marker.tag as PointOfInterest
            Util.bindInfoToView(poi, mInfoView, mMap)

            Util.setPaddingAfterLayout(mInfoView, mMap, marker.position, focusedMarker)

            mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            mInfoViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            focusedMarker = marker
            false
        }
        //endregion
    }

    /**
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }

    private fun updateLocationUI() {
        try {
            if (mLocationPermissionGranted) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.message)
        }
    }

    private fun getDeviceLocation() {
        /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                val locationResult = mFusedLocationClient.lastLocation
                locationResult.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        mLastKnownLocation = task.result!!
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, task.exception.toString())
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.message)
        }
    }
}
