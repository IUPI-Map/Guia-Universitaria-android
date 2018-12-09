package com.archivouni.guiauniversitaria
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter

class SlidingImageAdapter(private val context: Context, private val urls: Array<String>) : PagerAdapter() {
    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as View)
    }

    override fun getCount(): Int {
        return urls.size
    }

    override fun instantiateItem(view: ViewGroup, position: Int) : Any {
        val imageView = ImageView(context)

        Util.loadImageIntoView(urls[position], imageView)
        view.addView(imageView)
        return imageView
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view == obj
    }

}