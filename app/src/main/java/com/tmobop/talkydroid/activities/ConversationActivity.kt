package com.tmobop.talkydroid.activities

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.adapters.App
import com.tmobop.talkydroid.adapters.ConversationAdapter
import com.tmobop.talkydroid.classes.MessageUI
import java.util.*

class ConversationActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var btnSendMessage: ImageButton
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRecyclerView: RecyclerView
    private lateinit var conversationLayoutManager: LinearLayoutManager

    //TO CHANGE
    private val userName = "PAUL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        //----------------------- Toolbar ------------------------------
        setSupportActionBar(findViewById(R.id.toolbar_conversation))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)       // Add a go back arrow on left of the title

        // TO DO --> Change the title with the name of the interlocutor
        supportActionBar?.title = "Interlocutor Name"

        //-------------------- Write text bar ----------------------------
        // TO CHANGE
        App.user = userName

        editText = findViewById(R.id.editTextConversation)
        btnSendMessage = findViewById(R.id.send_message_button)

        // Send message button
        btnSendMessage.setOnClickListener {
            if(editText.text.isNotEmpty()) {
                val message = MessageUI(
                    content = editText.text.toString(),
                    time = Calendar.getInstance().timeInMillis,
                    user = App.user,
                    messageType = MessageUI.TYPE_MESSAGE_SEND
                )

                // Add the new message to the conversation
                conversationAdapter.addMessage(message)

                // Scroll the RecyclerView to the last added element
                conversationRecyclerView.scrollToPosition(conversationAdapter.itemCount - 1);

                // Reset the keyboard
                resetKeyboard()

                // TO DO --> Push to database
            }
            else {
                Toast.makeText(applicationContext, "Message should not be empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Send image button
        // TO DO

        //--------------------- Conversation ----------------------------

        // Conversation recycler view
        conversationRecyclerView = findViewById(R.id.messageListRecyclerView)
        conversationRecyclerView.setHasFixedSize(true)
        conversationLayoutManager = LinearLayoutManager(this)
        conversationRecyclerView.layoutManager = conversationLayoutManager
        conversationAdapter = ConversationAdapter(this)
        conversationRecyclerView.adapter = conversationAdapter
    }

    private fun resetKeyboard() {

        // Clean text box
        editText.text.clear()

        // Hide keyboard
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(currentFocus!!.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}