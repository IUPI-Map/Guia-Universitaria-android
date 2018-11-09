package com.archivouni.guiauniversitaria

import android.content.res.AssetManager
import android.graphics.Rect
import android.util.Log
import android.util.SparseArray
import com.archivouni.guiauniversitaria.MapsActivity.Companion.MAX_ZOOM
import com.archivouni.guiauniversitaria.MapsActivity.Companion.MIN_ZOOM
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.TileProvider.NO_TILE
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class GoogleMapsTileProvider(private val mAssets: AssetManager) : TileProvider{

    companion object {
        private const val TAG = "GoogleMapsTileProvider"

        private const val MAP_TILES_DIRECTORY = "map_tiles_bmp"
        private const val TILE_WIDTH = 256
        private const val TILE_HEIGHT = 256
        private const val BUFFER_SIZE = 16 * 1024
        private val TILE_ZOOMS = SparseArray<Rect>().apply {
            put(14, Rect(5185, 7339, 5186, 7339))
            put(15, Rect(10371, 14678, 10373, 14679))
            put(16, Rect(20742, 29357, 20746, 29359))
            put(17, Rect(41485, 58714, 41492, 58718))
            put(18, Rect(82971, 117428, 82984, 117437))
            put(19, Rect(165942, 234855, 165968, 234875))
        }
    }


    override fun getTile(x: Int, y: Int, zoom: Int): Tile? {
        Log.d(TAG, "Getting tile at x:$x, y:$y, zoom:$zoom")
        val image = readTileImage(x, y, zoom) as ByteArray
        return if (checkTileExists(x, y, zoom)) Tile(TILE_WIDTH, TILE_HEIGHT, image) else NO_TILE
    }

    /**
     * Loads map tile image from assets folder and converts to ByteArray
     */
    private fun readTileImage(x: Int, y: Int, zoom: Int): ByteArray? {
        var inStream: InputStream? = null
        var buffer: ByteArrayOutputStream? = null

        try {
            inStream = mAssets.open(getTileFilename(x, y, zoom))
            buffer = ByteArrayOutputStream()

            var nRead: Int
            val data = ByteArray(BUFFER_SIZE)

            nRead = inStream.read(data, 0, BUFFER_SIZE)
            while (nRead != -1) {
                buffer.write(data, 0, nRead)
                nRead = inStream.read(data,0, BUFFER_SIZE)
            }
            buffer.flush()

            return buffer.toByteArray()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return null
        } finally {
            if (inStream != null) try {
                inStream.close()
            } catch (ignored: Exception) {}

            if (buffer != null) try {
                buffer.close()
            } catch (ignored: Exception) {}


        }
    }

    private fun getTileFilename(x: Int, y: Int, zoom: Int): String {
        return "$MAP_TILES_DIRECTORY/$zoom/$x/$y.png"
    }
    private fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
        val b = TILE_ZOOMS.get(zoom)
        return if (b == null && !(zoom < MIN_ZOOM || zoom > MAX_ZOOM)) false else (b.left <= x && x <= b.right && b.top <= y && y <= b.bottom)
    }
}