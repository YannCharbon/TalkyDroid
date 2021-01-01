package com.tmobop.talkydroid.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.tmobop.talkydroid.R

class WelcomeActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    private val userINFO : String = "user_information"
    private lateinit var sharedPreferences: SharedPreferences
    private val userNameKey = "userNameKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        //-------------- Welcome activity ------------------
        handler = Handler()
        handler.postDelayed({

            // Get shared preferences
            sharedPreferences = getSharedPreferences(userINFO, Context.MODE_PRIVATE)

            // If user already register -> start mainActivity, else -> start loginActivity
            if (sharedPreferences.contains(userNameKey)) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 1000)
    }
}