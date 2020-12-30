package com.tmobop.talkydroid.activities

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.adapters.ConversationListAdapter
import com.tmobop.talkydroid.classes.*
import com.tmobop.talkydroid.services.NotificationService
import com.tmobop.talkydroid.storage.*
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.util.*

class MainActivity : AppCompatActivity(), ModRfUartManager.Listener {

    // Conversation adapter variables
    private lateinit var conversationListAdapter: ConversationListAdapter
    private lateinit var conversationListRecyclerView: RecyclerView

    // Hardware variables
    lateinit var mModRfUartManager: ModRfUartManager
    private lateinit var dialogViewConfigUsb: View
    private lateinit var timerScanningError: CountDownTimer
    private var channelScanIsRunning = false
    private lateinit var alertDialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    var connectedDeviceInfoText = ""
    private var isDeviceConnected: Boolean = false
    private var isDeviceOpen: Boolean = false
    private var isDeviceError: Boolean = false

    // Buttons variables
    lateinit var btnSettings: ImageButton
    lateinit var btnScanChannels: ImageButton

    // ViewModel variables
    private lateinit var messagesViewModel: MessageViewModel

    // Notifications variables
    private var notificationManager: NotificationManager? = null
    private val notificationId = 101


    //-------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //------------------------------- Settings button -------------------------------
        btnSettings = findViewById(R.id.toolbar_main_imageView_settings)
        btnSettings.setOnClickListener{
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
        }

        //--------------------------- Button scan channels  ----------------------------
        btnScanChannels = findViewById(R.id.toolbar_main_imageView_usbConnected)

        btnScanChannels.setOnClickListener {
            if (isDeviceConnected) {
                if (isDeviceOpen) {
                    scanChannels()
                }
                else {
                    // If device not open, try to open it
                    mModRfUartManager.getDevice()
                }
            }
            else {
                Toast.makeText(this, "The device is not connected...", Toast.LENGTH_SHORT).show()
            }
        }

        //-------------------------------ModRfUartManager -------------------------------
        mModRfUartManager = ModRfUartManager(this, this)
        mModRfUartManager.companion.userName = intent.getStringExtra(USER_NAME).toString()

        // Check if USB connected
        when(mModRfUartManager.getDevice()) {
            NO_DRIVER_AVAILABLE -> {
                isDeviceConnected = false
                isDeviceOpen = false
                Toast.makeText(this, "Device not connected", Toast.LENGTH_SHORT).show()
                btnScanChannels.setImageResource(R.drawable.ic_baseline_settings_remote_red)
            }
            GET_UART_DEVICES_SUCCESS -> {
                isDeviceConnected = true
                btnScanChannels.setImageResource(R.drawable.ic_baseline_settings_remote_green)
            }
        }

        //------------------------- get userName and userUUID -------------------------------
        val userName = intent.getStringExtra(USER_NAME).toString()
        val userUUID = mModRfUartManager.companion.userUUID

        //------------------------------ Conversation list ----------------------------------
        // Conversation recycler view
        conversationListRecyclerView = findViewById(R.id.conversationListRecyclerView)
        conversationListRecyclerView.setHasFixedSize(true)
        conversationListRecyclerView.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        conversationListRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                RecyclerView.VERTICAL
            )
        )

        conversationListAdapter = ConversationListAdapter(mutableListOf())
        conversationListRecyclerView.adapter = conversationListAdapter

        // on item click listener -> Start conversation activity
        conversationListAdapter.setOnConversationClickListener { userWithMessage ->
            val intent = Intent(this, ConversationActivity::class.java)
            intent.putExtra(TITLE_TEXT, userWithMessage.userEntity!!.userName)
            intent.putExtra(AVATAR_ID, userWithMessage.userEntity!!.avatar)
            intent.putExtra(USER_NAME, userName)
            intent.putExtra(USER_UUID, userUUID)
            intent.putExtra(RECEIVER_UUID, userWithMessage.userEntity!!.userId.toString())
            startActivityForResult(intent, START_CONVERSATION_REQUEST)
        }

        // View model
        userWithMessagesViewModel = ViewModelProvider(this).get(UserWithMessagesViewModel::class.java)
        messagesViewModel = ViewModelProvider(this).get(MessageViewModel::class.java)

        // Set all users to be offline
        lifecycleScope.launch {
            userWithMessagesViewModel!!.setAllUsersOffline()
        }

        // Display users from database
        userWithMessagesViewModel!!.getAllUsersWithMessages().observe(this, { usersWithMessages ->

            // Remove the application user from the list since we don't want to show it in the list
            //val idxApplicationUser = usersWithMessages.indexOfFirst{UserWithMessages().userEntity!!.userId == UUID.fromString(userUUID)}
            val conversationsShown: List<UserWithMessages> =
                usersWithMessages.filter { userWithMessages ->
                    userWithMessages.userEntity!!.userId != UUID.fromString(userUUID)
                }

            conversationListAdapter.swapConversations(conversationsShown)
        })

        // Add the user to database if he is new
        lifecycleScope.launch {
            val applicationUser = UserEntity(UUID.fromString(userUUID), userName, "", online = true)
            userWithMessagesViewModel!!.insertUser(applicationUser)      // If the user already exists, this line is ignored
        }

        //-------------------------------- Notifications ------------------------------------
        //notificationManager =
        //    getSystemService(
        //        Context.NOTIFICATION_SERVICE
        //    ) as NotificationManager

        //createNotificationChannel(
        //    CHANNEL_ID,
        //    "TalkyDroid_channel",
        //    "TalkyDroid_description"
        //)

        // Stop the notification service if it is running
        if (isMyServiceRunning(NotificationService::class.java)) {
            stopService(Intent(this, NotificationService::class.java))
        }

        //------------------------------ Floating button --------------------------------
        val floatingButton = findViewById<FloatingActionButton>(R.id.add_conversation_floating_button)

        floatingButton.setOnClickListener{
            // TODO -> Remove
            //mModRfUartManager.writeText("coucou", "289f909c-8263-4ea9-894f-868a067924df")

            // Notifications
            //sendNotification(content = "salut", senderName = "Paul")

            // New conversation
            //val user1 = UserEntity(UUID.randomUUID(), "Michel", "")
            //lifecycleScope.launch{
            //    userWithMessagesViewModel.insertUser(user1)
            //}
        }
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
        when (item.itemId) {
            R.id.action_settings -> {


                return true
            }
            R.id.action_modrf_config -> {


                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        val serviceIntent = Intent(this, NotificationService::class.java)
        startService(serviceIntent)
    }

    override fun onResume() {
        super.onResume()

        // Stop the service
        stopService(Intent(this, NotificationService::class.java))

        // Get the hardware manager
        mModRfUartManager = ModRfUartManager(this, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Change context back to main activity when returning from conversation activity
        mModRfUartManager = ModRfUartManager(this, this)
    }

    //-------------------------------------------------------------------
    companion object {
        var START_CONVERSATION_REQUEST = 0
        const val TITLE_TEXT = "title"
        const val AVATAR_ID = "avatar_id"
        const val USER_UUID = "user_uuid"
        const val USER_NAME = "user_name"
        const val RECEIVER_UUID = "receiver_uuid"
        const val CHANNEL_ID = "channel_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
        var userWithMessagesViewModel: UserWithMessagesViewModel? = null
    }

    //-------------------------------------------------------------------
    override fun onTextReceived(string: String, senderUUID: String) {
        runOnUiThread {
            if (senderUUID == "") {
                Toast.makeText(this, "Message received from unknown device. please rescan channel", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(
                    this,
                    "Received data from $senderUUID : $string",
                    Toast.LENGTH_SHORT
                ).show()

                val message = MessageEntity(
                    messageId = null,
                    senderId = UUID.fromString(senderUUID),
                    receiverId = UUID.fromString(mModRfUartManager.companion.userUUID),
                    content = string,
                    time = Calendar.getInstance().timeInMillis,
                    messageType = MessageType.TEXT
                )

                // Insert message to database
                lifecycleScope.launch {
                    // Insert message to database
                    messagesViewModel.insertMessage(message)
                }
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onLocationReceived(location: String, senderUUID: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                "Received location from $senderUUID : $location",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //-------------------------------------------------------------------
    override fun onError(customText: String, e: Exception) {
        runOnUiThread {
            Toast.makeText(this, customText + e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    //-------------------------------------------------------------------
    override fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int) {
        runOnUiThread{
            val progressBar = dialogViewConfigUsb.findViewById<ProgressBar>(R.id.main_dialog_progressbar_channel_scanning)
            progressBar.progress = currentAddress
        }
    }

    //-------------------------------------------------------------------
    override fun onDiscoverFinished(devicesFoundInChannel: ArrayList<ModRfUartManager.DeviceInChannel>) {
        runOnUiThread {
            channelScanIsRunning = false
            Toast.makeText(
                this,
                "Discovered " + devicesFoundInChannel.size + " device(s) in channel",
                Toast.LENGTH_SHORT
            ).show()

            // Close the dialog box
            alertDialog.dismiss()

            // Stop the timer
            timerScanningError.cancel()
        }

        // Add the new user to database
        lifecycleScope.launch {
            for (device in devicesFoundInChannel) {
                val newConversation = UserEntity(
                    UUID.fromString(device.userUUID),
                    device.userName,
                    "",
                    true
                )
                userWithMessagesViewModel!!.insertUser(newConversation)
                userWithMessagesViewModel!!.setUserOnline(UUID.fromString(device.userUUID))
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onDeviceJoinedNetwork(device: ModRfUartManager.DeviceInChannel) {
        runOnUiThread {
            Toast.makeText(this, "New device joined network", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            val newConversation = UserEntity(
                UUID.fromString(device.userUUID),
                device.userName,
                "",
                true
            )
            userWithMessagesViewModel!!.insertUser(newConversation)
            userWithMessagesViewModel!!.setUserOnline(UUID.fromString(device.userUUID))
        }
    }

    //-------------------------------------------------------------------
    override fun onDeviceAttached() {

        // Toggle the device attached logo to green
        btnScanChannels.setImageResource(R.drawable.ic_baseline_settings_remote_green)

        // Toggle device connected variable
        isDeviceConnected = true

        // Display a toast to inform the user that the device is attached
        runOnUiThread {
            Toast.makeText(this, "Device attached", Toast.LENGTH_SHORT).show()
            when (mModRfUartManager.getDevice()) {
                NO_DRIVER_AVAILABLE -> Toast.makeText(this, "The driver is not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onDeviceDetached() {

        // Toggle the device attached logo to red
        btnScanChannels.setImageResource(R.drawable.ic_baseline_settings_remote_red)

        // Toggle device connected variable
        isDeviceConnected = false
        isDeviceOpen = false

        // Display a toast to inform the user that the device is detached
        runOnUiThread {
            if (channelScanIsRunning) {
                Toast.makeText(
                    this,
                    "Device detached, the scanning as not been finished...",
                    Toast.LENGTH_SHORT
                ).show()
                timerScanningError.cancel()
                mModRfUartManager.close()
                channelScanIsRunning = false
            }
            else {
                Toast.makeText(this, "Device detached", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onDeviceOpened() {

        runOnUiThread{
            // Anti rebound
            if (!isDeviceOpen) {

                // Display a toast for information
                Toast.makeText(this, "Device opened", Toast.LENGTH_SHORT).show()

                // Toggle device connected variable
                isDeviceOpen = true

                // Wait 1s before launching the scan to be sure everything is ok (anti-rebound)
                sleep(2000)

                // Scan channels
                scanChannels()
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onDeviceOpenError() {

        // Toggle device connected variable
        isDeviceOpen = true

        runOnUiThread {
            Toast.makeText(this, "Cannot open Device : permission denied", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
        }
    }

    //-------------------------------------------------------------------
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
    private fun scanChannels() {

        runOnUiThread {
            // Check if a scan is already running
            if (!channelScanIsRunning) {

                // Set a variable to prevent multiple scanning at same time
                channelScanIsRunning = true

                // Display an alertDialog box with the scanning bar progress
                val dialogBuilder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                dialogViewConfigUsb = inflater.inflate(R.layout.main_dialog_scan_channel, null)
                dialogViewConfigUsb.setBackgroundColor(Color.TRANSPARENT)
                dialogBuilder.setView(dialogViewConfigUsb)
                alertDialog = dialogBuilder.create()
                alertDialog.show()

                // If the scanning take too long, -> if kill it
                timerScanningError = object : CountDownTimer(7000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // Nothing to do here
                    }

                    override fun onFinish() {
                        // Display a Toast to inform the user
                        Toast.makeText(applicationContext, "The channel scanning take too long, please reconnect the device", Toast.LENGTH_LONG).show()

                        // Hide the dialog box
                        alertDialog.dismiss()

                        // Close the modRFManager
                        mModRfUartManager.close()

                        // Set channel scanning to false
                        channelScanIsRunning = false
                    }
                }
                timerScanningError.start()

                // Discover all devices in network
                mModRfUartManager.discoverNetworkDevicesAndGetAddress(true)
            }
            else {
                Toast.makeText(this, "The scan is already running", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //-------------------------------------------------------------------
    //private fun createNotificationChannel(id: String, name: String, description: String) {
//
    //    val importance = NotificationManager.IMPORTANCE_HIGH
    //    val channel = NotificationChannel(id, name, importance)
//
    //    channel.description = description
    //    channel.enableLights(true)
    //    channel.lightColor = Color.BLUE
    //    channel.enableVibration(true)
    //    channel.vibrationPattern =
    //        longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
//
    //    notificationManager?.createNotificationChannel(channel)
    //}

    //-------------------------------------------------------------------
    //private fun sendNotification(content: String, senderName: String) {
    //    val replyLabel = "Enter your reply here"
    //    val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
    //        .setLabel(replyLabel)
    //        .build()
//
    //    val resultIntent = Intent(this, MainActivity::class.java)
//
    //    val resultPendingIntent = PendingIntent.getActivity(
    //        this,
    //        0,
    //        resultIntent,
    //        PendingIntent.FLAG_UPDATE_CURRENT
    //    )
//
    //    val icon = Icon.createWithResource(
    //        this@MainActivity,
    //        R.drawable.ic_baseline_send
    //    )
//
    //    val replyAction = Notification.Action.Builder(
    //        icon,
    //        "Reply", resultPendingIntent
    //    )
    //        .addRemoteInput(remoteInput)
    //        .build()
//
    //    val newMessageNotification = Notification.Builder(this, CHANNEL_ID)
    //        .setColor(
    //            ContextCompat.getColor(
    //                this,
    //                R.color.colorNotification
    //            )
    //        )
    //        .setSmallIcon(R.drawable.ic_walkie_talkie)
    //        .setContentTitle(senderName)
    //        .setContentText(content)
    //        .addAction(replyAction).build()
//
    //    val notificationManager = getSystemService(
    //        Context.NOTIFICATION_SERVICE
    //    ) as NotificationManager
//
    //    notificationManager.notify(
    //        notificationId,
    //        newMessageNotification
    //    )
    //}

    //-------------------------------------------------------------------
    //private fun handleNotificationIntent() {
//
    //    val intent = this.intent
//
    //    val remoteInput = RemoteInput.getResultsFromIntent(intent)
//
    //    if (remoteInput != null) {
//
    //        val inputString = remoteInput.getCharSequence(
    //            KEY_TEXT_REPLY
    //        ).toString()
//
//
    //        Toast.makeText(this, inputString, Toast.LENGTH_LONG).show()
//
    //        lateinit var repliedNotification: Notification
//
    //        runOnUiThread {
    //            // Test if hardware connected
    //            when (mModRfUartManager.getDevice()) {
//
    //                // If Hardware connected
    //                GET_UART_DEVICES_SUCCESS -> {
    //                    // TODO --> Send message to Hardware
    //                    // TODO --> Add msg sent to database
    //                    repliedNotification = Notification.Builder(this, CHANNEL_ID)
    //                        .setSmallIcon(
    //                            android.R.drawable.ic_dialog_info
    //                        )
    //                        .setContentText("Reply sent")
    //                        .build()
    //                }
//
    //                // else
    //                else -> {
    //                    // Display a error message
    //                    repliedNotification = Notification.Builder(this, CHANNEL_ID)
    //                        .setColor(
    //                            ContextCompat.getColor(
    //                                this,
    //                                R.color.red
    //                            )
    //                        )
    //                        .setSmallIcon(
    //                            android.R.drawable.stat_notify_error
    //                        )
    //                        .setContentText("Reply cannot be sent (Hardware not connected)")
    //                        .build()
    //                }
    //            }
    //        }
//
    //        notificationManager?.notify(notificationId, repliedNotification)
    //    }
    //}
}