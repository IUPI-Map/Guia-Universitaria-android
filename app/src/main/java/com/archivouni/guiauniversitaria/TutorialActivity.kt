package com.archivouni.guiauniversitaria

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import com.google.android.gms.maps.model.BitmapDescriptor

class TutorialActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TutorialActivity"

        private const val SCREEN_COUNT = 4
    }

    private val mTutorialScreens = arrayOf(R.drawable.tutorial01,
            R.drawable.tutorial02,
            R.drawable.tutorial03,
            R.drawable.tutorial04,
            R.drawable.tutorial05,
            R.drawable.tutorial06)

    private lateinit var mScreenView: ImageView
    private lateinit var mNextButton: ImageButton
    private lateinit var mPrevButton: ImageButton
    private lateinit var mQuitButton: ImageButton
    private var mCurrentScreen = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        mScreenView = findViewById(R.id.tutorial_image)

        mNextButton = findViewById(R.id.tutorial_button_next)
        mNextButton.setOnClickListener {
            if (++mCurrentScreen == mTutorialScreens.size)
                it.visibility = View.GONE
            mPrevButton.visibility = View.VISIBLE
            mScreenView.setImageResource(mTutorialScreens[mCurrentScreen])
        }

        mPrevButton = findViewById(R.id.tutorial_button_prev)
        mPrevButton.setOnClickListener {
            if (--mCurrentScreen == 0)
                it.visibility = View.GONE
            mNextButton.visibility = View.VISIBLE
            mScreenView.setImageResource(mTutorialScreens[mCurrentScreen])
        }
        mQuitButton = findViewById(R.id.tutorial_quit_button)
        mQuitButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
