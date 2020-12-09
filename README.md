# Install on VM

Because the android emulator that is associated with Android Studio does not support external USB connection, it can be useful to be able to access physical devices (USB, COM) on a emulated machine. The following steps explain how to use Android Studio with a custom VM that allows debugging and physical external hardware acces.

1. Download Android SDK https://developer.android.com/studio/releases/platform-tools
2. Put SDK into a folder and add it to PATH (Environment variables)
3. Download Android x86 VM for VirtualBox https://www.osboxes.org/android-x86/
4. Perform normal installation (2048 MB of RAM)
5. Configure VM network : Configure -> Network -> Adapter 1 -> Access mode : Bridge
6. Go to Configure -> USB
	- Activate USB 2.0
	- Plug RFmodule into USB
	- Add new filter (+) -> Select corresponding devices (FTDI...)
7. Configure VM COM ports : Configure -> Serial ports -> Activate Port 1 & 2
8. Validate changes
9. Start VM
10. Activate Developper mode (Settings -> System -> About Tablet : Click multiple times on Build number)
11. Go to Setting -> System -> About Tablet -> Advanced -> Developper options
	- Activate USB Debugging
	- Root Access : Apps and ADB
12. Install WiFi ADB from PlayStore
13. Launch WiFi ADB and enable it (slide to right multiple times if it doesn't work).
14. Once WiFi ADB launched, on host PC : in terminal type `adb connect <IP>:<port>`.
15. On Android Studio : innotek GmbH Virtualbox should be shown in selected device.
16. Install/debug as usual.



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
