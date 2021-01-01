package com.tmobop.talkydroid.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.activities.MainActivity
import com.tmobop.talkydroid.classes.GET_UART_DEVICES_SUCCESS
import com.tmobop.talkydroid.classes.MessageType
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.storage.*
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import java.util.*
import kotlin.collections.ArrayList

class NotificationService : LifecycleService(), ModRfUartManager.Listener {

    // Notification variables
    private var notificationManager: NotificationManager? = null
    private val notificationId = 101

    // Hardware variables
    private lateinit var mModRfUartManager: ModRfUartManager
    private lateinit var receiverUUID: String
    private var isNotificationSend: Boolean = false

    // Database
    private var job = SupervisorJob()
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var messagesDao: MessageDao
    private lateinit var messagesRepository: MessageRepository
    private lateinit var userWithMessagesDao: UserWithMessagesDao
    private lateinit var userWithMessagesRepository: UserWithMessagesRepository

    //------------------------------------------------------------------------
    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Get the hardware
            mModRfUartManager = ModRfUartManager(this, this)
            mModRfUartManager.unregisterUsbReceiver()

            // Get the notification service
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create the notification channel
            createNotificationChannel()
        }
    }

    //------------------------------------------------------------------------
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Set a variable to detect that the service is running
        SingletonServiceManager.isMyServiceRunning = true

        // Get the database
        coroutineScope = CoroutineScope(Dispatchers.Main + job)
        userWithMessagesDao = TalkyDroidDatabase.getDatabase(this, coroutineScope, application.resources).userWithMessagesDao()
        messagesDao = TalkyDroidDatabase.getDatabase(this, coroutineScope, application.resources).messageDao()
        userWithMessagesRepository = UserWithMessagesRepository(userWithMessagesDao)
        messagesRepository = MessageRepository(messagesDao)

        // Handle the notification intent
        handleNotificationIntent(intent)

        return START_NOT_STICKY
    }

    //------------------------------------------------------------------------
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    //------------------------------------------------------------------------
    override fun onDestroy() {
        super.onDestroy()
        notificationManager!!.cancel(notificationId);
        SingletonServiceManager.isMyServiceRunning = false
    }

    //------------------------------------------------------------------------
    private fun createNotificationChannel() {

        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TalkyDroid_channel",
            importance
        )

        channel.description = "TalkyDroid_description"
        channel.enableLights(true)
        channel.lightColor = Color.BLUE
        channel.enableVibration(true)
        channel.vibrationPattern =
            longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

        notificationManager?.createNotificationChannel(channel)
    }

    //------------------------------------------------------------------------
    private fun sendNotification(content: String, senderName: String) {

        val replyLabel = "Enter your reply here"

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val resultIntent = Intent(this, NotificationService::class.java)

        val resultPendingIntent = PendingIntent.getService(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val icon = Icon.createWithResource(
            this,
            R.drawable.ic_baseline_send
        )

        val replyAction = Notification.Action.Builder(
            icon,
            "Reply", resultPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        val newMessageNotification = Notification.Builder(this, CHANNEL_ID)
            .setColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorNotification
                )
            )
            .setSmallIcon(R.drawable.ic_walkie_talkie)
            .setContentTitle(senderName)
            .setContentText(content)
            .addAction(replyAction).build()

        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationManager.notify(
            notificationId,
            newMessageNotification
        )
    }

    //------------------------------------------------------------------------
    private fun handleNotificationIntent(intent: Intent?) {

        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        if (remoteInput != null) {

            val inputString = remoteInput.getCharSequence(
                MainActivity.KEY_TEXT_REPLY
            ).toString()

            lateinit var repliedNotification: Notification

            // Test if hardware connected
            when (mModRfUartManager.getDevice()) {

                // If Hardware connected
                GET_UART_DEVICES_SUCCESS -> {
                    // Create the message to send
                    val message = MessageEntity(
                        messageId = null,
                        senderId = UUID.fromString(mModRfUartManager.companion.userUUID),
                        receiverId = UUID.fromString(receiverUUID),
                        content = inputString,
                        time = Calendar.getInstance().timeInMillis,
                        messageType = MessageType.TEXT
                    )

                    // Send message to Hardware
                    mModRfUartManager.writeText(inputString, receiverUUID)

                    // Push to database
                    lifecycleScope.launch {
                        messagesRepository.insertMessage(message)
                    }
                    repliedNotification = Notification.Builder(this, CHANNEL_ID)
                        .setSmallIcon(
                            android.R.drawable.ic_dialog_info
                        )
                        .setContentText("Reply sent")
                        .build()
                }

                // else
                else -> {
                    // Display a error message
                    repliedNotification = Notification.Builder(this, CHANNEL_ID)
                        .setColor(
                            ContextCompat.getColor(
                                this,
                                R.color.red
                            )
                        )
                        .setSmallIcon(
                            android.R.drawable.stat_notify_error
                        )
                        .setContentText("Reply cannot be sent (Hardware not connected)")
                        .build()
                }
            }

            notificationManager?.notify(notificationId, repliedNotification)
            sleep(1500)
            notificationManager?.cancel(notificationId)
        }
    }

    //------------------------------------------------------------------------
    companion object {
        const val CHANNEL_ID = "channel_id"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    //-------------------------------------------------------------------
    override fun onTextReceived(string: String, senderUUID: String) {
        receiverUUID = senderUUID
        isNotificationSend = false

        if (senderUUID != "") {
            GlobalScope.launch(Dispatchers.Main) {
                userWithMessagesRepository.getUsersWithMessages().observe(this@NotificationService,
                    { usersWithMessage ->

                        // Get the sender entity
                        val senderEntity = usersWithMessage.findLast { userWithMessages ->
                            userWithMessages.userEntity!!.userId == UUID.fromString(senderUUID)
                        }

                        // Check if sender entity is in database
                        if (senderEntity != null) {
                            // Get the sender name
                            val senderName = senderEntity.userEntity!!.userName

                            if (!isNotificationSend) {
                                isNotificationSend = true
                                // Send the notification
                                sendNotification(string, senderName)
                            }
                        }
                    }
                )

                lifecycleScope.launch {
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
                        messagesRepository.insertMessage(message)
                    }
                }
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onLocationReceived(location: String, senderUUID: String) {
        receiverUUID = senderUUID
        isNotificationSend = false

        if (senderUUID != "") {
            GlobalScope.launch(Dispatchers.Main) {
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
                    messagesRepository.insertMessage(message)
                }

                // Get the userName
                userWithMessagesRepository.getUsersWithMessages().observe(this@NotificationService,
                    { usersWithMessage ->
                        // Get the sender entity
                        val senderEntity = usersWithMessage.findLast { userWithMessages ->
                            userWithMessages.userEntity!!.userId == UUID.fromString(senderUUID)
                        }

                        // Check if sender entity is in database
                        if (senderEntity != null) {
                            // Get the sender name
                            val senderName = senderEntity.userEntity!!.userName

                            if (!isNotificationSend) {
                                isNotificationSend = true
                                // Get coords
                                val cords = location.split(',')
                                val latitude = cords[0]
                                val longitude = cords[1]

                                // Send the notification
                                sendNotification(
                                    "Latitude = $latitude, Longitude = $longitude",
                                    senderName
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    //-------------------------------------------------------------------
    override fun onError(customText: String, e: Exception) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDiscoverFinished(devicesFoundInChannel: ArrayList<ModRfUartManager.DeviceInChannel>) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceJoinedNetwork(device: ModRfUartManager.DeviceInChannel) {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceAttached() {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceDetached() {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceOpened() {
        return
    }

    //-------------------------------------------------------------------
    override fun onDeviceOpenError() {
        return
    }
}