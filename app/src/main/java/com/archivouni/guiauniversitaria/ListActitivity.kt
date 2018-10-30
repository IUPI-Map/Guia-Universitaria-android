package com.archivouni.guiauniversitaria

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.archivouni.guiauniversitaria.R.id.recyclerView_list
import kotlinx.android.synthetic.main.activity_list.*

class ListActitivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.list_structure)
        recyclerView_list.layoutManager = LinearLayoutManager(this)
        recyclerView_list.adapter = InfoAdapter()
    }

    private class InfoAdapter: RecyclerView.Adapter<InfoViewHolder>() {

        override fun getItemCount(): Int {
            return 1
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val customView = layoutInflater.inflate(R.layout.info_row, parent, false)
            return InfoViewHolder(customView)
        }

        override fun onBindViewHolder(p0: InfoViewHolder, position: Int){

        }
    }


    private class InfoViewHolder(val customView: View): RecyclerView.ViewHolder(customView){

    }
}