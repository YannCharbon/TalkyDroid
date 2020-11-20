# ModRfUartManager class public content resume

The listener of this class must be implemented in MainActivity. Calls to others activities must be performed with help of Intents.

```
package com.tmobop.talkydroid.classes

....


const val DEFAULT_PACKET_SIZE = 62

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

    init {
        ...
    }

    @Synchronized
    fun getListener(): ModRfUartManager.Listener? {
        return mListener
    }

    companion object{
        lateinit var activityContext : Context

    }

    public fun close(){
        ...
    }

    public fun getDevice() : Int{
        ....
    }

    public fun getDeviceProporties() : UsbDeviceProperties? {
		...
    }



    public fun writeText(text: String, destUserUUID: String) : Int{
        ...
    }

    public fun discoverNetworkDevicesAndGetAddress(setAddressAfterDiscover: Boolean) {
        ...
    }
}
```