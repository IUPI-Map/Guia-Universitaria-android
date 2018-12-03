package com.archivouni.guiauniversitaria

import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

object Util {
    private const val TAG = "Util"

    //region Project-wide Constants
    const val IMAGE_SERVER_URL = "http://ec2-18-220-11-214.us-east-2.compute.amazonaws.com/"

    const val DEFAULT_ZOOM = 16.15f
    const val MIN_ZOOM = 16.15f
    const val MAX_ZOOM = 19f
    const val FOCUS_ZOOM = 17.5f

    const val UPR_BOUND_S = 18.39926710
    const val UPR_BOUND_W = -66.05599693
    const val UPR_BOUND_N = 18.41188018
    const val UPR_BOUND_E = -66.03826031

    const val MAP_TILES_DIRECTORY = "map_tiles_bmp"

    const val INFO_VIEW_PEEK_HEIGHT = 900
    const val LIST_VIEW_PEEK_HEIGHT = 600
    //endregion

    var infoViewHeight = 0
    var focusedMarker: Marker? = null

    fun setPaddingAfterLayout(view: View, map: GoogleMap, pos: LatLng? = null) {
        if (focusedMarker != null && focusedMarker!!.position == pos) {
            map.setPadding(0,0,0, infoViewHeight)
            return
        }
        view.apply {
            viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    infoViewHeight = minOf(this@apply.height, INFO_VIEW_PEEK_HEIGHT)
                    Log.d(TAG, "Padding: " + infoViewHeight.toString())
                    map.setPadding(0,0,0,infoViewHeight)
                    if (pos != null)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, FOCUS_ZOOM))
                }
            })
        }
    }

    fun bindInfoToView(data: String?, textView: TextView) {
        if (data != null) {
            textView.text = data
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    fun bindInfoToView(poi: PointOfInterest, view: View, map: GoogleMap) {
        val imageView = view.findViewById<ImageView>(R.id.info_image)
        if (poi.images != null) {
            imageView.visibility = View.VISIBLE
            loadImageIntoView(IMAGE_SERVER_URL + poi.images[0], imageView, map)
        } else {
            imageView.visibility = View.GONE
        }
        bindInfoToView(poi.name, view.findViewById(R.id.info_name))
        bindInfoToView(poi.acronym, view.findViewById(R.id.info_acronym))
        bindInfoToView(poi.description, view.findViewById(R.id.info_description))
    }

    fun loadImageIntoView(url: String,
                          imageView: ImageView,
                          map: GoogleMap,
                          fit: Boolean = false,
                          width: Int = 500,
                          height: Int = 500) {
        if (fit) {
            Picasso.get().load(url)
                    .fit()
                    .placeholder(R.drawable.progress_animation)
                    .into(imageView, LoadImageCallback(url, imageView, map))
        } else {
            Picasso.get().load(url)
                    .resize(width, height)
                    .placeholder(R.drawable.progress_animation)
                    .into(imageView, LoadImageCallback(url, imageView, map))
        }
    }

    class LoadImageCallback(val url: String, val view: View, val map: GoogleMap): Callback {
        companion object {
            private const val TAG = "LoadImageCallback"
        }

        override fun onSuccess() {
            setPaddingAfterLayout(view.parent as View, map)
        }

        override fun onError(e: Exception?) {
            Log.e(TAG, "Unable to load image at $url")
        }

    }
}