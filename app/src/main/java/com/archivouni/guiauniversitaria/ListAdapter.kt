package com.archivouni.guiauniversitaria

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

//This Class is in charged of displaying the list_item_poi
class ListAdapter(private val mData: Array<PointOfInterest>, private val mMap: GoogleMap): RecyclerView.Adapter<ListAdapter.POIViewHolder>() {

    companion object {
        private const val TAG = "ListAdapter"
    }

    class POIViewHolder(poiView: View): RecyclerView.ViewHolder(poiView) {
        var nameView: TextView? = null
        var acronymView: TextView? = null
        init {
            nameView = poiView.findViewById(R.id.poi_name) as TextView
            acronymView = poiView.findViewById(R.id.poi_acronym) as TextView
        }
    }

    //number of items in dataset
    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.list_item_poi, parent, false)
        cellForRow.setOnClickListener {
//            mMap.animateCamera(CameraUpdateFactory.newLatLng())
        }
        return POIViewHolder(cellForRow)
    }

    // Binds data to view when it becomes available
    override fun onBindViewHolder(viewHolder: POIViewHolder, pos: Int) {
        viewHolder.nameView?.text = mData[pos].mName
        viewHolder.acronymView?.text = mData[pos].mAcronym
        viewHolder.itemView.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(mData[pos].mPos))
        }
        Log.d(TAG, "Binding ${mData[pos].mName} to pos: $pos\n" +
                "Result: ${viewHolder.nameView?.text}")
    }
}