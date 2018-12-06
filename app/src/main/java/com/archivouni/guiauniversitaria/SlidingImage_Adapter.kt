package com.archivouni.guiauniversitaria
import android.content.Context
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.viewpager.widget.PagerAdapter
import com.archivouni.guiauniversitaria.Util.loadImageIntoView
import com.google.android.gms.maps.GoogleMap
import java.util.ArrayList


class SlidingImage_Adapter(private val context: Context, private val imageModelArrayList : ArrayList<ImageModel>, val map:GoogleMap, var pointOfInterest : PointOfInterest) : PagerAdapter() {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    lateinit var actualImage: ImageView

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return imageModelArrayList.size
    }

    override fun instantiateItem(view: ViewGroup, position: Int) {
        val imageLayout = inflater.inflate(R.layout.slidingimages_layout, view, false)!!

        val imageView = imageLayout
                .findViewById(R.id.image) as ImageView


        imageView.setImageResource(imageModelArrayList[position].getImage_drawables())

        view.addView(imageLayout, 0)

    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {}

    override fun saveState(): Parcelable? {
        return null
    }


}