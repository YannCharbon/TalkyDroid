package com.tmobop.talkydroid.activities

import android.Manifest
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.activities.MainActivity.Companion.RECEIVER_UUID
import com.tmobop.talkydroid.activities.MainActivity.Companion.TITLE_TEXT
import com.tmobop.talkydroid.activities.MainActivity.Companion.USER_NAME
import com.tmobop.talkydroid.activities.MainActivity.Companion.USER_UUID
import com.tmobop.talkydroid.adapters.ConversationAdapter
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.services.NotificationService
import com.tmobop.talkydroid.storage.MessageEntity
import com.tmobop.talkydroid.storage.MessageViewModel
import com.tmobop.talkydroid.storage.UserEntity
import com.tmobop.talkydroid.storage.UserWithMessagesViewModel
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.List as List1


class ConversationActivity : AppCompatActivity(), ModRfUartManager.Listener {

    private lateinit var conversationEditText: EditText

    // Button variables
    private lateinit var btnSendMessage: ImageButton
    private lateinit var btnSendFiles: ImageButton
    private lateinit var btnSendImage: ImageButton
    private lateinit var btnSendLocation: ImageButton
    private lateinit var btnSendDocument: ImageButton
    private lateinit var btnBackImageButton: ImageButton
    private lateinit var btnOpenCamera : ImageButton
    private lateinit var btnSettings: ImageButton

    private lateinit var imageAvatarImageView: ImageView
    private lateinit var titleTextView: TextView

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var conversationRecyclerView: RecyclerView
    private lateinit var messagesViewModel: MessageViewModel
    private lateinit var userWithMessagesViewModel: UserWithMessagesViewModel

    private lateinit var mainActivity: Intent

    private var imageUriFromGallery: Uri? = null
    private var imageUriFromCamera: Uri? = null

    private lateinit var userUUID : String
    private lateinit var receiverUUID : String
    private lateinit var userName : String

    // Hardware variables
    private lateinit var mModRfUartManager: ModRfUartManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        //-------------------------------- Get datas from intent -----------------------------------
        userUUID = intent.getStringExtra(USER_UUID).toString()
        receiverUUID = intent.getStringExtra(RECEIVER_UUID).toString()
        userName = intent.getStringExtra(USER_NAME).toString()

        //------------------------------------ Notifications ---------------------------------------
        if (isMyServiceRunning(NotificationService::class.java)) {
            stopService(Intent(this, NotificationService::class.java))
        }

        //-------------------------------------- Hardware ------------------------------------------
        mModRfUartManager = ModRfUartManager(this, this)

        //------------------------------------ Conversation ----------------------------------------
        // Conversation recycler view
        conversationRecyclerView = findViewById(R.id.messageListRecyclerView)
        conversationRecyclerView.setHasFixedSize(true)
        conversationRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )

        // Conversation adapter
        conversationAdapter = ConversationAdapter(this, mutableListOf())
        conversationRecyclerView.adapter = conversationAdapter

        // Get the user UUID
        ConversationAdapter.App.userUUID = UUID.fromString(userUUID)

        // View model
        messagesViewModel = ViewModelProvider(this).get(MessageViewModel::class.java)
        userWithMessagesViewModel = ViewModelProvider(this).get(UserWithMessagesViewModel::class.java)

        // Get users from database
        messagesViewModel.getAllMessages().observe(this, { messages ->
            conversationAdapter.swapMessages(messages)

            // Scroll the RecyclerView to the last added element
            conversationRecyclerView.scrollToPosition(conversationAdapter.itemCount - 1)
        })

        //-------------------- Write text bar ----------------------------
        conversationEditText = findViewById(R.id.conversation_editText)
        btnSendMessage = findViewById(R.id.send_message_button)

        // Send message button
        btnSendMessage.setOnClickListener {
            if(conversationEditText.text.isNotEmpty()) {

                val message = MessageEntity(
                    messageId = null,
                    senderId = UUID.fromString(userUUID),
                    receiverId = UUID.fromString(receiverUUID),
                    content = conversationEditText.text.toString(),
                    time = Calendar.getInstance().timeInMillis,
                    messageType = MessageType.TEXT
                )

                // TODO --> Send message to Hardware
                mModRfUartManager.writeText(conversationEditText.text.toString(), receiverUUID)

                // TODO --> Wait for acknowledge

                // IF --> OK

                lifecycleScope.launch {
                    messagesViewModel.insertMessage(message)
                }

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
                val gallery = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI
                )
                startActivityForResult(gallery, PICK_IMAGE_FROM_GALLERY)
                alertDialog.dismiss()
            }

            //---------------------- Send location --------------------------
            btnSendLocation = dialogView.findViewById(R.id.send_location_button)

            btnSendLocation.setOnClickListener {
                // Check permissions
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_DENIED)
                {
                    // Permission denied
                    val permissions = arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )

                    // Show pop up to request permission
                    requestPermissions(permissions, PERMISSION_CODE_LOCATION)
                }
                else {
                    // Permission granted
                    shareLocation()
                }

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
                requestPermissions(permission, PERMISSION_CODE_CAMERA)
            } else {
                // Permission granted
                openCamera()
            }
        }

        //----------------------- Toolbar ------------------------------
        // Back button
        btnBackImageButton = findViewById(R.id.conversation_toolbar_back_arrow_imageButton)

        btnBackImageButton.setOnClickListener {
            mainActivity = Intent(this, MainActivity::class.java)
            mainActivity.putExtra(USER_UUID, userUUID)
            mainActivity.putExtra(USER_NAME, userName)
            startActivity(mainActivity)
            finish()
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

        // Stop the service
        stopService(Intent(this, NotificationService::class.java))

        // Get the hardware manager
        mModRfUartManager = ModRfUartManager(this, this)
    }

    //---------------------------------------------------------------------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //----------------------------- Send image from gallery ------------------------------------
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_FROM_GALLERY) {

            imageUriFromGallery = data?.data

            val message = MessageEntity(
                messageId = null,
                senderId = UUID.fromString(userUUID),
                receiverId = UUID.fromString(receiverUUID),
                content = imageUriFromGallery.toString(),
                time = Calendar.getInstance().timeInMillis,
                messageType = MessageType.IMAGE
            )

            // TODO --> Send image with hardware

            // TODO --> Wait acknowledge

            // Add message to database
            lifecycleScope.launch {
                messagesViewModel.insertMessage(message)
            }
        }

        //----------------------------- Send image from camera -------------------------------------
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE_FROM_CAMERA) {

            val message = MessageEntity(
                messageId = null,
                senderId = UUID.fromString(userUUID),
                receiverId = UUID.fromString(receiverUUID),
                content = imageUriFromCamera.toString(),
                time = Calendar.getInstance().timeInMillis,
                messageType = MessageType.IMAGE
            )

            // TODO --> Send image

            // TODO --> Wait acknowledge

            // Add message to database
            lifecycleScope.launch {
                messagesViewModel.insertMessage(message)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)
    }

    //--------------------------------------- Camera -----------------------------------------------
    private fun openCamera() {
        val contentCameraValues = ContentValues()
        contentCameraValues.put(MediaStore.Images.Media.TITLE, "New Picture")
        contentCameraValues.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        imageUriFromCamera = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentCameraValues
        )

        //Camera intent
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUriFromCamera)
        startActivityForResult(cameraIntent, PICK_IMAGE_FROM_CAMERA)
    }


    //----------------------------------- ShareLocation --------------------------------------------
    private fun shareLocation() {

        lateinit var fusedLocationProviderClient: FusedLocationProviderClient

        // Check permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission denied -> Nothing to do since we already prompt the user for permissions
        }
        else { // Permission granted -> Share location

            // Get the location provider
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            // Get the last location
            fusedLocationProviderClient.lastLocation.addOnSuccessListener {

                // Check if location is not null
                if (it != null) {

                    // Get latitude and longitude
                    val latitude = it.latitude.toString()
                    val longitude = it.longitude.toString()

                    // Display the coords
                    //Toast.makeText(this, "Latitude = $latitude, Longitude = $longitude", Toast.LENGTH_LONG).show()

                    // Create the message to send
                    val message = MessageEntity(
                        messageId = null,
                        senderId = UUID.fromString(userUUID),
                        receiverId = UUID.fromString(receiverUUID),
                        content = "$latitude,$longitude",
                        time = Calendar.getInstance().timeInMillis,
                        messageType = MessageType.LOCATION
                    )

                    // Send message to hardware
                    mModRfUartManager.writeText(message.content, receiverUUID, isLocation = true)

                    // Push to database
                    lifecycleScope.launch {
                        messagesViewModel.insertMessage(message)
                    }
                }
                else {
                    Toast.makeText(this, "The location could not be obtained...", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //-------------------------------------- Permissions -------------------------------------------
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            PERMISSION_CODE_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission from popup granted
                    openCamera()
                } else {
                    // Permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_CODE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission from popup granted
                    shareLocation()
                } else {
                    // Permission from popup was denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isMyServiceRunning(serviceClass : Class<*> ) : Boolean{
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    //-------------------------------------------------------------------
    companion object {
        const val PERMISSION_CODE_CAMERA = 1000
        const val PERMISSION_CODE_LOCATION = 1001
        const val PICK_IMAGE_FROM_GALLERY = 100
        const val PICK_IMAGE_FROM_CAMERA = 101
    }

    override fun onTextReceived(string: String, senderUUID: String) {

        runOnUiThread {
            val message = MessageEntity(
                messageId = null,
                senderId = UUID.fromString(receiverUUID),
                receiverId = UUID.fromString(userUUID),
                content = string,
                time = Calendar.getInstance().timeInMillis,
                messageType = MessageType.TEXT
            )

            // Add message received to database
            lifecycleScope.launch {
                messagesViewModel.insertMessage(message)
            }
        }
    }

    override fun onLocationReceived(location: String, senderUUID: String) {
        runOnUiThread {
            val message = MessageEntity(
                messageId = null,
                senderId = UUID.fromString(receiverUUID),
                receiverId = UUID.fromString(userUUID),
                content = location,
                time = Calendar.getInstance().timeInMillis,
                messageType = MessageType.LOCATION
            )

            // Add message received to database
            lifecycleScope.launch {
                messagesViewModel.insertMessage(message)
            }
        }
    }

    override fun onError(customText: String, e: Exception) {
        TODO("Not yet implemented")
    }

    override fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int) {
        TODO("Not yet implemented")
    }

    override fun onDiscoverFinished(devicesFoundInChannel: ArrayList<ModRfUartManager.DeviceInChannel>) {
        TODO("Not yet implemented")
    }

    override fun onDeviceJoinedNetwork(device: ModRfUartManager.DeviceInChannel) {
        runOnUiThread {
            Toast.makeText(this, "New device joined network", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            val newConversation = UserEntity(
                UUID.fromString(device.userUUID),
                device.userName,
                "",
                online = true
            )
            userWithMessagesViewModel.insertUser(newConversation)
        }
    }

    override fun onDeviceAttached() {
        runOnUiThread {
            Toast.makeText(this, "Device attached", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeviceDetached() {
        runOnUiThread {
            Toast.makeText(this, "Device detached", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeviceOpened() {
        runOnUiThread {
            Toast.makeText(this, "Device opened", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeviceOpenError() {
        runOnUiThread {
            Toast.makeText(this, "Device open error", Toast.LENGTH_SHORT).show()
        }
    }
}
