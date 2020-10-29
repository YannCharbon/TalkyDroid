package com.tmobop.talkydroid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        //-------------- Welcome activity ------------------
        handler = Handler()
        handler.postDelayed({

            val intent = Intent(this, ConversationActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)
    }
}