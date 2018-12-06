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
    private val inflater: LayoutInflater
    lateinit var actualImage: ImageView

    init {
        inflater = LayoutInflater.from(context)
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return imageModelArrayList.size
    }

    /*
      fun bindInfoToView(poi: PointOfInterest, view: View, map: GoogleMap)

      fun loadImageIntoView(url: String,
                          imageView: ImageView,
                          map: GoogleMap,
                          fit: Boolean = false,
                          width: Int = 500,
                          height: Int = 500)



     */



    override fun instantiateItem(view: ViewGroup, position: Int): Any {
        val imageLayout = inflater.inflate(R.layout.slidingimages_layout, view, false)!!

        val imageView = imageLayout
                .findViewById(R.id.image) as ImageView

        Util.bindInfoToView(pointOfInterest,imageView,map)

        imageView.setImageResource(imageModelArrayList[position].getImage_drawables())

        view.addView(imageLayout, 0)

        return Util.bindInfoToView(pointOfInterest,imageView,map)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {}

    override fun saveState(): Parcelable? {
        return null
    }


}