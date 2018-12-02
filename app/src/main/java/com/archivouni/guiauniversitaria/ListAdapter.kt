package com.archivouni.guiauniversitaria

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.archivouni.guiauniversitaria.MapsActivity.Companion.ON_CLICK_ZOOM
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.Exception
import java.lang.RuntimeException

//This Class is in charged of displaying the list_item_poi
class ListAdapter(private val mData: Array<Marker?>,
                  private val mMap: GoogleMap,
                  private val mListViewBehavior: BottomSheetBehavior<*>,
                  private val mInfoViewBehavior: BottomSheetBehavior<*>)
    : RecyclerView.Adapter<ListAdapter.POIViewHolder>() {

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
        val listItem = layoutInflater.inflate(R.layout.list_item_poi, parent, false)
        return POIViewHolder(listItem)
    }

    // Binds data to view when it becomes available
    override fun onBindViewHolder(viewHolder: POIViewHolder, pos: Int) {
        if (mData[pos] == null) {
            Log.e(TAG, "Marker $pos is null")
            throw RuntimeException()
        }
        val poi = mData[pos]!!.tag as PointOfInterest
        viewHolder.nameView?.text = poi.name ?: ""
        viewHolder.acronymView?.text = poi.acronym ?: ""
        if (poi.latLng != null) {
            viewHolder.itemView.setOnClickListener {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poi.latLng, ON_CLICK_ZOOM))
                mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                mInfoViewBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        Log.d(TAG, "Binding ${poi.name} to pos: $pos\n" +
                "Result: ${viewHolder.nameView?.text}")
    }
}