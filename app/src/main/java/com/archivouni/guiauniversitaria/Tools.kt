package com.archivouni.guiauniversitaria

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.AsyncTask
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import java.net.URL

object Tools {
    fun loadImageFromURL(url: URL): Bitmap {
        return LoadImage().execute(url).get()
    }

    private class LoadImage: AsyncTask<URL, Void, Bitmap>() {
        override fun doInBackground(vararg url: URL?): Bitmap {
            return loadImageFromURL(url[0]!!)
        }
    }
}