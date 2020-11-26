package com.tmobop.talkydroid.classes

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


const val DEFAULT_WRITE_TIMEOUT = 1000

const val DEFAULT_PACKET_SIZE = 62
const val DEFAULT_PACKET_SIZE_USABLE = 61

const val FRAME_HEADER_SIZE = 8
const val TEXT_HEADER_SIZE = 1

const val TEXT_CONTENT_USABLE_SIZE = DEFAULT_PACKET_SIZE-FRAME_HEADER_SIZE-TEXT_HEADER_SIZE

const val NO_DRIVER_AVAILABLE = 0
const val DRIVER_NO_PERMISSION = 1
const val CONNECTION_NULL = 2
const val GET_UART_DEVICES_SUCCESS = 3

const val SUCCESS = 0
const val ERROR = 1

const val TYPE_TEXT : UByte = 0u
const val TYPE_LOC : UByte = 1u
const val TYPE_IMG : UByte = 2u
const val TYPE_DISCOVER : UByte = 3u
const val TYPE_ACK : UByte = 4u

const val DISCOVER_REQUEST_TYPE_REQUEST : UByte = 0u
const val DISCOVER_REQUEST_TYPE_ANSWER : UByte = 1u
const val DISCOVER_REQUEST_TYPE_INFO : UByte = 2u

const val MAX_DEV_ADDRESS = 254
const val MIN_DEV_ADDRESS = 1
const val TOT_DEV_ADDRESSES = MAX_DEV_ADDRESS - MIN_DEV_ADDRESS
const val BROADCAST_ADDRESS: UByte = 255u
const val NOT_ASSIGNED_ADDRESS: UByte = 0u

const val SHARED_PREFS ="ModRfUartManagerPrefs"

const val MAX_USERNAME_LENGTH = 38

// Intent constants
const val INTENT_GET_USB_PERMISSION = "INTENT_GET_USB_PERMISSION"
const val ACTION_USB_DEV_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
const val ACTION_USB_DEV_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"

class ModRfUartManager(context: Context, listener: Listener) {

    private var usbManager: UsbManager? = null
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialDevice? = null

    private var localAddress: UByte = 0u  //TODO change to 0u
    public var channel: UByte = 0u
    public var userName: String = "Default name"
    public var userUUID: String = ""

    data class DeviceInChannel(
        val userUUID: String,
        val address: UByte,
        val userName: String
    )

    public var devicesInChannel = ArrayList<DeviceInChannel>()

    data class UsbDeviceProperties(
        val deviceName : String,
        val manufacturerName : String?,
        val productName: String?,
        val id: Int
    )

    enum class PermissionState {
        NONE, PENDING, TREATED
    }
    private var permissionState = PermissionState.NONE

    private var portIsOpen = false

    enum class TimeoutState{
        NONE, PENDING, OCCURED, CANCELLED
    }
    private var timeoutState: TimeoutState = TimeoutState.NONE

    data class TalkyDataPacketFrame(
        var channel: UByte,
        var senderAddress: UByte,
        var receiverAddress: UByte,
        var packetsNumber: UShort,
        var packetId: UShort,
        var dataType: UByte,
        var content: ByteArray
    )

    data class TalkyDataPacketTextContent(
        var packetTextLen: UByte,
        var text: ByteArray
    )

    data class TalkyDataPacketDiscoverContent(
        var requestType: UByte,
        var userUUID: ByteArray,
        var userNameLength: UByte,
        var userName: ByteArray
    )



    private var mListener: ModRfUartManager.Listener? = null

    public interface Listener{
        public fun onTextReceived(string: String, senderUUID: String)
        public fun onError(customText: String, e: java.lang.Exception)
        public fun onDiscoverProgress(currentAddress: Int, totalAddresses: Int)
        public fun onDiscoverFinished(devicesFoundInChannel: ArrayList<DeviceInChannel>)
        public fun onDeviceJoinedNetwork(device: DeviceInChannel)
        public fun onDeviceAttached()
        public fun onDeviceDetached()
        public fun onDeviceOpened()
        public fun onDeviceOpenError()
    }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == INTENT_GET_USB_PERMISSION) {
                val granted: Boolean =
                    intent.getExtras()!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    connection = usbManager!!.openDevice(device)
                    openPort()
                    getListener()?.onDeviceOpened()
                } else  // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    getListener()?.onDeviceOpenError()
                }
            } else if (intent.action == ACTION_USB_DEV_ATTACHED) {
                //TODO modify
                getListener()?.onDeviceAttached()
            } else if (intent.action == ACTION_USB_DEV_DETACHED) {
                //TODO modify
                if (portIsOpen) {
                    serialPort!!.close()
                }
                portIsOpen = false
                getListener()?.onDeviceDetached()
            }
        }
    }

    init {
        activityContext = context
        mListener = listener

        val mySharedPref = activityContext.getSharedPreferences(SHARED_PREFS, Activity.MODE_PRIVATE)
        userUUID = mySharedPref.getString("userUUID", "")!!
        if(userUUID == ""){
            userUUID = UUID.randomUUID().toString()
            val editor = mySharedPref.edit()
            editor.putString("userUUID", userUUID)
            editor.apply()
        }

        val filter = IntentFilter()
        filter.addAction(INTENT_GET_USB_PERMISSION)
        filter.addAction(ACTION_USB_DEV_ATTACHED)
        filter.addAction(ACTION_USB_DEV_DETACHED)
        activityContext.registerReceiver(usbReceiver, filter)
    }

    @Synchronized
    fun getListener(): ModRfUartManager.Listener? {
        return mListener
    }

    companion object{
        lateinit var activityContext : Context

    }

    public fun close(){
        if(serialPort != null){
            serialPort!!.close()
        }
        portIsOpen = false
        //readThread.interrupt()
        //readThread.join()
    }

    public fun getDevice() : Int{

        usbManager = activityContext.getSystemService(Context.USB_SERVICE) as UsbManager
        //val usbDevices = usbManager!!.deviceList

        val usbDevices : MutableMap<String, UsbDevice> = usbManager!!.deviceList

        if(usbDevices.isEmpty()) {
            return NO_DRIVER_AVAILABLE
        }

        device = usbDevices.values.first()

        if(!usbManager!!.hasPermission(device)) {
            val usbPermissionIntent =
                PendingIntent.getBroadcast(activityContext, 0, Intent(INTENT_GET_USB_PERMISSION), 0)
            usbManager!!.requestPermission(device, usbPermissionIntent)
        }

        return GET_UART_DEVICES_SUCCESS
    }

    public fun getDeviceProporties() : UsbDeviceProperties? {
        if(device != null){
            return UsbDeviceProperties(
                device!!.deviceName,
                device!!.manufacturerName,
                device!!.productName,
                device!!.deviceId
            )
        }
        return null
    }


    private fun openPort() : Int{
        if(connection != null){
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)

            if(serialPort != null){
                serialPort!!.open()
                serialPort!!.setBaudRate(57600)
                serialPort!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                serialPort!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                serialPort!!.setParity(UsbSerialInterface.PARITY_NONE)
                serialPort!!.read(mCallback)

                portIsOpen = true
            } else {
                portIsOpen = false

                return ERROR
            }

            return SUCCESS
        } else {
            return ERROR
        }
    }

    private val mCallback =
        UsbReadCallback { arg0 ->
            val listener = getListener()
            assembleReadData(arg0)
        }

    var packetList = ArrayList<ByteArray>()
    var currentPacket = ByteArray(DEFAULT_PACKET_SIZE)
    var currentPacketIndex = 0
    private fun assembleReadData(data: ByteArray){
        try {
            var size = data.size

            // Save current packet if income is to big
            while(size >= DEFAULT_PACKET_SIZE - currentPacketIndex){
                for(i in 0..(DEFAULT_PACKET_SIZE - currentPacketIndex - 1)){
                    currentPacket[i + currentPacketIndex] = data[i]
                }
                currentPacketIndex = 0
                size -= (DEFAULT_PACKET_SIZE - currentPacketIndex)
                packetList.add(currentPacket.copyOf())
                currentPacket.fill(0)
            }

            // Treat residue
            if(size > 0){
                for(i in 0..(size - 1)){
                    currentPacket[i + currentPacketIndex] = data[i]
                }
                currentPacketIndex = size
            }

            // Treat each assembled packet
            for(packet in packetList){
                val listener = getListener()
                handleReadData(packet)
                packetList.remove(packet)
            }
        } catch (e: java.lang.Exception){
            val listener = this.getListener()
            listener!!.onError("Could not assemble incoming data packets", e)
        }
    }



    var readPacketList = ArrayList<ModRfUartManager.TalkyDataPacketFrame>()
    private fun handleReadData(readData: ByteArray){

        try {
            val dataPacketFrame = byteArrayToPacketFrame(readData)

            // Implement address and channel
            if((dataPacketFrame.receiverAddress == localAddress || dataPacketFrame.receiverAddress == BROADCAST_ADDRESS)
                && dataPacketFrame.channel == channel){
                if(dataPacketFrame.packetId.toInt() == 0){
                    readPacketList.clear()
                    readPacketList.add(dataPacketFrame)
                } else if (dataPacketFrame.packetId.toInt() < dataPacketFrame.packetsNumber.toInt()){
                    readPacketList.add(dataPacketFrame)
                }

                val listener = getListener()

                // All packets gathered
                if(dataPacketFrame.packetId.toInt() == dataPacketFrame.packetsNumber.toInt() - 1){
                    when(readPacketList[0].dataType){
                        TYPE_TEXT -> {
                            var resultText: String = ""
                            for (packet in readPacketList){
                                val dataPacketContentTextContent = byteArrayToTextContent(packet.content)
                                resultText += String(dataPacketContentTextContent.text)
                            }

                            var senderUUID: String = addressToUuid(readPacketList[0].senderAddress)

                            if(listener != null){
                                listener.onTextReceived(resultText, senderUUID)
                            }
                        }
                        TYPE_DISCOVER -> {

                            // there should be only one packet in list
                            val dataPacketDiscoverContent = byteArrayToDiscoverContent(readPacketList[0].content)

                            if(dataPacketDiscoverContent.requestType == DISCOVER_REQUEST_TYPE_REQUEST){
                                // if discover request -> Build answer
                                val responsePacketContent = TalkyDataPacketDiscoverContent(
                                    requestType =  DISCOVER_REQUEST_TYPE_ANSWER,
                                    userUUID = getBytesFromUUID(UUID.fromString(userUUID))!!,
                                    userNameLength = userName.length.toUByte(),
                                    userName = userName.toByteArray()
                                )
                                val responsePacket = TalkyDataPacketFrame(
                                    channel = channel,
                                    senderAddress = localAddress,
                                    receiverAddress = dataPacketFrame.senderAddress,
                                    packetsNumber = 1u,
                                    packetId = 0u,
                                    dataType = TYPE_DISCOVER,
                                    content =  discoverContentToByteArray(responsePacketContent)
                                )

                                val packetByteArray = packetFrameToByteArray(responsePacket)
                                serialPort!!.write(packetByteArray)
                            } else if (dataPacketDiscoverContent.requestType == DISCOVER_REQUEST_TYPE_ANSWER){
                                // if discover answer -> store new device
                                timeoutState = TimeoutState.CANCELLED
                                Timer("TimerTimeout").cancel()
                                var newDevice = DeviceInChannel(
                                    userUUID = getUUIDFromBytes(dataPacketDiscoverContent.userUUID).toString(),
                                    address = dataPacketFrame.senderAddress,
                                    userName = String(dataPacketDiscoverContent.userName)
                                )
                                devicesInChannel.add(newDevice)
                            } else if (dataPacketDiscoverContent.requestType == DISCOVER_REQUEST_TYPE_INFO){
                                // if discover info -> store new device
                                var newDevice = DeviceInChannel(
                                    userUUID = getUUIDFromBytes(dataPacketDiscoverContent.userUUID).toString(),
                                    address = dataPacketFrame.senderAddress,
                                    userName = String(dataPacketDiscoverContent.userName)
                                )
                                devicesInChannel.add(newDevice)

                                if(listener != null){
                                    listener.onDeviceJoinedNetwork(newDevice)
                                }
                            }
                        }

                    }
                }
            }
        } catch (e: java.lang.Exception){
            val listener = this.getListener()
            listener!!.onError("Could not handle/decode incoming packets", e)
        }

    }

    public fun writeText(text: String, destUserUUID: String) : Int{
        if(portIsOpen){

            var destAddress: UByte = uuidToAddress(destUserUUID)

            val listener = getListener()

            val packetsNumber = text.length / TEXT_CONTENT_USABLE_SIZE + 1
            var packets = ArrayList<ModRfUartManager.TalkyDataPacketFrame>()
            for(i in 0 until packetsNumber){
                try {
                    var packetTextLen: UByte = 0u
                    var contentText: ByteArray? = null
                    if(i < packetsNumber - 1){
                        packetTextLen = (TEXT_CONTENT_USABLE_SIZE).toUByte()
                        contentText = text.substring(i*TEXT_CONTENT_USABLE_SIZE until (i+1)*TEXT_CONTENT_USABLE_SIZE).toByteArray()
                    } else {
                        packetTextLen = (text.length % TEXT_CONTENT_USABLE_SIZE).toUByte()
                        contentText = text.substring(i*TEXT_CONTENT_USABLE_SIZE).toByteArray()
                    }

                    var packetContent = ModRfUartManager.TalkyDataPacketTextContent(
                        packetTextLen,
                        contentText!!
                    )

                    packets.add(
                        ModRfUartManager.TalkyDataPacketFrame(
                            channel,
                            localAddress,
                            destAddress,
                            packetsNumber.toUShort(),
                            i.toUShort(),
                            TYPE_TEXT,
                            textContentToByteArray(packetContent)
                        )
                    )
                } catch (e: java.lang.Exception){
                    listener!!.onError("Could not split text data into packets", e)
                }
            }

            try{
                for (packet in packets){
                    val packetByteArray = packetFrameToByteArray(packet)
                    serialPort!!.write(packetByteArray)
                    Thread.sleep(100)
                }

                return SUCCESS
            } catch(e: Exception){
                listener!!.onError("Could not write packet", e)
            }

            return ERROR

        } else {
            return ERROR
        }
    }

    public fun discoverNetworkDevicesAndGetAddress(setAddressAfterDiscover: Boolean) {
        devicesInChannel.clear()

        val listener = getListener()!!

        if(portIsOpen){
            GlobalScope.launch {
                for(i in 1..254){

                    listener.onDiscoverProgress(i,254)

                    val packetContent = TalkyDataPacketDiscoverContent(
                        DISCOVER_REQUEST_TYPE_REQUEST,
                        getBytesFromUUID(UUID.fromString(userUUID))!!,
                        userName.length.toUByte(),
                        userName.toByteArray()
                    )
                    val packet = TalkyDataPacketFrame(
                        channel,
                        localAddress,
                        i.toUByte(),
                        1u,
                        0u,
                        TYPE_DISCOVER,
                        discoverContentToByteArray(packetContent)
                    )



                    val packetByteArray = packetFrameToByteArray(packet)
                    serialPort!!.write(packetByteArray)
                    Timer("TimerTimeout", false).schedule(100) {
                        if(timeoutState == TimeoutState.PENDING){
                            timeoutState = TimeoutState.OCCURED
                            this.cancel()
                        }
                    }
                    timeoutState = TimeoutState.PENDING
                    while(timeoutState == TimeoutState.PENDING){}
                    timeoutState = TimeoutState.NONE
                }

                if(setAddressAfterDiscover == true){

                    localAddress = getFirstAvailableAddressFromDeviceList()

                    val packetContent = TalkyDataPacketDiscoverContent(
                        DISCOVER_REQUEST_TYPE_INFO,
                        getBytesFromUUID(UUID.fromString(userUUID))!!,
                        userName.length.toUByte(),
                        userName.toByteArray()
                    )
                    val packet = TalkyDataPacketFrame(
                        channel,
                        localAddress,
                        BROADCAST_ADDRESS,
                        1u,
                        0u,
                        TYPE_DISCOVER,
                        discoverContentToByteArray(packetContent)
                    )

                    val packetByteArray = packetFrameToByteArray(packet)

                    serialPort!!.write(packetByteArray)

                }

                listener.onDiscoverFinished(devicesInChannel)

            }
        }
    }

    private fun getFirstAvailableAddressFromDeviceList() : UByte{
        var addressesInChannel = ArrayList<UByte>(devicesInChannel.size)

        for(device in devicesInChannel){
            addressesInChannel.add(device.address)
        }

        var tempAddress: UByte = 1u

        while(addressesInChannel.contains(tempAddress)){
            tempAddress++
        }

        return tempAddress
    }

    private fun uuidToAddress(uuid: String) : UByte{
        var i = 0
        if(devicesInChannel.isNotEmpty()){
            while(devicesInChannel[i].userUUID != uuid){ i++ }
            return devicesInChannel[i].address
        } else {
            return 0u
        }
    }

    private fun addressToUuid(address: UByte) : String{
        var i = 0
        if(devicesInChannel.isNotEmpty()){
            while(devicesInChannel[i].address != address){ i++ }
            return devicesInChannel[i].userUUID
        } else {
            return ""
        }
    }



    private fun textContentToByteArray(textContent: ModRfUartManager.TalkyDataPacketTextContent) : ByteArray{
        val byteBuffer = ByteBuffer.allocate(DEFAULT_PACKET_SIZE - FRAME_HEADER_SIZE)
        byteBuffer.put(textContent.packetTextLen.toByte())
        byteBuffer.put(textContent.text)
        return byteBuffer.array()
    }

    private fun discoverContentToByteArray(discoverContent: TalkyDataPacketDiscoverContent) : ByteArray{
        val byteBuffer = ByteBuffer.allocate(DEFAULT_PACKET_SIZE - FRAME_HEADER_SIZE)
        byteBuffer.put(discoverContent.requestType.toByte())
        byteBuffer.put(discoverContent.userUUID)
        byteBuffer.put(discoverContent.userNameLength.toByte())
        byteBuffer.put(discoverContent.userName)
        return byteBuffer.array()
    }

    private fun packetFrameToByteArray(packetFrame: ModRfUartManager.TalkyDataPacketFrame) : ByteArray{
        val byteBuffer = ByteBuffer.allocate(DEFAULT_PACKET_SIZE)
        byteBuffer.put(packetFrame.channel.toByte())
        byteBuffer.put(packetFrame.senderAddress.toByte())
        byteBuffer.put(packetFrame.receiverAddress.toByte())
        byteBuffer.putShort(packetFrame.packetsNumber.toShort())
        byteBuffer.putShort(packetFrame.packetId.toShort())
        byteBuffer.put(packetFrame.dataType.toByte())
        byteBuffer.put(packetFrame.content)
        return byteBuffer.array()
    }

    private fun byteArrayToTextContent(byteArray: ByteArray) : ModRfUartManager.TalkyDataPacketTextContent {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        val textLen = byteBuffer.get().toUByte()
        val text = ByteArray(textLen.toInt())
        byteBuffer.get(text)

        val packetTextContent = ModRfUartManager.TalkyDataPacketTextContent(
            textLen,
            text
        )

        return packetTextContent
    }

    private fun byteArrayToDiscoverContent(byteArray: ByteArray) : TalkyDataPacketDiscoverContent {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        val requestType = byteBuffer.get().toUByte()
        val userUUID = ByteArray(16)
        byteBuffer.get(userUUID)
        val userNameLength = byteBuffer.get().toUByte()
        val userName = ByteArray(userNameLength.toInt())
        byteBuffer.get(userName)

        val packetDiscoverContent = TalkyDataPacketDiscoverContent(
            requestType,
            userUUID,
            userNameLength,
            userName
        )

        return packetDiscoverContent
    }

    private fun byteArrayToPacketFrame(byteArray: ByteArray) : ModRfUartManager.TalkyDataPacketFrame {
        val byteBuffer = ByteBuffer.wrap(byteArray)
        val channel = byteBuffer.get().toUByte()
        val senderAddress = byteBuffer.get().toUByte()
        val receiverAddress = byteBuffer.get().toUByte()
        val packetsNumber = byteBuffer.getShort().toUShort()
        val packetId = byteBuffer.getShort().toUShort()
        val dataType = byteBuffer.get().toUByte()
        val content = ByteArray(DEFAULT_PACKET_SIZE - FRAME_HEADER_SIZE)
        byteBuffer.get(content)

        val packetFrame = ModRfUartManager.TalkyDataPacketFrame(
            channel,
            senderAddress,
            receiverAddress,
            packetsNumber,
            packetId,
            dataType,
            content
        )

        return packetFrame
    }

    fun getBytesFromUUID(uuid: UUID): ByteArray? {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

    fun getUUIDFromBytes(bytes: ByteArray?): UUID? {
        val byteBuffer = ByteBuffer.wrap(bytes)
        val high = byteBuffer.long
        val low = byteBuffer.long
        return UUID(high, low)
    }

}