package com.archivouni.guiauniversitaria

import android.app.ListActivity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_structure.view.*


/*******************************************
 *  Created by RLopez on 10/11/18
 *  credits to Lets build an app on Youtube
 ******************************************/
//This Class is in charged of displaying the list
class ListAdapter: RecyclerView.Adapter<CustomViewHolder>() {


    //number of items on the list
    override fun getItemCount(): Int {
        return 4
    }

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context)
        val cellForRow = layoutInflater.inflate(R.layout.list_structure, p0, false)
        return CustomViewHolder(cellForRow)
    }

    override fun onBindViewHolder(p0: CustomViewHolder, p1: Int) {
        p0.view.structure_name?.text = "Name of Structure"
    }
}



class CustomViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    init {
        view.setOnClickListener {
            println("TEST")

            //This variable is the one that redirects me to another activity
            val intent = Intent(view.context, ListActivity::class.java)

            view.context.startActivity(intent)
        }
    }
}