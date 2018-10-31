package com.archivouni.guiauniversitaria

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng

class ListActivity : AppCompatActivity(){

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private val poiList = arrayOf(PointOfInterest("Ciencias Naturales II", "CN", LatLng(18.403971, -66.046375)),
    PointOfInterest("Biblioteca Jose M. Lazaro", "", LatLng(18.404268, -66.049842)),
    PointOfInterest("Archivo Central UPRRP", "", LatLng(18.404100, -66.046861)))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        viewManager = LinearLayoutManager(this)
        viewAdapter = ListAdapter(poiList)

        recyclerView = findViewById<RecyclerView>(R.id.recycler_view_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }
}