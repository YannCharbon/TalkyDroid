package com.tmobop.talkydroid.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.adapters.App
import com.tmobop.talkydroid.adapters.ConversationAdapter
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.MessageUI
import java.util.*


class ConversationActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var btnSendMessage: ImageButton
    private lateinit var btnSendFiles: ImageButton
    private lateinit var btnSendImage: ImageButton
    private lateinit var btnSendLocation: ImageButton
    private lateinit var btnSendDocument: ImageButton
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRecyclerView: RecyclerView
    private lateinit var conversationLayoutManager: LinearLayoutManager

    private val pickImage = 100
    private var imageUri: Uri? = null

    //TODO -> Change the username dynamically
    private val userName = "PAUL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        //----------------------- Toolbar ------------------------------
        setSupportActionBar(findViewById(R.id.toolbar_conversation))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)       // Add a go back arrow on left of the title

        // Title of the toolbar
        supportActionBar?.title = (intent.getStringExtra(MainActivity.TITLE_TEXT))

        //-------------------- Write text bar ----------------------------
        // TO CHANGE
        App.user = userName

        editText = findViewById(R.id.conversation_editText)
        btnSendMessage = findViewById(R.id.send_message_button)

        // Send message button
        btnSendMessage.setOnClickListener {
            if(editText.text.isNotEmpty()) {
                val message = MessageUI(
                    content = editText.text.toString(),
                    time = Calendar.getInstance().timeInMillis,
                    user = App.user,
                    messageType = MessageType.TEXT,
                    avatarID = 0
                )

                // TODO --> Send message to Hardware

                // TODO --> Wait for acknowledge

                // IF --> OK

                // Add the new message to the conversation
                conversationAdapter.addMessage(message)

                // Scroll the RecyclerView to the last added element
                conversationRecyclerView.scrollToPosition(conversationAdapter.itemCount - 1);

                // Reset the keyboard
                resetKeyboard()

                // TODO --> Push to database

                // IF --> NOK

                //
            }
            else {
                Toast.makeText(
                    applicationContext,
                    "Message should not be empty",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //-------------------- Send file button --------------------
        // Button
        btnSendFiles = findViewById(R.id.send_file_button)

        // Open the dialog box to choose the file type
        btnSendFiles.setOnClickListener {
            val dialogBuilder = AlertDialog.Builder(this)
            val inflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.choose_file_type, null)
            dialogView.setBackgroundColor(Color.TRANSPARENT)
            dialogBuilder.setView(dialogView)
            val alertDialog = dialogBuilder.create()
            alertDialog.show()

            //----------------------- Send image ----------------------------
            btnSendImage = dialogView.findViewById(R.id.send_image_button)

            btnSendImage.setOnClickListener {
                val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                startActivityForResult(gallery, pickImage)
                alertDialog.dismiss()
            }

            //---------------------- Send location --------------------------
            btnSendLocation = dialogView.findViewById(R.id.send_location_button)

            btnSendLocation.setOnClickListener {
                // TODO
                Toast.makeText(this, "SEND LOCATION", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }

            //------------------------ Send files ---------------------------
            btnSendDocument = dialogView.findViewById(R.id.send_document_button)

            btnSendDocument.setOnClickListener {
                // TODO
                Toast.makeText(this, "SEND DOCUMENT", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
        }

        //--------------------- Conversation ----------------------------

        // Conversation recycler view
        conversationRecyclerView = findViewById(R.id.messageListRecyclerView)
        conversationRecyclerView.setHasFixedSize(true)
        conversationLayoutManager = LinearLayoutManager(this)
        conversationRecyclerView.layoutManager = conversationLayoutManager
        conversationAdapter = ConversationAdapter(this)
        conversationRecyclerView.adapter = conversationAdapter
    }

    //---------------------------------------------------------------------
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    //---------------------------------------------------------------------
    private fun resetKeyboard() {

        // Clean text box
        editText.text.clear()

        // Hide keyboard
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus!!.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    //---------------------------------------------------------------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //------------------------ Send image -----------------------------
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            val message = MessageUI(
                content = imageUri.toString(),
                time = Calendar.getInstance().timeInMillis,
                user = App.user,
                messageType = MessageType.IMAGE,
                avatarID = 0
            )

            // TODO --> Send image

            // TODO --> Wait acknowledge

            // Add the new message to the conversation
            conversationAdapter.addMessage(message)

            // Scroll the RecyclerView to the last added element
            conversationRecyclerView.scrollToPosition(conversationAdapter.itemCount - 1);
        }
    }
}