package com.archivouni.guiauniversitaria

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//This Class is in charged of displaying the list_item_poi
class ListAdapter(private val mData: Array<PointOfInterest>): RecyclerView.Adapter<ListAdapter.POIViewHolder>() {

    class POIViewHolder(poiView: View): RecyclerView.ViewHolder(poiView) {
        val nameView = poiView.findViewById(R.id.poi_name) as TextView
        val acronymView = poiView.findViewById(R.id.poi_acronym) as TextView
    }

    //number of items in dataset
    override fun getItemCount(): Int {
        return mData.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.list_item_poi, parent, false)

        cellForRow.setOnClickListener {
            // Move to activity showing information
        }
        return POIViewHolder(cellForRow)
    }

    // Binds data to view when it becomes available
    override fun onBindViewHolder(viewHolder: POIViewHolder, pos: Int) {
        viewHolder.nameView.text = mData[pos].mName
        viewHolder.acronymView.text = mData[pos].mAcronym
    }
}