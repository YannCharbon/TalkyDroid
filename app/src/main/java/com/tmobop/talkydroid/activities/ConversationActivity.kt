package com.tmobop.talkydroid.activities

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.activities.MainActivity.Companion.TITLE_TEXT
import com.tmobop.talkydroid.activities.MainActivity.Companion.USER_NAME
import com.tmobop.talkydroid.adapters.App
import com.tmobop.talkydroid.adapters.ConversationAdapter
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.MessageUI
import java.util.*


class ConversationActivity : AppCompatActivity() {

    private lateinit var conversationEditText: EditText
    private lateinit var btnSendMessage: ImageButton
    private lateinit var btnSendFiles: ImageButton
    private lateinit var btnSendImage: ImageButton
    private lateinit var btnSendLocation: ImageButton
    private lateinit var btnSendDocument: ImageButton
    private lateinit var btnBackImageButton: ImageButton
    private lateinit var btnOpenCamera : ImageButton
    private lateinit var imageAvatarImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRecyclerView: RecyclerView
    private lateinit var conversationLayoutManager: LinearLayoutManager
    private lateinit var mainActivity: Intent

    private var imageUriFromGallery: Uri? = null
    private var imageUriFromCamera: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        //----------------------- Toolbar ------------------------------
        // Back button
        btnBackImageButton = findViewById(R.id.conversation_toolbar_back_arrow_imageButton)

        btnBackImageButton.setOnClickListener {
            mainActivity = Intent(this, MainActivity::class.java)
            startActivity(mainActivity)
        }

        // Avatar image
        imageAvatarImageView = findViewById(R.id.conversation_toolbar_avatar_imageView)
        // TODO --> set the image view to  avatar image

        // Title edit text
        titleTextView = findViewById(R.id.conversation_toolbar_title_textView)
        titleTextView.text = intent.getStringExtra(TITLE_TEXT)

        // Settings button
        btnSettings = findViewById(R.id.conversation_toolbar_settings_button)

        // TODO --> Do something when click on settings button


        //-------------------- Write text bar ----------------------------
        App.user = intent.getStringExtra(USER_NAME).toString()

        conversationEditText = findViewById(R.id.conversation_editText)
        btnSendMessage = findViewById(R.id.send_message_button)

        // Send message button
        btnSendMessage.setOnClickListener {
            if(conversationEditText.text.isNotEmpty()) {
                val message = MessageUI(
                    content = conversationEditText.text.toString(),
                    time = Calendar.getInstance().timeInMillis,
                    user = App.user,
                    messageType = MessageType.TEXT,
                    avatarID = 0
                )

                // TODO --> Send message to Hardware

                // TODO --> Wait for acknowledge

                // IF --> OK

                // Push message to data base
                //Storage.writeData(this, message, message.receiverUUID.toString())

                // Add the new message to the conversation
                conversationAdapter.addMessage(message)

                // Scroll the RecyclerView to the last added element
                conversationRecyclerView.scrollToPosition(conversationAdapter.itemCount - 1);

                // Reset the keyboard
                resetKeyboard()

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
                startActivityForResult(gallery, PICK_IMAGE_FROM_GALLERY)
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

        //------------------------- Open camera -------------------------
        // Capture button
        btnOpenCamera = findViewById(R.id.open_camera_imageButton)

        btnOpenCamera.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED
                ) {
                    // Permission denied
                    val permission = arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )

                    // Show pop up to request permission
                    requestPermissions(permission, PERMISSION_CODE)
                } else {
                    // Permission granded
                    openCamera()
                }
            } else {
                // system OS not supported
                openCamera()
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
        conversationEditText.text.clear()

        // Hide keyboard
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            currentFocus!!.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    //--------------------------------------------------------------------
    override fun onResume() {
        super.onResume()

    }

    //---------------------------------------------------------------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //----------------------------- Send image from gallery ------------------------------------
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_FROM_GALLERY) {

            imageUriFromGallery = data?.data

            val message = MessageUI(
                content = imageUriFromGallery.toString(),
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

        //----------------------------- Send image from camera -------------------------------------
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_FROM_CAMERA) {

            val message = MessageUI(
                content = imageUriFromCamera.toString(),
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

    //--------------------------------------- Camera -----------------------------------------------
    private fun openCamera() {
        val contentCameraValues = ContentValues()
        contentCameraValues.put(MediaStore.Images.Media.TITLE, "New Picture")
        contentCameraValues.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUriFromCamera = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentCameraValues)

        //Camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriFromCamera)
        startActivityForResult(cameraIntent, PICK_IMAGE_FROM_CAMERA)
    }

    //-------------------------------------- Permissions -------------------------------------------
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission from popup granted
                    openCamera()
                }
                else {
                    // Permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //-------------------------------------------------------------------
    companion object {
        const val PERMISSION_CODE = 1000
        const val PICK_IMAGE_FROM_GALLERY = 100
        const val PICK_IMAGE_FROM_CAMERA = 101
    }
}