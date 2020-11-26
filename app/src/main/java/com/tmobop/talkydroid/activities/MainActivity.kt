package com.tmobop.talkydroid.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tmobop.talkydroid.R
import com.tmobop.talkydroid.adapters.ConversationListAdapter
import com.tmobop.talkydroid.adapters.OnItemClickListener
import com.tmobop.talkydroid.classes.ConversationUI
import com.tmobop.talkydroid.classes.ModRfUartManager
import com.tmobop.talkydroid.classes.NO_DRIVER_AVAILABLE
import java.util.*

class MainActivity : AppCompatActivity(), OnItemClickListener, ModRfUartManager.Listener {

    private lateinit var conversationListAdapter: ConversationListAdapter
    private lateinit var conversationListRecyclerView: RecyclerView
    private lateinit var conversationListLayoutManager: LinearLayoutManager

    private lateinit var userUUID: String
    private lateinit var userName: String
    lateinit var mModRfUartManager: ModRfUartManager

    lateinit var btnConnect: Button
    lateinit var btnScan: Button
    lateinit var btnExit: Button
    lateinit var dialogViewConfigUsb: View

    var channelScanIsRunning = false

    var connectedDeviceInfoText = ""

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

        //-------------------------------ModRfUartManager -------------------------------
        mModRfUartManager = ModRfUartManager(this, this)

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
            R.id.action_settings -> return true
            R.id.action_modrf_config -> {

                val dialogBuilder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                dialogViewConfigUsb = inflater.inflate(R.layout.dialog_config_modrf, null)
                dialogViewConfigUsb.setBackgroundColor(Color.TRANSPARENT)
                dialogBuilder.setView(dialogViewConfigUsb)
                val alertDialog = dialogBuilder.create()
                alertDialog.show()

                btnConnect = dialogViewConfigUsb.findViewById<Button>(R.id.dialog_config_modrf_btn_connect)

                btnConnect.setOnClickListener {
                    val ret = mModRfUartManager.getDevice()
                    when(ret) {
                        NO_DRIVER_AVAILABLE -> Toast.makeText(this, "No device available", Toast.LENGTH_SHORT).show()
                    }
                }
                //TODO disable button when connected and change its text to "Connected"


                btnScan = dialogViewConfigUsb.findViewById<Button>(R.id.dialog_config_modrf_btn_scan)

                btnScan.setOnClickListener {
                    btnScan.isEnabled = false
                    channelScanIsRunning = true
                    mModRfUartManager.discoverNetworkDevicesAndGetAddress(true)
                }

                if(channelScanIsRunning){
                    btnScan.isEnabled = false
                }


                btnExit = dialogViewConfigUsb.findViewById<Button>(R.id.dialog_config_modrf_btn_exit)

                btnExit.setOnClickListener {
                    alertDialog.dismiss()
                }

                dialogViewConfigUsb.findViewById<TextView>(R.id.dialog_config_modrf_text_device_info).text = connectedDeviceInfoText

                return true
            }
            else -> return super.onOptionsItemSelected(item)
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

        var START_MODRF_CONFIG_REQUEST = 1
    }

    override fun onTextReceived(string: String, senderUUID: String) {
        runOnUiThread {
            Toast.makeText(this, "Received data from " + senderUUID + " " + string, Toast.LENGTH_SHORT).show()
            //TODO
        }
    }

    override fun onError(customText: String, e: Exception) {
        runOnUiThread {
            Toast.makeText(this, customText + e.toString(), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int) {
        runOnUiThread{
            dialogViewConfigUsb.findViewById<ProgressBar>(R.id.dialog_config_modrf_progressbar_scan).progress = currentAddress
        }
    }

    override fun onDiscoverFinished(devicesFoundInChannel: ArrayList<ModRfUartManager.DeviceInChannel>) {
        runOnUiThread {
            channelScanIsRunning = false
            btnScan.isEnabled = true
            Toast.makeText(this, "Discovered " + devicesFoundInChannel.size + " device(s) in channel", Toast.LENGTH_SHORT).show()

            conversationListAdapter.clearConversations()

            for(device in devicesFoundInChannel){
                val conversation = ConversationUI(
                    title = device.userName,
                    description = "userUUID " + device.userUUID,
                    time = Calendar.getInstance().timeInMillis,
                    avatarID = 0
                )
                conversationListAdapter.addConversation(conversation)
            }

        }
    }

    override fun onDeviceJoinedNetwork(device: ModRfUartManager.DeviceInChannel) {
        runOnUiThread {
            Toast.makeText(this, "New device joined network", Toast.LENGTH_SHORT).show()

            val conversation = ConversationUI(
                title = device.userName,
                description = "userUUID " + device.userUUID,
                time = Calendar.getInstance().timeInMillis,
                avatarID = 0
            )
            conversationListAdapter.addConversation(conversation)
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
            conversationListAdapter.clearConversations()
        }
    }

    override fun onDeviceOpened() {
        runOnUiThread{
            Toast.makeText(this, "Device opened", Toast.LENGTH_SHORT).show()
            val deviceProperties = mModRfUartManager.getDeviceProporties()!!
            connectedDeviceInfoText =
                "Name : " + deviceProperties.deviceName + "\n" +
                "Manufacturer : " + deviceProperties.manufacturerName + "\n" +
                "Product name : " + deviceProperties.productName + "\n" +
                "id : " + deviceProperties.id
            dialogViewConfigUsb.findViewById<TextView>(R.id.dialog_config_modrf_text_device_info).text = connectedDeviceInfoText
        }
    }

    override fun onDeviceOpenError() {
        runOnUiThread {
            Toast.makeText(this, "Cannot open Device : permission denied", Toast.LENGTH_SHORT).show()
        }
    }

}