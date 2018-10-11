package com.archivouni.guiauniversitaria

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    data class POI(val name: String, val pos: LatLng)

    var points = arrayOf(POI("Ciencias Naturales II", LatLng(18.403971, -66.046375)),
            POI("Biblioteca Jose M. Lazaro", LatLng(18.404268, -66.049842)),
            POI("Archivo Central UPRRP", LatLng(18.404100, -66.046861)))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
        points.forEach { mMap.addMarker(MarkerOptions().position(it.pos).title(it.name)) }
        val upr = LatLng(18.404123, -66.048714)
        val uprBounds = LatLngBounds(LatLng(18.399495, -66.055392), LatLng(18.409678, -66.040672))
        mMap.setLatLngBoundsForCameraTarget(uprBounds)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(upr, 16f))
        mMap.setMinZoomPreference(15f)
    }

}
