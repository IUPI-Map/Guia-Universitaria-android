package com.archivouni.guiauniversitaria

import android.Manifest
import android.content.Context
import android.graphics.Color
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
import android.os.AsyncTask.execute
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ImageButton
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.google.android.gms.maps.model.PolylineOptions
import android.widget.Toast
import com.archivouni.guiauniversitaria.R.id.map
import com.archivouni.guiauniversitaria.R.raw.poi
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject




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

    fun bindInfoToView(data: String?, textView: TextView) {
        if (data != null) {
            textView.text = data
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    fun bindRouteToButton(view: ImageButton, map: GoogleMap, origin: LatLng, dest: LatLng, key: String) {
        val url = getDirectionsUrl(origin, dest, key)
        view.setOnClickListener {
            DownloadTask(map).execute(url)
        }
    }

    fun bindInfoToView(poi: PointOfInterest, view: View, map: GoogleMap, myLocation: LatLng, key: String) {
        val imageView = view.findViewById<ImageView>(R.id.info_image)
        if (poi.images != null) {
            imageView.visibility = View.VISIBLE
            loadImageIntoView(IMAGE_SERVER_URL + poi.images[0], imageView, map)
        } else {
            imageView.visibility = View.GONE
        }
        bindRouteToButton(view.findViewById(R.id.info_route_button), map, myLocation, poi.latLng!!, key)
        bindInfoToView(poi.name, view.findViewById(R.id.info_name))
//        bindInfoToView(poi.acronym, view.findViewById(R.id.info_acronym))
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

    class DirectionsJSONParser {
        companion object {
            private const val TAG_ROUTES = "routes"
            private const val TAG_LEGS = "legs"
            private const val TAG_STEPS = "steps"
            private const val TAG_POLYLINE = "polyline"
            private const val TAG_POINTS="points"
        }

        fun parse(jObject: JSONObject): List<List<HashMap<String,String>>> {
            val routes = ArrayList<List<HashMap<String,String>>>()
            val jRoutes: JSONArray?
            var jLegs: JSONArray?
            var jSteps: JSONArray?

            try {
                jRoutes = jObject.getJSONArray(TAG_ROUTES)
                for (i in 0 until jRoutes.length()) {
                    jLegs = (jRoutes.get(i) as JSONObject).getJSONArray(TAG_LEGS)
                    val path = ArrayList<HashMap<String,String>>()

                    for(j in 0 until jLegs.length()) {
                        jSteps = (jLegs.get(j) as JSONObject).getJSONArray(TAG_STEPS)

                        var list: List<LatLng>? = null
                        for (k in 0 until jSteps.length()) {
                            val polyline = ((jSteps.get(k) as JSONObject)
                                    .get(TAG_POLYLINE) as JSONObject)
                                    .get(TAG_POINTS) as String
                            list = decodePoly(polyline)
                        }

                        for (l in 0 until list!!.size) {
                            val hm = HashMap<String,String>()
                            hm.put("lat", list.get(l).latitude.toString())
                            hm.put("lng", list.get(l).longitude.toString())
                            path.add(hm)
                        }
                    }
                    routes.add(path)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return routes
        }

        private fun decodePoly(encoded: String): List<LatLng> {
            val poly = ArrayList<LatLng>()
            var index =0
            val len = encoded.length
            var lat = 0
            var lng = 0

            while (index < len) {
                // TODO: Put in function
                var b: Int
                var shift = 0
                var result = 0
                do {
                    b = encoded[index++].toInt() - 63
                    result = result xor ((b and 0x1f) shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlat = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
                lat += dlat

                shift = 0
                result = 0
                do {
                    b = encoded[index++].toInt() - 63
                    result = result xor ((b and 0x1f) shl shift)
                    shift += 5
                } while (b >= 0x20)
                val dlng = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
                lng += dlng

                val p = LatLng(lat / 1e5, lng / 1e5)
                poly.add(p)
            }
            return poly
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng, key: String): String {
        val str_origin = "origin=${origin.latitude},${origin.longitude}"
        val str_dest = "destination=${dest.latitude},${origin.longitude}"
        val sensor = "sensor=false"
        val mode = "mode=walking"
        val params = "$str_origin&$str_dest&$sensor&$mode"
        val output = "json"
        return "$GOOGLE_API_URL$output?$params&key=$key"

    }

    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            // Creating an http connection to communicate with url
            urlConnection = url.openConnection() as HttpURLConnection

            // Connecting to url
            urlConnection.connect()

            // Reading data from url
            iStream = urlConnection.inputStream

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
            Log.d(TAG, e.toString())
        } finally {
            iStream!!.close()
            urlConnection!!.disconnect()
        }
        return data
    }

    // Fetches data from url passed
    class DownloadTask(val map: GoogleMap): AsyncTask<String, Void, String>() {

        // Downloading data in non-ui thread
        override fun doInBackground(vararg url: String): String {

            Log.d(TAG, "Downloading json from ${url[0]}")
            // For storing data from web service
            var data = ""

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0])
            } catch (e: Exception) {
                Log.d("Background Task", e.toString())
            }

            Log.d(TAG, "Data: $data")
            return data
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            val parserTask = ParserTask(map)

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result)
        }
    }

    class ParserTask(val map: GoogleMap): AsyncTask<String, Void, List<List<HashMap<String, String>>>>() {

        // Parsing the data in non-ui thread
        override fun doInBackground(vararg jsonData: String): List<List<HashMap<String, String>>>? {
            Log.d(TAG, "Parsing data")
            val jObject: JSONObject
            var routes: List<List<HashMap<String, String>>>? = null

            try {
                jObject = JSONObject(jsonData[0])
                val parser = DirectionsJSONParser()

                // Starts parsing data
                routes = parser.parse(jObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return routes
        }

        // Executes in UI thread, after the parsing process
        override fun onPostExecute(result: List<List<HashMap<String, String>>>) {
            var points: ArrayList<LatLng>? = null
            var lineOptions: PolylineOptions? = null

            // Traversing through all the routes
            for (i in result.indices) {
                points = ArrayList()
                lineOptions = PolylineOptions()

                // Fetching i-th route
                val path = result[i]

                // Fetching all the points in i-th route
                for (j in path.indices) {
                    val point = path[j]

                    if (j == 0) {    // Get distance from the list
                        continue
                    } else if (j == 1) { // Get duration from the list
                        continue
                    }

                    val lat = point["lat"]!!.toDouble()
                    val lng = point["lng"]!!.toDouble()
                    val position = LatLng(lat, lng)

                    points.add(position)
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points)
                lineOptions.width(2f)
                lineOptions.color(Color.RED)
            }

            // Drawing polyline in the Google Map for the i-th route
            points!!.forEach {
                Log.d(TAG, "Point: ${it.toString()}")
            }
            map.addPolyline(lineOptions)
            Log.d(TAG, "Finished parsing")
        }
    }

}