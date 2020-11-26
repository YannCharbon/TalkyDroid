package com.tmobop.talkydroid.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.adapters.ConversationListAdapter
import com.tmobop.talkydroid.adapters.OnItemClickListener
import com.tmobop.talkydroid.classes.ConversationUI
import java.util.*


class MainActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var conversationListAdapter: ConversationListAdapter
    private lateinit var conversationListRecyclerView: RecyclerView
    private lateinit var conversationListLayoutManager: LinearLayoutManager

    private lateinit var userUUID: String
    private lateinit var userName: String

    //-------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar_main))

        //------------------------------ Floating button ------------------------------------
        // TO CHANGE (test)
        findViewById<FloatingActionButton>(R.id.add_conversation_floating_button).setOnClickListener {
            val conversation = ConversationUI(
                title = "UserName",
                description = "Last message",
                time = Calendar.getInstance().timeInMillis,
                avatarID = 0
            )

            // Add to the adapter
            conversationListAdapter.addConversation(conversation)
        }

        //------------------------- get userName and userUUID -------------------------------
        userName = intent.getStringExtra(USER_NAME).toString()
        userUUID = intent.getStringExtra(USER_UUID).toString()

        //------------------------------ Conversation list ----------------------------------
        // Conversation recycler view
        conversationListRecyclerView = findViewById(R.id.conversationListRecyclerView)
        conversationListRecyclerView.setHasFixedSize(true)
        conversationListLayoutManager = LinearLayoutManager(this)
        conversationListRecyclerView.layoutManager = conversationListLayoutManager
        conversationListAdapter = ConversationListAdapter(this, this)
        conversationListRecyclerView.adapter = conversationListAdapter
    }

    //-------------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //-------------------------------------------------------------------
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    //-------------------------------------------------------------------
    // Click on conversation
    override fun onItemClick(item: ConversationUI, position: Int) {
        val intent = Intent(this, ConversationActivity::class.java)
        intent.putExtra(TITLE_TEXT, item.title)
        intent.putExtra(AVATAR_ID, item.avatarID)
        intent.putExtra(USER_NAME, userName)

        startActivityForResult(intent, START_CONVERSATION_REQUEST)
    }

    //-------------------------------------------------------------------
    companion object {
        var START_CONVERSATION_REQUEST = 0
        const val TITLE_TEXT = "title"
        const val AVATAR_ID = "avatar_id"
        const val USER_UUID = "user_uuid"
        const val USER_NAME = "user_name"
        const val RECEIVER_UUID = "receiver_uuid"
    }
}