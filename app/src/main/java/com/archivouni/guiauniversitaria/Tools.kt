package com.archivouni.guiauniversitaria

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.AsyncTask
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.squareup.picasso.Picasso
import java.net.URL

object Tools {
//    fun loadImageFromURL(url: String): Bitmap {
//        return LoadImage().execute(URL(url)).get()
//    }

//    class LoadImage: AsyncTask<String, Void, Bitmap>() {
//        override fun doInBackground(vararg urls: String?): Bitmap {
//            return BitmapFactory.decodeStream(URL(urls[0]!!).openConnection().getInputStream())
//        }
//    }
    
    fun bindInfoToView(poi: PointOfInterest, view: View) {
        if (poi.name != null) {
            view.findViewById<TextView>(R.id.info_title).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.info_title).text = poi.name
        } else {
            view.findViewById<TextView>(R.id.info_title).visibility = View.GONE
        }

        if (poi.acronym != null) {
            view.findViewById<TextView>(R.id.info_acronym).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.info_acronym).text = poi.acronym
        } else {
            view.findViewById<TextView>(R.id.info_acronym).visibility = View.GONE
        }

        if (poi.description != null) {
            view.findViewById<TextView>(R.id.info_description).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.info_description).text = poi.description
        } else {
            view.findViewById<TextView>(R.id.info_description).visibility = View.GONE
        }

        // TODO: Bind images to gallery instead of single image
        val imageView = view.findViewById<ImageView>(R.id.info_image)
        if (poi.images.isNotEmpty()) {
//                poi.images.forEach {path ->
//                    Picasso.get().load(IMAGE_SERVER_URL + path)
//                            .fit()
//                            .into(imageView)
//                }
            Picasso.get().load(MapsActivity.IMAGE_SERVER_URL + poi.images[0])
//                    .fit()
                    .resize(400, 400)
                    .into(imageView)
        }
    }
}