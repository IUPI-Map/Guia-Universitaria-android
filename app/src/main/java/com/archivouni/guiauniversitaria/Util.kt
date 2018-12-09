package com.archivouni.guiauniversitaria

import android.app.Activity
import android.graphics.Bitmap
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
import android.view.inputmethod.InputMethodManager
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.squareup.picasso.Transformation
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException
import java.util.*
import kotlin.Exception


// This object hold project-wide constants and methods
object Util {
    private const val TAG = "Util"

    //region Project-wide Constants
    const val IMAGE_SERVER_URL = "http://ec2-18-220-11-214.us-east-2.compute.amazonaws.com/images/"
    const val GOOGLE_API_URL = "https://maps.googleapis.com/maps/api/directions/"

    const val IMAGE_WIDTH = 700
    const val IMAGE_HEIGHT = 500
    //endregion

    //region Util Constants
    private const val POLYLINE_WIDTH = 20f
    private const val POLYLINE_COLOR = Color.BLACK
    // End cap options: square, round, butt (default)
    private const val POLYLINE_START_CAP = "round"
    private const val POLYLINE_END_CAP = "round"
    // Joint type options: bevel, round, default
    private const val POLYLINE_JOINT_TYPE = JointType.BEVEL
    // Pattern options: gap, dash, dot, solid (default)
    private const val POLYLINE_STROKE_PATTERN = "solid"
    private const val POLYLINE_STROKE_LENGTH = 16f

    private const val CONNECT_TIMEOUT = 15
    private const val READ_TIMEOUT = 15
    //endregion

    private val cachedRoutes = HashMap<String, Polyline>()
    var currentRoutes = MutableList<Polyline?>(0) { null }

    // Hide android soft keyboard
    // https://stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: View(activity)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    fun bindTextToView(data: String?, textView: TextView?) {
        if (textView == null) {
            Log.e("Data Binding", "Textview not found")
            return
        }

        if (data != null) {
            textView.text = data
            textView.visibility = View.VISIBLE
        } else {
            textView.visibility = View.GONE
        }
    }

    fun loadImageIntoView(url: String, imageView: ImageView?,
                          fit: Boolean = false,
                          width: Int = 500, height: Int = 500,
                          callback: Callback = LoadImageCallback()) {
        if (imageView == null) {
            Log.e("Data Binding", "Imageview not found")
            return
        }
        if (fit) {
            Picasso.get().load(url)
                    .fit()
                    .placeholder(R.drawable.progress_animation)
                    .into(imageView, callback)
        } else {
            Picasso.get().load(url)
                    .resize(width, height)
                    .placeholder(R.drawable.progress_animation)
                    .into(imageView, callback)
        }
    }

    class LoadImageCallback: Callback {
        companion object {
            private const val TAG = "LoadImageCallback"
        }

        override fun onSuccess() {
            Log.d(TAG, "Succesfully loaded image")
        }

        override fun onError(e: Exception?) {
            Log.e(TAG, "Unable to load image")
        }
    }

    object DirectionsJSONParser {
        private const val TAG_ROUTES = "routes"
        private const val TAG_OVERVIEW_POLYLINE = "overview_polyline"
        private const val TAG_POINTS="points"

        fun parse(jObject: JSONObject): List<LatLng>? {
            val jRoutes: JSONArray?

            try {
                jRoutes = jObject.getJSONArray(TAG_ROUTES)
                if (jRoutes.length() > 0) {
                    val overviewPolyline = (jRoutes[0] as JSONObject).getJSONObject(TAG_OVERVIEW_POLYLINE)
                    val encodedPoints = overviewPolyline.getString(TAG_POINTS)
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

    fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)

            // Creating an http connection to communicate with url
            urlConnection = (url.openConnection() as HttpURLConnection)
                    .apply {
                        readTimeout = READ_TIMEOUT * 1000
                        connectTimeout = CONNECT_TIMEOUT * 1000
                        requestMethod = "GET"
                    }

            // Connecting to url
            urlConnection.connect()

            // Reading data from url
            iStream = urlConnection.inputStream

            val br = BufferedReader(InputStreamReader(iStream))

            val sb = StringBuffer()

            var line = br.readLine()
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
    class DownloadTask(private val map: GoogleMap): AsyncTask<String, Void, String?>() {
        // TODO: Give progress updates
        private lateinit var url: String

        // Downloading data in non-ui thread
        override fun doInBackground(vararg urls: String): String? {
            url = urls[0]
            if (cachedRoutes.containsKey(url)) {
                Log.d("$TAG.DownloadTask", "Route already cached, cancelling download")
                this.cancel(true)
            }
            if (this.isCancelled)
                return null

            Log.d("$TAG.DownloadTask", "Downloading json from $url")
            // For storing data from web service
            var data: String? = null

            try {
                // Fetching the data from web service
                data = downloadUrl(url)
            } catch (e: UnknownHostException) {
                Log.e("$TAG.DownloadTask", "Unable to connect to host: $url")
            } catch (e: Exception) {
                Log.d("$TAG.DownloadTask", e.toString())
            }
            return data
        }

        override fun onCancelled() {
            currentRoutes.apply {
                forEach { polyline ->
                    polyline?.remove()
                }
                add(map.addPolyline(createLineOptions(cachedRoutes[url]?.points)))
            }
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        override fun onPostExecute(result: String?) {
            result ?: return

            val parserTask = ParserTask(url, map)

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result)
        }
    }

    class ParserTask(private val url: String, private val map: GoogleMap): AsyncTask<String, Void, List<LatLng>?>() {

        // Parsing the data in non-ui thread
        override fun doInBackground(vararg jsonData: String): List<LatLng>? {
            Log.d("$TAG.ParserTask", "Parsing data . . .")
            val jObject: JSONObject
            val route: List<LatLng>?

            try {
                jObject = JSONObject(jsonData[0])
                route = DirectionsJSONParser.parse(jObject)
                return route
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        // Executes in UI thread, after the parsing process
        override fun onPostExecute(result: List<LatLng>?) {
            if (result == null) {
                Log.e("$TAG.ParserTask", "Error parsing data")
                return
            }

            // Add route to map and store in hashmap with url as key
            currentRoutes.add(map.addPolyline(createLineOptions(result))
                    .also { polyline ->
                        cachedRoutes[url] = polyline
                    }
            )
            Log.d("$TAG.ParserTask", "Route added to map")
        }
    }

    private fun createLineOptions(points: List<LatLng>?): PolylineOptions {
        return PolylineOptions().addAll(points)
                .width(POLYLINE_WIDTH)
                .color(POLYLINE_COLOR)
                .startCap(when (POLYLINE_START_CAP) {
                    "round" -> RoundCap()
                    "square" -> SquareCap()
                    else -> ButtCap()
                })
                .endCap(when (POLYLINE_END_CAP) {
                    "round" -> RoundCap()
                    "square" -> SquareCap()
                    else -> ButtCap()
                })
                .jointType(POLYLINE_JOINT_TYPE)
                .apply {
                    if (POLYLINE_STROKE_PATTERN != "solid") {
                        pattern(List(points!!.size) {
                            when (POLYLINE_STROKE_PATTERN) {
                                "dash" -> Dash(POLYLINE_STROKE_LENGTH)
                                "gap" -> Gap(POLYLINE_STROKE_LENGTH)
                                "dot" -> Dot()
                                else -> null
                            }
                        })
                    }
                }
    }
}