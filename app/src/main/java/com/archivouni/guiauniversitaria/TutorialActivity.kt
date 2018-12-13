package com.archivouni.guiauniversitaria

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_tutorial.*


class TutorialActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TutorialActivity"
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
        mScreenView.setImageResource(mTutorialScreens[mCurrentScreen])

        mNextButton = findViewById(R.id.tutorial_button_next)
        mNextButton.setOnClickListener {
            mCurrentScreen++
            mScreenView.setImageResource(mTutorialScreens[mCurrentScreen])
            mPrevButton.visibility = View.VISIBLE
            if (mCurrentScreen == mTutorialScreens.size - 1)
                it.visibility = View.GONE
        }

        mPrevButton = findViewById(R.id.tutorial_button_prev)
        mPrevButton.setOnClickListener {
            mCurrentScreen--
            mScreenView.setImageResource(mTutorialScreens[mCurrentScreen])
            mNextButton.visibility = View.VISIBLE
            if (mCurrentScreen == 0)
                it.visibility = View.GONE
        }
//        if (mCurrentScreen == 1){
//
//            textTutorial01.visibility = View.VISIBLE
//            textTutorial02.visibility = View.INVISIBLE
//            textTutorial03.visibility = View.INVISIBLE
//            textTutorial04.visibility = View.INVISIBLE
//            textTutorial05.visibility = View.INVISIBLE
//            textTutorial06.visibility = View.INVISIBLE
//        }
//        else if(mCurrentScreen == 2){
//            textTutorial02.visibility = View.VISIBLE
//            textTutorial01.visibility = View.INVISIBLE
//            textTutorial03.visibility = View.INVISIBLE
//            textTutorial04.visibility = View.INVISIBLE
//            textTutorial05.visibility = View.INVISIBLE
//            textTutorial06.visibility = View.INVISIBLE
//        }
//        else if(mCurrentScreen == 3){
//            textTutorial03.visibility = View.VISIBLE
//            textTutorial01.visibility = View.INVISIBLE
//            textTutorial02.visibility = View.INVISIBLE
//            textTutorial04.visibility = View.INVISIBLE
//            textTutorial05.visibility = View.INVISIBLE
//            textTutorial06.visibility = View.INVISIBLE
//        }
//        else if(mCurrentScreen == 4){
//            textTutorial04.visibility = View.VISIBLE
//            textTutorial01.visibility = View.INVISIBLE
//            textTutorial02.visibility = View.INVISIBLE
//            textTutorial03.visibility = View.INVISIBLE
//            textTutorial05.visibility = View.INVISIBLE
//            textTutorial06.visibility = View.INVISIBLE
//        }
//        else if(mCurrentScreen == 5){
//            textTutorial05.visibility = View.VISIBLE
//            textTutorial01.visibility = View.INVISIBLE
//            textTutorial02.visibility = View.INVISIBLE
//            textTutorial04.visibility = View.INVISIBLE
//            textTutorial03.visibility = View.INVISIBLE
//            textTutorial06.visibility = View.INVISIBLE
//        }
//        else{
//            textTutorial06.visibility = View.VISIBLE
//            textTutorial01.visibility = View.INVISIBLE
//            textTutorial02.visibility = View.INVISIBLE
//            textTutorial04.visibility = View.INVISIBLE
//            textTutorial05.visibility = View.INVISIBLE
//            textTutorial03.visibility = View.INVISIBLE
//        }

        mQuitButton = findViewById(R.id.tutorial_quit_button)
        mQuitButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
