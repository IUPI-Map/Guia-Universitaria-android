package com.archivouni.guiauniversitaria

/**
 * Created by Parsania Hardik on 03-Jan-17.
 */
class ImageModel {

    private var image_drawable: Int = 0

    fun getImage_drawables(): Int {
        println(image_drawable)
        return image_drawable
    }

    fun setImage_drawables(image_drawable: Int) {
        println("hello")
        println(this.image_drawable)


        this.image_drawable = image_drawable
    }
}