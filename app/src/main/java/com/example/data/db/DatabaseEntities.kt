package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String, // email or uuid from Supabase
    val displayName: String,
    val phone: String,
    val email: String,
    val bio: String,
    val profilePhoto: String = "",
    val onlineStatus: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val hideLastSeen: Boolean = false,
    val hideProfilePhoto: Boolean = false,
    val hideOnlineStatus: Boolean = false
) : Serializable

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String, // phone number or id
    val name: String,
    val phone: String,
    val email: String = "",
    val isCloud: Boolean = false,
    val isFavorite: Boolean = false,
    val avatarUri: String = ""
) : Serializable

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String, // participant phone or cloud conversation UID
    val title: String,
    val type: String, // "SMS" or "CLOUD"
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val isLocked: Boolean = false,
    val isStarred: Boolean = false,
    val profilePhoto: String = "",
    val carrierName: String = "SIM 1" // For Carrier/SIM selection
) : Serializable

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String, // Unique UID
    val conversationId: String,
    val senderIdOrPhone: String,
    val content: String,
    val type: String, // "SMS" or "CLOUD"
    val status: String, // "SENDING", "SENT", "DELIVERED", "SEEN", "FAILED"
    val simSlot: Int = 0, // 0 for SIM 1, 1 for SIM 2
    val timestamp: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false,
    val isForwarded: Boolean = false,
    // Group Features Core Extensions
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reactions: String = "", // Serialized text or simple comma-separated string e.g. "👍,❤️"
    val isPinned: Boolean = false,
    val mentions: String = "" // Serialized mentions string e.g. "@alex"
) : Serializable

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarUrl: String = "",
    val adminId: String,
    val description: String = "",
    val membersCount: Int = 1,
    // Expanded Metadata for Premium Controls
    val category: String = "Chat", // Chat, Social, Office, Study, Announcement
    val groupType: String = "Private", // Private, Public, Community, Announcement
    val inviteUrl: String = "",
    val qrCodePayload: String = "",
    val hiddenMembers: Boolean = false,
    val adminOnlyMessage: Boolean = false,
    val adminOnlyAnnounce: Boolean = false,
    val requireApproval: Boolean = false,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val isEncrypted: Boolean = true // End-to-end encryption toggle
) : Serializable

@Entity(tableName = "group_members")
data class GroupMemberEntity(
    @PrimaryKey val uniqueId: String, // groupId + "_" + phone
    val groupId: String,
    val memberPhone: String,
    val memberName: String,
    val role: String, // "OWNER", "ADMIN", "MEMBER"
    val isMuted: Boolean = false,
    val isBanned: Boolean = false
) : Serializable

@Entity(tableName = "group_polls")
data class GroupPollEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val creatorPhone: String,
    val creatorName: String,
    val question: String,
    val optionsJson: String, // Comma or pipe separated options
    val votesJson: String, // Serialized map or list of votes (option_index:count)
    val isAnonymous: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "group_events")
data class GroupEventEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val creatorName: String,
    val title: String,
    val description: String,
    val eventDateText: String, // formatted readable event date
    val timestamp: Long = System.currentTimeMillis(),
    val rsvpYesList: String = "", // Comma-separated member names/phones who RSVP'd YES
    val rsvpNoList: String = "" // RSVP NO
) : Serializable

@Entity(tableName = "group_notifications")
data class GroupNotificationLogEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val text: String,
    val type: String, // "JOIN", "LEAVE", "PROMOTION", "PHOTO", "NAME", "ANNOUNCEMENT"
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "blocked_users")
data class BlockedUserEntity(
    @PrimaryKey val userIdOrPhone: String
) : Serializable
