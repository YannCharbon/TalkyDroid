package com.tmobop.talkydroid.activities

import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.adapters.ConversationAdapter
import com.tmobop.talkydroid.adapters.ConversationListAdapter
import com.tmobop.talkydroid.adapters.OnItemClickListener
import com.tmobop.talkydroid.classes.ConversationUI
import java.util.*

class MainActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var conversationListAdapter: ConversationListAdapter
    private lateinit var conversationListRecyclerView: RecyclerView
    private lateinit var conversationListLayoutManager: LinearLayoutManager

    //-------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar_main))

        //------------------------------ Floating button ------------------------------------
        // TO CHANGE (test)
        findViewById<FloatingActionButton>(R.id.add_conversation_floating_button).setOnClickListener {
                val conversation = ConversationUI(
                    title = "Title 1",
                    description = "Description 1",
                    time = Calendar.getInstance().timeInMillis,
                    avatarID = 0
                )
                conversationListAdapter.addConversation(conversation)
        }

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

        startActivityForResult(intent, START_CONVERSATION_REQUEST)
    }

    //-------------------------------------------------------------------
    companion object {
        var START_CONVERSATION_REQUEST = 0
        const val TITLE_TEXT = "title"
        const val AVATAR_ID = "avatar_id"
    }
}