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
import java.net.URL

object Tools {
//    fun loadImageFromURL(url: String): Bitmap {
//        return LoadImage().execute(URL(url)).get()
//    }

    class LoadImage: AsyncTask<String, Void, Bitmap>() {
        override fun doInBackground(vararg urls: String?): Bitmap {
            return BitmapFactory.decodeStream(URL(urls[0]!!).openConnection().getInputStream())
        }
    }
}