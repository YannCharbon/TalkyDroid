package com.tmobop.talkydroid.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.adapters.ConversationListAdapter
import com.tmobop.talkydroid.classes.GET_UART_DEVICES_SUCCESS
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.classes.NO_DRIVER_AVAILABLE
import com.tmobop.talkydroid.services.NotificationService
import com.tmobop.talkydroid.services.SingletonServiceManager
import com.tmobop.talkydroid.storage.*
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.util.*

class MainActivity : AppCompatActivity(), ModRfUartManager.Listener {

    // Conversation adapter variables
    private lateinit var conversationListAdapter: ConversationListAdapter
    private lateinit var conversationListRecyclerView: RecyclerView

    // Shared preferences
    private lateinit var sharedPreferences: SharedPreferences
    private val userINFO : String = "user_information"
    private val userNameKey = "userNameKey"
    private val userAvatarPathKey = "userAvatarPathKey"

    // Hardware variables
    lateinit var mModRfUartManager: ModRfUartManager
    private lateinit var dialogViewConfigUsb: View
    private lateinit var timerScanningError: CountDownTimer
    private var channelScanIsRunning = false
    private lateinit var alertDialog: AlertDialog
    private var isDeviceConnected: Boolean = false
    private var isDeviceOpen: Boolean = false

    // Buttons variables
    private lateinit var btnSettings: ImageButton
    private lateinit var btnScanChannels: ImageButton

    // ViewModel variables
    private lateinit var userWithMessagesViewModel: UserWithMessagesViewModel
    private lateinit var messagesViewModel: MessageViewModel

    // Locks
    private var usbPermissionRequestPending = false

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
        // Get hardware
        mModRfUartManager = ModRfUartManager(this, this)

        //------------------------------ Get user profil ------------------------------------
        // Get shared preferences
        sharedPreferences = getSharedPreferences(userINFO, Context.MODE_PRIVATE)

        // Get values
        val userName = sharedPreferences.getString(userNameKey, "unknown")
        val userAvatar = sharedPreferences.getString(userAvatarPathKey, "")
        val userUUID = mModRfUartManager.companion.userUUID

        // Set username to hardware
        if (userName != null) {
            mModRfUartManager.companion.userName = userName
        } else {
            mModRfUartManager.companion.userName = "unknown"
        }

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
            userWithMessagesViewModel.setAllUsersOffline()
        }

        // Display users from database
        userWithMessagesViewModel.getAllUsersWithMessages().observe(this, { usersWithMessages ->

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
            val applicationUser = UserEntity(UUID.fromString(userUUID),
                userName.toString(), userAvatar.toString(), online = true)
            userWithMessagesViewModel.insertUser(applicationUser)      // If the user already exists, this line is ignored
        }

        // Stop the notification service if it is running
        if (SingletonServiceManager.isMyServiceRunning) {
            stopService(Intent(this, NotificationService::class.java))
        }
    }

    //-------------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //-------------------------------------------------------------------
    override fun onPause() {
        super.onPause()

        // Start the notification service
        if (!SingletonServiceManager.isMyServiceRunning) {
            val serviceIntent = Intent(this, NotificationService::class.java)
            startService(serviceIntent)
        }
    }

    //-------------------------------------------------------------------
    override fun onResume() {
        super.onResume()

        // Stop the service if it is running
        if (SingletonServiceManager.isMyServiceRunning) {

            stopService(Intent(this, NotificationService::class.java))
        }

        // Get the hardware manager
        mModRfUartManager.changeContext(this, this)


        Toast.makeText(this, "ioiouhku", Toast.LENGTH_SHORT).show()

        // Check if USB connected
        if(!usbPermissionRequestPending){
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
        }
    }

    //-------------------------------------------------------------------
    companion object {
        var START_CONVERSATION_REQUEST = 0
        const val TITLE_TEXT = "title"
        const val AVATAR_ID = "avatar_id"
        const val USER_UUID = "user_uuid"
        const val USER_NAME = "user_name"
        const val RECEIVER_UUID = "receiver_uuid"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    //-------------------------------------------------------------------
    override fun onTextReceived(string: String, senderUUID: String) {
        runOnUiThread {
            if (senderUUID == "") {
                Toast.makeText(this, "Message received from unknown device. Please rescan channel", Toast.LENGTH_SHORT).show()
            }
            else {
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

                // Display a toast to inform the user that there is new messages
                Toast.makeText(
                    this,
                    "New messages received",
                    Toast.LENGTH_SHORT
                ).show()

                // Get the userName
                //userWithMessagesViewModel.getAllUsersWithMessages().observe(this,
                //    { usersWithMessage ->
                //        // Get the sender entity
                //        val senderEntity = usersWithMessage.findLast { userWithMessages ->
                //            userWithMessages.userEntity!!.userId == UUID.fromString(senderUUID)
                //        }
//
                //        // Check if sender entity is in database
                //        if (senderEntity != null) {
                //            // Get the sender name
                //            val senderName = senderEntity.userEntity!!.userName
//
                //            Toast.makeText(
                //                this,
                //                "Received message from $senderName : $string",
                //                Toast.LENGTH_SHORT
                //            ).show()
                //        }
                //    }
                //)
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onLocationReceived(location: String, senderUUID: String) {
        runOnUiThread {
            if (senderUUID == "") {
                Toast.makeText(this, "Message received from unknown device. please rescan channel", Toast.LENGTH_SHORT).show()
            }
            else {
                val message = MessageEntity(
                    messageId = null,
                    senderId = UUID.fromString(senderUUID),
                    receiverId = UUID.fromString(mModRfUartManager.companion.userUUID),
                    content = location,
                    time = Calendar.getInstance().timeInMillis,
                    messageType = MessageType.LOCATION
                )

                // Insert message to database
                lifecycleScope.launch {
                    // Insert message to database
                    messagesViewModel.insertMessage(message)
                }

                // Display a toast to inform the user that there is new messages
                Toast.makeText(
                    this,
                    "New messages received",
                    Toast.LENGTH_SHORT
                ).show()

                // Get the userName
                //userWithMessagesViewModel.getAllUsersWithMessages().observe(this,
                //    { usersWithMessage ->
                //        // Get the sender entity
                //        val senderEntity = usersWithMessage.findLast { userWithMessages ->
                //            userWithMessages.userEntity!!.userId == UUID.fromString(senderUUID)
                //        }
//
                //        // Check if sender entity is in database
                //        if (senderEntity != null) {
                //            // Get the sender name
                //            val senderName = senderEntity.userEntity!!.userName
//
                //            // Get coords
                //            val cords = location.split(',')
                //            val latitude = cords[0]
                //            val longitude = cords[1]
//
                //            Toast.makeText(
                //                this,
                //                "Received location from $senderName : \n" +
                //                        "latitude = $latitude\n" +
                //                        "longitude = $longitude",
                //                Toast.LENGTH_SHORT
                //            ).show()
                //        }
                //    }
                //)
            }
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
                userWithMessagesViewModel.insertUser(newConversation)
                userWithMessagesViewModel.setUserOnline(UUID.fromString(device.userUUID))
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
            userWithMessagesViewModel.insertUser(newConversation)
            userWithMessagesViewModel.setUserOnline(UUID.fromString(device.userUUID))
        }
    }

    //-------------------------------------------------------------------
    override fun onDeviceAttached() {

        // Toggle the device attached logo to green
        btnScanChannels.setImageResource(R.drawable.ic_baseline_settings_remote_green)

        // Display a toast to inform the user that the device is attached
        runOnUiThread {
            Toast.makeText(this, "Device attached", Toast.LENGTH_SHORT).show()
            if(!isDeviceConnected){
                // Toggle device connected variable
                isDeviceConnected = true

                usbPermissionRequestPending = true
                when (mModRfUartManager.getDevice()) {
                    NO_DRIVER_AVAILABLE -> Toast.makeText(this, "The driver is not available", Toast.LENGTH_SHORT).show()
                }
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
            lifecycleScope.launch {
                userWithMessagesViewModel.setAllUsersOffline()
            }

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

                // Wait 2s before launching the scan to be sure everything is ok (anti-rebound)
                sleep(2000)

                usbPermissionRequestPending = false

                // Scan channels
                scanChannels()
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onDeviceOpenError() {
        // Toggle device connected variable
        isDeviceConnected = false
        isDeviceOpen = false

        runOnUiThread {
            Toast.makeText(this, "Cannot open Device : permission denied", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
        }
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
}