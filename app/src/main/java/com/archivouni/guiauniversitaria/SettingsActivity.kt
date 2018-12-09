package com.archivouni.guiauniversitaria

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Opens the Activity for the Details Page
        detalles_button.setOnClickListener{
            val detallesIntent = Intent(this, DetailsActivity::class.java)
            startActivity(detallesIntent)
        }

        // Opens the Activity for the Tutorial Page
        tutorial_button.setOnClickListener{
            val tutorialIntent = Intent(this, TutorialActivity::class.java)
            startActivity(tutorialIntent)
        }

        // Opens the Activity for the Contact Page
        contacto_button.setOnClickListener{
            val contactoIntent = Intent(this, ContactActivity::class.java)
            startActivity(contactoIntent)
        }
    }
}
