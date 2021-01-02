package com.tmobop.talkydroid.storage

import android.content.Context
import android.content.res.Resources
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

//--------------------------------------- Entities -------------------------------------------------

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "user_id")
    var userId: UUID,
    @ColumnInfo(name = "user_name")
    var userName: String,
    @ColumnInfo(name = "avatar")
    var avatar: String,
    @ColumnInfo(name = "online")
    var online: Boolean
)

//-------------------------------------
@Entity(tableName = "message",
        foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["user_id"], childColumns = ["sender_id"], onDelete = ForeignKey.CASCADE)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    var messageId: Int?,
    @ColumnInfo(name = "sender_id", index = true)
    var senderId: UUID,
    @ColumnInfo(name = "receiver_id")
    var receiverId: UUID,
    @ColumnInfo(name = "content")
    var content: String,
    @ColumnInfo(name = "time")
    var time: Long,
    @ColumnInfo(name = "message_type")
    var messageType: String
)

//-------------------------------------

class UserWithMessages {
    @Embedded
    var userEntity: UserEntity? = null

    @Relation(
        parentColumn = "user_id",
        entityColumn = "sender_id"
    )
    var userMessageEntities: List<MessageEntity>? = null
}

//---------------------

class Converters {
    @TypeConverter
    fun fromUUID(value: UUID): String? {
        return value.toString()
    }

    @TypeConverter
    fun toUUID(value: String): UUID? {
        return UUID.fromString(value)
    }
}

//------------------------------------------- Dao --------------------------------------------------

@Dao
interface UserDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(userEntity: UserEntity)

    @Update
    suspend fun updateUser(userEntity: UserEntity)

    @Delete
    suspend fun deleteUser(userEntity: UserEntity)

    @Query("DELETE FROM user")
    suspend fun deleteAllUsers()

    @Query("SELECT * FROM user")
    fun getAllUsers(): LiveData<List<UserEntity>>

    @Query("SELECT * FROM user WHERE user_id = :id")
    fun getUserFromId(id: UUID): LiveData<UserEntity>

    @Query("SELECT EXISTS (SELECT 1 FROM user WHERE user_id = :id)")
    fun exists(id: UUID): LiveData<Boolean>
}

//-----------------------------------------------

@Dao
interface MessageDao {
    @Insert
    suspend fun insertMessage(messageEntity: MessageEntity)

    @Update
    suspend fun updateMessage(messageEntity: MessageEntity)

    @Delete
    suspend fun deleteMessage(messageEntity: MessageEntity)

    @Query("DELETE FROM message")
    suspend fun deleteAllMessages()

    @Query("SELECT * FROM message")
    fun getAllMessages(): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM message WHERE sender_id = :senderId OR receiver_id = :senderId")
    fun getAllMessagesFromUserId(senderId: UUID): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM message ORDER BY time DESC LIMIT 1" )
    fun getLastMessage(): LiveData<MessageEntity>

    @Query("SELECT * FROM message ORDER BY time ASC")
    fun getOrderedMessages(): LiveData<List<MessageEntity>>
}

//-----------------------------------------------

@Dao
interface UserWithMessagesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(userEntity: UserEntity)

    @Query("DELETE FROM user")
    suspend fun deleteAllUsers()

    @Query("DELETE FROM user WHERE user_id = :id")
    suspend fun deleteUserFromUserId(id: UUID)

    @Query("SELECT EXISTS (SELECT 1 FROM user WHERE user_id = :id)")
    fun exists(id: UUID): LiveData<Boolean>

    @Query("UPDATE user SET online = 0")
    suspend fun setAllUsersOffline()

    @Query("UPDATE user SET online = 1 WHERE user_id = :id")
    suspend fun setUserOnline(id: UUID)

    @Query("UPDATE user SET user_name = :userName WHERE user_id = :id")
    suspend fun setUserName(id: UUID, userName: String)

    @Query("UPDATE user SET avatar = :avatarPath WHERE user_id = :id")
    suspend fun setUserAvatar(id: UUID, avatarPath: String)

    @Query("SELECT * FROM message")
    fun getAllMessages(): LiveData<List<MessageEntity>>

    @Query("SELECT * FROM message WHERE sender_id = :senderId OR receiver_id = :senderId")
    fun getAllMessagesFromUserId(senderId: UUID): LiveData<List<MessageEntity>>

    @Transaction
    @Query("SELECT * FROM user")
    fun getUsersWithMessages(): LiveData<List<UserWithMessages>>

    @Transaction
    @Query("SELECT * FROM user WHERE user_id = :id")
    fun getUserWithMessagesFromId(id: UUID): LiveData<UserWithMessages>

    @Query("SELECT * FROM message WHERE sender_id = :senderId AND receiver_id = :receiverId")
    fun getMessagesFromUserIds(senderId: UUID, receiverId: UUID): LiveData<List<MessageEntity?>?>

    @Query("DELETE FROM message WHERE sender_id = :senderId OR receiver_id = :senderId")
    suspend fun deleteAllMessagesInConversation(senderId: UUID)
}

//---------------------

@Database(entities = [UserEntity::class, MessageEntity::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TalkyDroidDatabase : RoomDatabase() {

    // Get all the Dao
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun userWithMessagesDao(): UserWithMessagesDao

    // Database callback
    private class PlayerDatabaseCallback(
        private val scope : CoroutineScope,
        private val resources : Resources
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { _ ->
                scope.launch {
                    //val userWithMessagesDao = database.userWithMessagesDao()
                    //prePopulateDatabase(userWithMessagesDao)
                }
            }
        }

        // Function user for testing
        //private suspend fun prePopulateDatabase(userWithMessagesDao: UserWithMessagesDao) {
        //    val user1 : UserEntity = UserEntity(UUID.fromString("1"), "Paul", "")
        //    val user2 : UserEntity = UserEntity(UUID.fromString("2"), "Michel", "")
        //    val user3 : UserEntity = UserEntity(UUID.fromString("3"), "Jean", "")
//
        //    userWithMessagesDao.insertUser(user1)
        //    userWithMessagesDao.insertUser(user2)
        //    userWithMessagesDao.insertUser(user3)
        //}
    }

    companion object {
        @Volatile
        private var INSTANCE: TalkyDroidDatabase? = null

        // Get instance
        fun getDatabase(
            context: Context,
            coroutineScope: CoroutineScope,
            resources: Resources
        ): TalkyDroidDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TalkyDroidDatabase::class.java,
                    "TalkyDroid_database"
                )
                    .addCallback(PlayerDatabaseCallback(coroutineScope, resources))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}