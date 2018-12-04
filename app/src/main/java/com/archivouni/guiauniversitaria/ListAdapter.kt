package com.archivouni.guiauniversitaria

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.RuntimeException

//This Class is in charged of displaying the list_item_poi
class ListAdapter(private val mData: Array<Marker?>,
                  private val mMap: GoogleMap,
                  private val mInfoView: View,
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
        Util.bindTextToView(poi.name, viewHolder.nameView!!)
        Util.bindTextToView(poi.acronym, viewHolder.acronymView!!)
        if (poi.latLng != null) {
            viewHolder.itemView.setOnClickListener {
                if (Util.focusedMarker != null) {
                    Util.focusedMarker!!.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                }
                mData[pos]!!.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                Util.bindInfoToView(poi, mInfoView, mMap)

                Util.setPaddingAfterLayout(mInfoView, mMap, poi.latLng)
                Util.focusedMarker = mData[pos]
                mListViewBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                mInfoViewBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        Log.d(TAG, "Binding ${poi.name} to pos: $pos\n" +
                "Result: ${viewHolder.nameView!!.text}")
    }

    private fun calculatePadding(latLng: LatLng, yOffset: Int): LatLng =
        LatLng(latLng.latitude - yOffset, latLng.longitude)
}