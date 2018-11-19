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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val TAG = "MapsActivity"

        private const val IMAGE_SERVER_URL = "http://ec2-18-220-11-214.us-east-2.compute.amazonaws.com/"

        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        private const val DEFAULT_ZOOM = 16.15f
        const val MIN_ZOOM = 16.15f
        const val MAX_ZOOM = 19f

        const val UPR_BOUND_S = 18.39926710
        const val UPR_BOUND_W = -66.05599693
        const val UPR_BOUND_N = 18.41188018
        const val UPR_BOUND_E = -66.03826031
    }

    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mLocationPermissionGranted = false

    private lateinit var mLastKnownLocation: Location

    private lateinit var mListView: View
    private lateinit var mListViewBehavior: BottomSheetBehavior<*>

    private lateinit var recyclerView: RecyclerView
    private var viewAdapter: RecyclerView.Adapter<*>? = null
    private lateinit var viewManager: RecyclerView.LayoutManager

    // TODO: Remove once SQLite is functional
    val poiList = arrayOf(PointOfInterest("Ciencias Naturales II", "CN", LatLng(18.403971, -66.046375)),
            PointOfInterest("Biblioteca Jose M. Lazaro", "1", LatLng(18.404268, -66.049842)),
            PointOfInterest("Archivo Central UPRRP", "2", LatLng(18.404100, -66.046861)),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null),
            PointOfInterest(null,null,null))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        /*****************MAP LOGIC BEGINS**********************/
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        /*****************BOTTOM_SHEET BEGINS**********************/
        mListView = findViewById(R.id.list_view)
        mListViewBehavior = BottomSheetBehavior.from(mListView)
        mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        /*****************SEARCH_BUTTON BEGINS**********************/
        val searchButton = findViewById<View>(R.id.search_button)
        searchButton.setOnClickListener {
            // Open list view on click
            mListViewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        /*****************SEARCH_BAR BEGINS**********************/
        val searchBar = findViewById<SearchView>(R.id.search_bar)
        // TODO: Implement search logic here
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

        /*****************RECYCLER_VIEW BEGINS**********************/
        /**
         * Recycler view is initialized here because ListAdapter requires that the map be
         * initialized in order to bind list items to their position on the map.
         */
        viewManager = LinearLayoutManager(this)
        viewAdapter = ListAdapter(poiList, mMap, mListViewBehavior)
        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_list).apply {
            // Recycler view options
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        /*****************MY_LOCATION LOGIC BEGINS**********************/
//        TODO: Change from get_last_location to get_location_updates
//        getLocationPermission()
//        updateLocationUI()
//        getDeviceLocation()

        /*****************MAP_OPTIONS BEGINS**********************/
        // Start with empty map
        mMap.mapType = GoogleMap.MAP_TYPE_NONE
        // Add tile overlay
        mMap.addTileOverlay(TileOverlayOptions().tileProvider(GoogleMapsTileProvider(resources.assets)))
        // Set bounds for camera
        val uprBounds = LatLngBounds(LatLng(UPR_BOUND_S, UPR_BOUND_W), LatLng(UPR_BOUND_N, UPR_BOUND_E))
        mMap.setLatLngBoundsForCameraTarget(uprBounds)
        // Open camera at LatLng specified by upr
        val upr = LatLng(18.404123, -66.048714)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(upr, DEFAULT_ZOOM))
        // Limit zoom
        mMap.setMinZoomPreference(MIN_ZOOM)
        mMap.setMaxZoomPreference(MAX_ZOOM)
        // Add markers to map
        // TODO: Implement using SQLite
        poiList.forEach {
            if (it.mLatLng != null) {
                mMap.addMarker(MarkerOptions().position(it.mLatLng).title(it.mName))
            }
        }
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
