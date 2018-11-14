package com.archivouni.guiauniversitaria

import android.app.ListActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        private const val TAG = "MapsActivity"

        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val DEFAULT_ZOOM = 16.15f
        const val MIN_ZOOM = 16.15f
        const val MAX_ZOOM = 19f

        private const val UPR_BOUND_S = 18.39926710
        private const val UPR_BOUND_W = -66.05599693
        private const val UPR_BOUND_N = 18.41188018
        private const val UPR_BOUND_E = -66.03826031

    }

    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mLocationPermissionGranted = false

    private lateinit var mLastKnownLocation: Location

    private lateinit var mListView: ConstraintLayout
    private lateinit var mListViewBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager


    // Test data, will be replaced with PointOfInterest class
    data class POI(val name: String, val pos: LatLng)
    private var points = arrayOf(POI("Ciencias Naturales II", LatLng(18.403971, -66.046375)),
            POI("Biblioteca Jose M. Lazaro", LatLng(18.404268, -66.049842)),
            POI("Archivo Central UPRRP", LatLng(18.404100, -66.046861)))

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

        mListView = findViewById(R.id.list_view)

        mListViewBehavior = BottomSheetBehavior.from(mListView)
        mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val searchButton = findViewById<View>(R.id.search_button)
        searchButton.setOnClickListener { _ ->
            mListViewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        val layoutInflater = LayoutInflater.from(this)
//        val listLayout = layoutInflater.inflate(R.layout.list_view, )
        val searchBar = findViewById<SearchView>(R.id.search_bar)
        searchBar.setOnQueryTextFocusChangeListener { v, hasFocus ->
            if(hasFocus) {
                mListViewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
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

//        getLocationPermission()
//        updateLocationUI()
//        getDeviceLocation()


        mMap.mapType = GoogleMap.MAP_TYPE_NONE
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
        points.forEach { mMap.addMarker(MarkerOptions().position(it.pos).title(it.name)) }
        val upr = LatLng(18.404123, -66.048714)
        val uprBounds = LatLngBounds(LatLng(UPR_BOUND_S, UPR_BOUND_W), LatLng(UPR_BOUND_N, UPR_BOUND_E))
        mMap.setLatLngBoundsForCameraTarget(uprBounds)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(upr, DEFAULT_ZOOM))
        mMap.setMinZoomPreference(MIN_ZOOM)
        mMap.setMaxZoomPreference(MAX_ZOOM)

        mMap.addTileOverlay(TileOverlayOptions().tileProvider(GoogleMapsTileProvider(resources.assets)))

        viewManager = LinearLayoutManager(this)
        viewAdapter = ListAdapter(poiList, mMap)

        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
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
