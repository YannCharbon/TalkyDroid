package com.tmobop.talkydroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ConversationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        //----------------------- Toolbar ------------------------------
        setSupportActionBar(findViewById(R.id.toolbar_conversation))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)       // Add a go back arrow on left of the title

        // To do --> Change the title with the name of the interlocutor
        supportActionBar?.title = "Interlocutor Name"
    }
}