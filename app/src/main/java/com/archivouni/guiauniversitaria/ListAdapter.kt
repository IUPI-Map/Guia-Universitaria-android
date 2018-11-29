package com.archivouni.guiauniversitaria

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.bottomsheet.BottomSheetBehavior

//This Class is in charged of displaying the list_item_poi
class ListAdapter(private val mData: Array<PointOfInterest>, private val mMap: GoogleMap, private val mBottomSheetBehavior: BottomSheetBehavior<*>): RecyclerView.Adapter<ListAdapter.POIViewHolder>() {

    companion object {
        private const val TAG = "ListAdapter"

        private const val ON_CLICK_ZOOM = 17.5f
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
        val listItem = layoutInflater.inflate(R.layout.list_item_poi, parent, false)
        return POIViewHolder(listItem)
    }

    // Binds data to view when it becomes available
    override fun onBindViewHolder(viewHolder: POIViewHolder, pos: Int) {
        viewHolder.nameView?.text = mData[pos].name
        viewHolder.acronymView?.text = mData[pos].acronym
        if (mData[pos].latLng != null) {
            viewHolder.itemView.setOnClickListener {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mData[pos].latLng, ON_CLICK_ZOOM))
                mBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                // TODO: Open information view for point of interest
            }
        }
        Log.d(TAG, "Binding ${mData[pos].name} to pos: $pos\n" +
                "Result: ${viewHolder.nameView?.text}")
    }
}