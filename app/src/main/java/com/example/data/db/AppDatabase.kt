package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: String): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE name LIKE :query OR phone LIKE :query")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts")
    suspend fun deleteAllContacts()
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE type = :type ORDER BY timestamp DESC")
    fun getConversationsByType(type: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, timestamp = :timestamp, unreadCount = unreadCount + :unreadIncrement WHERE id = :id")
    suspend fun updateLastMessage(id: String, lastMessage: String, timestamp: Long, unreadIncrement: Int)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE conversations SET isLocked = :isLocked WHERE id = :id")
    suspend fun setLocked(id: String, isLocked: Boolean)

    @Query("UPDATE conversations SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: String, isStarred: Boolean)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface MessageDao {
    @Query("SELECT COUNT(*) FROM messages")
    fun getMessagesCount(): Flow<Int>

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE :query")
    fun searchMessages(query: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateMessageStarred(id: String, isStarred: Boolean)

    @Query("UPDATE messages SET isPinned = :isPinned WHERE id = :id")
    suspend fun updateMessagePinned(id: String, isPinned: Boolean)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :id")
    suspend fun updateMessageReactions(id: String, reactions: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages WHERE conversationId = :convId")
    suspend fun deleteMessagesByConversationId(convId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: String): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)
}

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersForGroup(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersForGroupSync(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND memberPhone = :phone LIMIT 1")
    suspend fun getMember(groupId: String, phone: String): GroupMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND memberPhone = :phone")
    suspend fun removeMember(groupId: String, phone: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersByGroup(groupId: String)
}

@Dao
interface GroupPollDao {
    @Query("SELECT * FROM group_polls WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getPollsForGroup(groupId: String): Flow<List<GroupPollEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: GroupPollEntity)
}

@Dao
interface GroupEventDao {
    @Query("SELECT * FROM group_events WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getEventsForGroup(groupId: String): Flow<List<GroupEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: GroupEventEntity)
}

@Dao
interface GroupNotificationLogDao {
    @Query("SELECT * FROM group_notifications WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getNotificationsForGroup(groupId: String): Flow<List<GroupNotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: GroupNotificationLogEntity)
}

@Dao
interface BlockedUserDao {
    @Query("SELECT DISTINCT userIdOrPhone FROM blocked_users")
    fun getBlockedList(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockUser(blocked: BlockedUserEntity)

    @Query("DELETE FROM blocked_users WHERE userIdOrPhone = :id")
    suspend fun unblockUser(id: String)

    @Query("SELECT COUNT(*) FROM blocked_users WHERE userIdOrPhone = :id")
    suspend fun isBlocked(id: String): Int
}

@Database(
    entities = [
        ProfileEntity::class,
        ContactEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        GroupEntity::class,
        BlockedUserEntity::class,
        GroupMemberEntity::class,
        GroupPollEntity::class,
        GroupEventEntity::class,
        GroupNotificationLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun contactDao(): ContactDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun groupPollDao(): GroupPollDao
    abstract fun groupEventDao(): GroupEventDao
    abstract fun groupNotificationLogDao(): GroupNotificationLogDao
    abstract fun blockedUserDao(): BlockedUserDao
}
