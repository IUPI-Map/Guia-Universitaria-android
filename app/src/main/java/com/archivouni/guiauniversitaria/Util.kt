package com.archivouni.guiauniversitaria

import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import android.os.AsyncTask
import com.archivouni.guiauniversitaria.R.id.map
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.squareup.picasso.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import kotlin.Exception


// This object hold project-wide constants and methods
object Util {
    private const val TAG = "Util"

    //region Project-wide Constants
    const val IMAGE_SERVER_URL = "http://ec2-18-220-11-214.us-east-2.compute.amazonaws.com/"
    const val GOOGLE_API_URL = "https://maps.googleapis.com/maps/api/directions/"

    const val LOCATION_REQUEST_INTERVAL = 10L
    const val LOCATION_REQUEST_FASTEST_INTERVAL = 5L

    const val DEFAULT_ZOOM = 16.15f
    const val MIN_ZOOM = 16.15f
    const val MAX_ZOOM = 19f
    const val FOCUS_ZOOM = 17.5f

    const val DEFAULT_LATITUDE = 18.404123
    const val DEFAULT_LONGITUDE = -66.048714

    const val UPR_BOUND_S = 18.39926710
    const val UPR_BOUND_W = -66.05599693
    const val UPR_BOUND_N = 18.41188018
    const val UPR_BOUND_E = -66.03826031

//    const val MAP_TILES_DIRECTORY = "map_tiles_bmp"

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

    fun bindInfoToView(poi: PointOfInterest, view: View, map: GoogleMap) {
        val imageView = view.findViewById<ImageView>(R.id.info_image)
        if (poi.images != null) {
            imageView.visibility = View.VISIBLE
            loadImageIntoView(IMAGE_SERVER_URL + poi.images[0], imageView, map)
        } else {
            imageView.visibility = View.GONE
        }
        bindTextToView(poi.name, view.findViewById(R.id.info_name))
//        bindTextToView(poi.acronym, view.findViewById(R.id.info_acronym))
        bindTextToView(poi.description, view.findViewById(R.id.info_description))
    }

    fun bindTextToView(data: String?, textView: TextView) {
        if (data != null) {
            textView.text = data
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
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

    class LoadImageCallback(private val url: String, val view: View, val map: GoogleMap): Callback {
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

    object DirectionsJSONParser {
        private const val TAG_ROUTES = "routes"
        private const val TAG_LEGS = "legs"
        private const val TAG_STEPS = "steps"
        private const val TAG_OVERVIEW_POLYLINE = "overview_polyline"
        private const val TAG_POINTS="points"

        fun parse(jObject: JSONObject): List<LatLng>? {
            val jRoutes: JSONArray?
            var jLegs: JSONArray?
            var jSteps: JSONArray?

            try {
                jRoutes = jObject.getJSONArray(TAG_ROUTES)
                if (jRoutes.length() > 0) {
                    val overviewPolyline = (jRoutes.get(0) as JSONObject).getJSONObject(TAG_OVERVIEW_POLYLINE)
                    val encodedPoints = overviewPolyline.getString(TAG_POINTS)
                    Log.d(TAG, "Polyline: $encodedPoints")
                    return PolyUtil.decode(encodedPoints)
                } else {
                    throw Exception("No routes")
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
            return null
        }

    }

    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            // Creating an http connection to communicate with url
            urlConnection = (url.openConnection() as HttpURLConnection)
                    .apply {
                        readTimeout = 15 * 1000
                        connectTimeout = 15 * 1000
                        requestMethod = "GET"
                    }

            // Connecting to url
            urlConnection.connect()
            Log.d(TAG, "Connected to host")

            // Reading data from url
            iStream = urlConnection.inputStream
            Log.d(TAG, iStream.toString())

            val br = BufferedReader(InputStreamReader(iStream))

            val sb = StringBuffer()

            var line = br.readLine()
            Log.d(TAG, line)
            while (line != null) {
                sb.append(line)
                line = br.readLine()
            }

            data = sb.toString()

            br.close()

        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        } finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        return data
    }

    // Fetches data from url passed
    class DownloadTask(val map: GoogleMap): AsyncTask<String, Void, String?>() {

        // Downloading data in non-ui thread
        override fun doInBackground(vararg url: String): String {

            Log.d(TAG, "Downloading json from ${url[0]}")
            // For storing data from web service
            var data = ""

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0])
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Unable to connect to host: ${url[0]}")
            } catch (e: Exception) {
                Log.d("Tag", e.toString())
            }

            Log.d(TAG, "Data: $data")
            return data
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        override fun onPostExecute(result: String?) {

            if (result == null) {
                return
            }
            Log.d(TAG,"json length: ${result.length}")
            val parserTask = ParserTask(map)

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result)
        }
    }

    class ParserTask(val map: GoogleMap): AsyncTask<String, Void, List<LatLng>?>() {

        // Parsing the data in non-ui thread
        override fun doInBackground(vararg jsonData: String): List<LatLng>? {
            Log.d(TAG, "Parsing data")
            val jObject: JSONObject
            val route: List<LatLng>?

            try {
                Log.d(TAG, "Test")
                jObject = JSONObject(jsonData[0])
                route = DirectionsJSONParser.parse(jObject)
                Log.d(TAG, route.toString())
                return route
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        // Executes in UI thread, after the parsing process
        override fun onPostExecute(result: List<LatLng>?) {
            if (result == null) {
                Log.e(TAG, "Data not found")
                return
            }
            val lineOptions = PolylineOptions().apply {
                addAll(result)
                width(8f)
                zIndex(4f)
                color(Color.RED)
                visible(true)
            }
            var x = map.addPolyline(lineOptions)
            Log.d(TAG, "Finished parsing")
        }
    }

}