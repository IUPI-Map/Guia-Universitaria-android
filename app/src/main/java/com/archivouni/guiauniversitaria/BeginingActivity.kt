package com.archivouni.guiauniversitaria

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_begining.*
import kotlinx.android.synthetic.main.activity_settings.*


class BeginingActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_begining)
        beginning_tutorial.setOnClickListener {
            val tutorial = Intent(this, TutorialActivity::class.java)
            startActivity(tutorial) }

        begginging_map.setOnClickListener {
            val mapButton = Intent(this, MainActivity::class.java)
            startActivity(mapButton)
        }
    }
}