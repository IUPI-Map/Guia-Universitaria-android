package com.archivouni.guiauniversitaria

import com.google.android.gms.maps.model.LatLng

class PointOfInterest(val mName: String?, val mAcronym: String?, val mLatLng: LatLng?, val mType: TYPE = TYPE.DEFAULT) {
    enum class TYPE {
        DEFAULT,
        BUILDING,
        ARTWORK
    }
}