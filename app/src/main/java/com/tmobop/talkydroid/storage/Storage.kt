import android.content.Context
import android.util.Log
import com.tmobop.talkydroid.classes.MessageUI
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object Storage {
    private val LOG_TAG = Storage::class.java.simpleName
    private const val PATH = "message_list/"
    private const val EXTENSION = ".ser"

    fun writeData(context: Context, message: MessageUI, fileName: String) {
        var fos: FileOutputStream? = null
        var oos: ObjectOutputStream? = null

        try {
            // Open file and write list
            fos = context.openFileOutput(PATH + fileName + EXTENSION, Context.MODE_PRIVATE)
            oos = ObjectOutputStream(fos)
            oos.writeObject(message)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not write to file.")
            e.printStackTrace()
        } finally {
            try {
                oos?.close()
                fos?.close()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Could not close the file.")
                e.printStackTrace()
            }

        }
    }

    fun eraseData(context: Context, fileName: String) {
        var fos: FileOutputStream? = null
        var oos: ObjectOutputStream? = null
        try {
            // Open file and write list
            fos = context.openFileOutput(PATH + fileName + EXTENSION, Context.MODE_PRIVATE)
            oos = ObjectOutputStream(fos)
            oos.writeObject((""))
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not write to file.")
            e.printStackTrace()
        } finally {
            try {
                oos?.close()
                fos?.close()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Could not close the file.")
                e.printStackTrace()
            }
        }
    }

    fun readData(context: Context, fileName: String): MutableList<MessageUI>? {
        var fis: FileInputStream? = null
        var ois: ObjectInputStream? = null

        var messageList: MutableList<MessageUI>? = ArrayList()

        try {
            // Open file and read list
            fis = context.openFileInput(PATH + fileName + EXTENSION)
            ois = ObjectInputStream(fis)

            messageList = ois.readObject() as? MutableList<MessageUI>
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not read from file.")
            e.printStackTrace()
        } finally {
            try {
                ois?.close()
                fis?.close()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Could not close the file.")
                e.printStackTrace()
            }

        }

        return messageList
    }
}
