package com.tmobop.talkydroid.activities

import android.Manifest
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
import com.squareup.picasso.Picasso
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.activities.MainActivity.Companion.RECEIVER_UUID
import com.tmobop.talkydroid.activities.MainActivity.Companion.TITLE_TEXT
import com.tmobop.talkydroid.activities.MainActivity.Companion.USER_NAME
import com.tmobop.talkydroid.activities.MainActivity.Companion.USER_UUID
import com.tmobop.talkydroid.adapters.ConversationAdapter
import com.tmobop.talkydroid.classes.CircleTransformation
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.services.NotificationService
import com.tmobop.talkydroid.services.SingletonServiceManager
import com.tmobop.talkydroid.storage.MessageEntity
import com.tmobop.talkydroid.storage.MessageViewModel
import com.tmobop.talkydroid.storage.UserEntity
import com.tmobop.talkydroid.storage.UserWithMessagesViewModel
import kotlinx.coroutines.launch
import java.util.*


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
    private lateinit var imageOnlineImageView: ImageView
    private lateinit var conversationToolbar: Toolbar
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
    private var isUserOnline: Boolean = false

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
        if (SingletonServiceManager.isMyServiceRunning) {
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

        // Observe the database to see if user become online or if userAvatar change
        userWithMessagesViewModel.getAllUsersWithMessages().observe(this, {usersWithMessage ->

            // Get the senderEntity
            val senderEntity = usersWithMessage.findLast { userWithMessages ->
                userWithMessages.userEntity!!.userId == UUID.fromString(receiverUUID)
            }

            // Get the avatar imageView
            imageAvatarImageView = findViewById(R.id.conversation_toolbar_avatar_imageView)

            // Get the online/offline imageView
            imageOnlineImageView = findViewById(R.id.conversation_toolbar_online)

            // Get the conversation toolbar
            conversationToolbar = findViewById(R.id.conversation_toolbar)

            // Set the avatar imageView
            if (senderEntity != null) {
                if (senderEntity.userEntity?.avatar == "") {
                    imageAvatarImageView.setImageResource(R.drawable.ic_baseline_unknown_user)
                }
                else {
                    // Round image
                    Picasso.get()
                        .load(Uri.parse(senderEntity.userEntity!!.avatar))
                        .error(R.drawable.ic_baseline_unknown_user)
                        .placeholder(R.drawable.ic_baseline_unknown_user)
                        .transform(CircleTransformation())
                        .into(imageAvatarImageView)
                }
            }

            // Set the online logo
            if (senderEntity != null) {
                if (senderEntity.userEntity?.online == true) {
                    imageOnlineImageView.setImageResource(R.drawable.ic_baseline_online)
                    btnSendMessage.setBackgroundResource(R.drawable.ic_baseline_send_background)
                    imageAvatarImageView.alpha = 1F
                    isUserOnline = true
                    conversationEditText.isEnabled = true
                }
                else {
                    imageOnlineImageView.setImageResource(R.drawable.ic_baseline_offline)
                    btnSendMessage.setBackgroundResource(R.drawable.ic_baseline_cannotsend_background)
                    imageAvatarImageView.alpha = 0.5F
                    isUserOnline = false
                    conversationEditText.isEnabled = false
                }
            }
        })

        //-------------------- Write text bar ----------------------------
        conversationEditText = findViewById(R.id.conversation_editText)

        btnSendMessage = findViewById(R.id.send_message_button)

        // Send message button
        btnSendMessage.setOnClickListener {
            if (isUserOnline) {
                if (conversationEditText.text.isNotEmpty()) {
                    val message = MessageEntity(
                        messageId = null,
                        senderId = UUID.fromString(userUUID),
                        receiverId = UUID.fromString(receiverUUID),
                        content = conversationEditText.text.toString(),
                        time = Calendar.getInstance().timeInMillis,
                        messageType = MessageType.TEXT
                    )

                    // Send message to Hardware
                    mModRfUartManager.writeText(conversationEditText.text.toString(), receiverUUID)

                    // Push to database
                    lifecycleScope.launch {
                        messagesViewModel.insertMessage(message)
                    }

                    // Reset the keyboard
                    resetKeyboard()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Message should not be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else {
                Toast.makeText(
                    applicationContext,
                    "The user is offline",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //-------------------- Send file button --------------------
        // Button
        btnSendFiles = findViewById(R.id.send_file_button)

        // Open the dialog box to choose the file type
        btnSendFiles.setOnClickListener {
            if (isUserOnline) {
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
                        == PackageManager.PERMISSION_DENIED
                    ) {
                        // Permission denied
                        val permissions = arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )

                        // Show pop up to request permission
                        requestPermissions(permissions, PERMISSION_CODE_LOCATION)
                    } else {
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
            else {
                Toast.makeText(
                    applicationContext,
                    "The user is offline",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //------------------------- Open camera -------------------------
        // Capture button
        btnOpenCamera = findViewById(R.id.open_camera_imageButton)

        btnOpenCamera.setOnClickListener {
            if (isUserOnline) {
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
            else {
                Toast.makeText(
                    applicationContext,
                    "The user is offline",
                    Toast.LENGTH_SHORT
                ).show()
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

        // Title edit text
        titleTextView = findViewById(R.id.conversation_toolbar_title_textView)
        titleTextView.text = intent.getStringExtra(TITLE_TEXT)

        // Settings button
        btnSettings = findViewById(R.id.conversation_toolbar_settings_button)
        btnSettings.setOnClickListener {
            showSettingsPopup(btnSettings)
        }
    }

    //---------------------------------------------------------------------
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            finish() // close this activity and return to previous activity (if there is any)
        }

        return super.onOptionsItemSelected(item)
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

            // TODO --> Send image with hardware

            // Add message to database
            lifecycleScope.launch {
                messagesViewModel.insertMessage(message)
            }
        }

        //------------------------------- Set senderAvatar -----------------------------------------
        if (resultCode == RESULT_OK && requestCode == SET_SENDER_AVATAR_FROM_GALLERY) {

            imageUriFromGallery = data?.data

            // Add user avatar to gallery
            lifecycleScope.launch {
                userWithMessagesViewModel.setUserAvatar(UUID.fromString(receiverUUID), imageUriFromGallery.toString())
            }
        }
    }

    //------------------------------------- onPause ------------------------------------------------
    override fun onPause() {
        super.onPause()

        // start the notification service if it not already running
        if (!SingletonServiceManager.isMyServiceRunning) {
            val serviceIntent = Intent(this, NotificationService::class.java)
            startService(serviceIntent)
        }
    }

    //-------------------------------------- onStop ------------------------------------------------
    override fun onStop() {
        super.onStop()

        // start the notification service if it not already running
        if (!SingletonServiceManager.isMyServiceRunning) {
            val serviceIntent = Intent(this, NotificationService::class.java)
            startService(serviceIntent)
        }
    }

    //-------------------------------------- onResume ----------------------------------------------
    override fun onResume() {
        super.onResume()

        // Stop the service if it is running
        if (SingletonServiceManager.isMyServiceRunning) {
            stopService(Intent(this, NotificationService::class.java))
        }

        // Get the hardware manager
        mModRfUartManager = ModRfUartManager(this, this)
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

    //------------------------------------ Settings menu -------------------------------------------
    private fun showSettingsPopup(view: View) {
        // Get the popUpMenu
        val popup = PopupMenu(this, view)

        // Inflate the menu
        popup.inflate(R.menu.conversation_settings_menu)

        // Set on Item click listener
        popup.setOnMenuItemClickListener { item: MenuItem? ->

            when (item!!.itemId) {

                // Delete all messages in conversation
                R.id.conversation_settings_deletaAllMessages -> {
                    Toast.makeText(this, "All messages deleted", Toast.LENGTH_SHORT).show()
                    lifecycleScope.launch {
                        userWithMessagesViewModel.deleteAllMessagesInConversation(
                            UUID.fromString(
                                receiverUUID
                            )
                        )
                    }
                }

                // Set sender avatar
                R.id.conversation_settings_setSenderAvatar -> {
                    // Open the gallery
                    val gallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    startActivityForResult(gallery, SET_SENDER_AVATAR_FROM_GALLERY)
                }
            }
            true
        }

        popup.show()
    }

    //-------------------------------------------------------------------
    companion object {
        const val PERMISSION_CODE_CAMERA = 1000
        const val PERMISSION_CODE_LOCATION = 1001
        const val PICK_IMAGE_FROM_GALLERY = 100
        const val PICK_IMAGE_FROM_CAMERA = 101
        const val SET_SENDER_AVATAR_FROM_GALLERY = 102
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
        return
    }

    override fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int) {
        return
    }

    override fun onDiscoverFinished(devicesFoundInChannel: ArrayList<ModRfUartManager.DeviceInChannel>) {
        return
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
            userWithMessagesViewModel.setUserOnline(UUID.fromString(device.userUUID))
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

            lifecycleScope.launch {
                userWithMessagesViewModel.setAllUsersOffline()
            }
        }
    }

    override fun onDeviceOpened() {
        runOnUiThread {
            Toast.makeText(this, "Device opened", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDeviceOpenError() {
        runOnUiThread {
            Toast.makeText(this, "Cannot open Device : permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
