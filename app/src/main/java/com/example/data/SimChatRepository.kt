package com.example.data

import android.content.Context
import com.example.data.db.*
import com.example.data.sms.SmsManagerWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class SimChatRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val smsWrapper: SmsManagerWrapper
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // DAOs
    private val profileDao = database.profileDao()
    private val contactDao = database.contactDao()
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val groupDao = database.groupDao()
    private val blockedDao = database.blockedUserDao()
    private val groupMemberDao = database.groupMemberDao()
    private val groupPollDao = database.groupPollDao()
    private val groupEventDao = database.groupEventDao()
    private val groupNotificationLogDao = database.groupNotificationLogDao()

    // Public flows
    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val contacts: Flow<List<ContactEntity>> = contactDao.getAllContacts()
    val favorites: Flow<List<ContactEntity>> = contactDao.getFavoriteContacts()
    val groups: Flow<List<GroupEntity>> = groupDao.getAllGroups()
    val blockedList: Flow<List<String>> = blockedDao.getBlockedList()
    val messagesCount: Flow<Int> = messageDao.getMessagesCount()
    val starredMessages: Flow<List<MessageEntity>> = messageDao.getStarredMessages()

    private val _incomingFcmFlow = MutableSharedFlow<com.example.viewmodel.SimulatedNotification>(extraBufferCapacity = 64)
    val incomingFcmFlow: SharedFlow<com.example.viewmodel.SimulatedNotification> = _incomingFcmFlow.asSharedFlow()

    // Realtime typing status or presence state simulated per conversation ID
    private val _typingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStates: StateFlow<Map<String, Boolean>> = _typingStates.asStateFlow()

    init {
        // Start with a clean slate for real carriers - no mock data injected on init
    }

    // Messages interactions
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(convId)
    }

    suspend fun sendMessage(
        convId: String, 
        content: String, 
        type: String, 
        mode: String,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSender: String? = null,
        mentions: String = ""
    ): String {
        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val meEmail = sessionManager.userEmail ?: "me@simchat.com"

        // Create the user message
        val userMsg = MessageEntity(
            id = msgId,
            conversationId = convId,
            senderIdOrPhone = "ME",
            content = content,
            type = mode, // "SMS" or "CLOUD"
            status = "SENDING",
            simSlot = if (sessionManager.preferredCarrierSim == "SIM 1") 0 else 1,
            timestamp = now,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSender = replyToSender,
            reactions = "",
            isPinned = false,
            mentions = mentions
        )
        messageDao.insertMessage(userMsg)

        // Find or create Conversation holder
        val conv = conversationDao.getConversationById(convId)
        if (conv == null) {
            val fallbackTitle = database.contactDao().getAllContacts().firstOrNull()?.find { it.id == convId }?.name ?: convId
            conversationDao.insertConversation(
                ConversationEntity(
                    id = convId,
                    title = fallbackTitle,
                    type = mode,
                    lastMessage = content,
                    timestamp = now,
                    unreadCount = 0,
                    carrierName = sessionManager.preferredCarrierSim
                )
            )
        } else {
            conversationDao.updateLastMessage(convId, content, now, 0)
        }

        // Send depending on mode
        if (mode == "SMS") {
            smsWrapper.sendSmsMessage(convId, content, if (sessionManager.preferredCarrierSim == "SIM 1") 0 else 1) { status ->
                scope.launch {
                    messageDao.updateMessageStatus(msgId, status)
                }
            }
        } else {
            // CLOUD mode
            messageDao.updateMessageStatus(msgId, "SENT")
            scope.launch {
                delay(800)
                messageDao.updateMessageStatus(msgId, "DELIVERED")
                delay(800)
                messageDao.updateMessageStatus(msgId, "SEEN")
            }
        }

        // HYBRID FALLBACK LOGIC:
        // If this is a group conversation, notify non-cloud SMS fallback members via SMS!
        val group = groupDao.getGroupById(convId)
        if (group != null) {
            // Retrieve members who are not registered on cloud (simulate SMS falling back)
            scope.launch {
                val members = database.groupMemberDao().getMembersForGroupSync(convId)
                members.forEach { m ->
                    // If member has phone and isn't cloud/registered, send important fallback SMS!
                    if (m.memberPhone != meEmail && m.memberPhone.contains("@") == false && (group.groupType == "Announcement" || group.adminOnlyAnnounce)) {
                        android.util.Log.d("SimChatRepository", "HYBRID DISPATCH: Sending fallback SMS message to unregistered recipient ${m.memberPhone} for Announcement Group '${group.name}'")
                        smsWrapper.sendSmsMessage(m.memberPhone, "[Grp: ${group.name}] ${m.memberName}: $content", if (sessionManager.preferredCarrierSim == "SIM 1") 0 else 1) { }
                    }
                }
            }
            // Trigger customized responses
            simulateGroupResponses(group, content)
        } else {
            // Trigger simulated response to user's message
            triggerAutoReply(convId, content, mode)
        }

        return msgId
    }

    private fun triggerAutoReply(convId: String, content: String, mode: String) {
        scope.launch {
            // Simulation logic
            delay(1500)
            setTyping(convId, true)
            delay(2000)
            setTyping(convId, false)

            val replyId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val lowercaseContent = content.lowercase()

            val response = when {
                lowercaseContent.contains("hello") || lowercaseContent.contains("hey") || lowercaseContent.contains("hi") -> {
                    "Hi there! This is a real-time message response from SimChat's cloud backend simulation engine."
                }
                lowercaseContent.contains("ota") || lowercaseContent.contains("otp") || lowercaseContent.contains("code") -> {
                    "Your SimChat OTP verification code is: 582496. Please do not share this with anyone."
                }
                lowercaseContent.contains("link") || lowercaseContent.contains("website") || lowercaseContent.contains("url") -> {
                    "Check out our secure website here: https://ai.studio/build. It's safe!"
                }
                lowercaseContent.contains("phone") || lowercaseContent.contains("call") -> {
                    "Sure! You can call support at +254 711 223 344 or email info@simchat.com"
                }
                else -> {
                    "Received in real-time! Let's build! 🚀"
                }
            }

            val replyMsg = MessageEntity(
                id = replyId,
                conversationId = convId,
                senderIdOrPhone = convId,
                content = response,
                type = mode,
                status = "SENT",
                timestamp = now
            )
            messageDao.insertMessage(replyMsg)
            conversationDao.updateLastMessage(convId, response, now, 1)
        }
    }

    private fun setTyping(convId: String, isTyping: Boolean) {
        val current = _typingStates.value.toMutableMap()
        current[convId] = isTyping
        _typingStates.value = current
    }

    // Actions
    suspend fun clearUnreads(convId: String) {
        conversationDao.markAsRead(convId)
    }

    suspend fun toggleStarConversation(convId: String, isStarred: Boolean) {
        conversationDao.setStarred(convId, isStarred)
    }

    suspend fun toggleLockConversation(convId: String, isLocked: Boolean) {
        conversationDao.setLocked(convId, isLocked)
    }

    suspend fun deleteConversation(convId: String) {
        val conv = conversationDao.getConversationById(convId)
        if (conv != null) {
            database.messageDao().deleteMessagesByConversationId(convId)
            conversationDao.deleteConversation(conv)
        }
    }

    suspend fun blockContact(id: String) {
        blockedDao.blockUser(BlockedUserEntity(id))
    }

    suspend fun unblockContact(id: String) {
        blockedDao.unblockUser(id)
    }

    suspend fun createGroup(name: String, desc: String, avatar: String) {
        createPremiumGroup(
            name = name,
            desc = desc,
            avatar = avatar,
            category = "Chat",
            groupType = "Private",
            memberList = emptyList()
        )
    }

    suspend fun createPremiumGroup(
        name: String, 
        desc: String, 
        avatar: String, 
        category: String, 
        groupType: String, 
        memberList: List<ContactEntity>,
        hiddenMembers: Boolean = false,
        adminOnlyMessage: Boolean = false,
        adminOnlyAnnounce: Boolean = false,
        requireApproval: Boolean = false,
        isEncrypted: Boolean = true
    ): String {
        val groupId = "group_${UUID.randomUUID()}"
        val userEmail = sessionManager.userEmail ?: "me@simchat.com"
        val userName = sessionManager.userName ?: "You"
        
        // Build unique invite links and QR codes
        val inviteUrl = "https://simchat.app/join/$groupId"
        
        val group = GroupEntity(
            id = groupId,
            name = name,
            avatarUrl = avatar,
            adminId = userEmail,
            description = desc,
            membersCount = memberList.size + 1,
            category = category,
            groupType = groupType,
            inviteUrl = inviteUrl,
            qrCodePayload = "simchat_group_qr:$groupId",
            hiddenMembers = hiddenMembers,
            adminOnlyMessage = adminOnlyMessage,
            adminOnlyAnnounce = adminOnlyAnnounce,
            requireApproval = requireApproval,
            isArchived = false,
            isMuted = false,
            isEncrypted = isEncrypted
        )
        
        groupDao.insertGroup(group)
        
        // Insert conversation holder for group chat
        val now = System.currentTimeMillis()
        conversationDao.insertConversation(
            ConversationEntity(
                id = groupId,
                title = name,
                type = "CLOUD",
                lastMessage = "$userName created the group",
                timestamp = now,
                unreadCount = 0,
                isGroup = true,
                profilePhoto = avatar
            )
        )
        
        // Insert Owner member
        groupMemberDao.insertMember(
            GroupMemberEntity(
                uniqueId = "${groupId}_owner",
                groupId = groupId,
                memberPhone = userEmail,
                memberName = "$userName (Owner)",
                role = "OWNER"
            )
        )
        
        // Auto-insert other selected members
        memberList.forEach { contact ->
            groupMemberDao.insertMember(
                GroupMemberEntity(
                    uniqueId = "${groupId}_${contact.phone}",
                    groupId = groupId,
                    memberPhone = contact.phone,
                    memberName = contact.name,
                    role = "MEMBER"
                )
            )
            
            // Log joining notification
            logGroupNotification(groupId, "${contact.name} was added to the group", "JOIN")
            
            // Hybrid SMS fallback announcement if groupType is Announcement, member is not cloud
            if (groupType == "Announcement" && !contact.isCloud) {
                try {
                    smsWrapper.sendSmsMessage(
                        contact.phone,
                        "[SimChat Hybrid SMS]: You have been added to the Announcement group '$name'. Link: $inviteUrl",
                        if (sessionManager.preferredCarrierSim == "SIM 1") 0 else 1
                    ) { }
                } catch (e: Exception) {
                    android.util.Log.e("SimChatRepository", "Failed sending fallback announcement carrier SMS", e)
                }
            }
        }
        
        // Log group configuration notification
        logGroupNotification(groupId, "$userName created group '$name' with ${memberList.size + 1} members", "JOIN")
        
        return groupId
    }

    suspend fun getGroupById(groupId: String): GroupEntity? {
        return groupDao.getGroupById(groupId)
    }

    fun getMembersForGroup(groupId: String): Flow<List<GroupMemberEntity>> =
        groupMemberDao.getMembersForGroup(groupId)

    fun getPollsForGroup(groupId: String): Flow<List<GroupPollEntity>> =
        groupPollDao.getPollsForGroup(groupId)

    fun getEventsForGroup(groupId: String): Flow<List<GroupEventEntity>> =
        groupEventDao.getEventsForGroup(groupId)

    fun getNotificationsForGroup(groupId: String): Flow<List<GroupNotificationLogEntity>> =
        groupNotificationLogDao.getNotificationsForGroup(groupId)

    suspend fun updateGroupPhoto(groupId: String, photo: String) {
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            groupDao.insertGroup(group.copy(avatarUrl = photo))
            // Update conv as well
            val conv = conversationDao.getConversationById(groupId)
            if (conv != null) {
                conversationDao.insertConversation(conv.copy(profilePhoto = photo))
            }
            logGroupNotification(groupId, "Group profile photo was updated", "PHOTO")
        }
    }

    suspend fun updateGroupName(groupId: String, name: String) {
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            groupDao.insertGroup(group.copy(name = name))
            val conv = conversationDao.getConversationById(groupId)
            if (conv != null) {
                conversationDao.insertConversation(conv.copy(title = name))
            }
            logGroupNotification(groupId, "Group name was updated to '$name'", "NAME")
        }
    }

    suspend fun updateGroupDescription(groupId: String, desc: String) {
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            groupDao.insertGroup(group.copy(description = desc))
        }
    }

    suspend fun updateGroupPrivacySettings(
        groupId: String,
        groupType: String,
        hiddenMembers: Boolean,
        adminOnlyMessage: Boolean,
        adminOnlyAnnounce: Boolean,
        requireApproval: Boolean
    ) {
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            groupDao.insertGroup(
                group.copy(
                    groupType = groupType,
                    hiddenMembers = hiddenMembers,
                    adminOnlyMessage = adminOnlyMessage,
                    adminOnlyAnnounce = adminOnlyAnnounce,
                    requireApproval = requireApproval
                )
            )
            logGroupNotification(groupId, "Group privacy & notification privileges updated", "ANNOUNCEMENT")
        }
    }

    suspend fun logGroupNotification(groupId: String, text: String, type: String) {
        groupNotificationLogDao.insertLog(
            GroupNotificationLogEntity(
                id = "grp_notif_${UUID.randomUUID()}",
                groupId = groupId,
                text = text,
                type = type
            )
        )
    }

    suspend fun addGroupMember(groupId: String, phone: String, name: String, role: String = "MEMBER") {
        groupMemberDao.insertMember(
            GroupMemberEntity(
                uniqueId = "${groupId}_${phone}",
                groupId = groupId,
                memberPhone = phone,
                memberName = name,
                role = role
            )
        )
        // Increment members count
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            groupDao.insertGroup(group.copy(membersCount = group.membersCount + 1))
        }
        logGroupNotification(groupId, "$name joined the group", "JOIN")
    }

    suspend fun removeGroupMember(groupId: String, phone: String, name: String) {
        groupMemberDao.removeMember(groupId, phone)
        // Decrement members count
        val group = groupDao.getGroupById(groupId)
        if (group != null) {
            groupDao.insertGroup(group.copy(membersCount = maxOf(1, group.membersCount - 1)))
        }
        logGroupNotification(groupId, "$name was removed from the group", "LEAVE")
    }

    suspend fun promoteMember(groupId: String, phone: String, name: String, role: String) {
        val m = groupMemberDao.getMember(groupId, phone)
        if (m != null) {
            groupMemberDao.insertMember(m.copy(role = role))
            logGroupNotification(groupId, "$name was promoted to $role", "PROMOTION")
        }
    }

    suspend fun muteMember(groupId: String, phone: String, isMuted: Boolean) {
        val m = groupMemberDao.getMember(groupId, phone)
        if (m != null) {
            groupMemberDao.insertMember(m.copy(isMuted = isMuted))
        }
    }

    suspend fun banMember(groupId: String, phone: String, isBanned: Boolean) {
        val m = groupMemberDao.getMember(groupId, phone)
        if (m != null) {
            groupMemberDao.insertMember(m.copy(isBanned = isBanned))
            logGroupNotification(groupId, "${m.memberName} was banned from group", "LEAVE")
        }
    }

    suspend fun leaveGroup(id: String) {
        groupDao.deleteGroupById(id)
        groupMemberDao.deleteMembersByGroup(id)
        val conv = conversationDao.getConversationById(id)
        if (conv != null) {
            conversationDao.deleteConversation(conv)
        }
    }

    // Direct mutators for Polls & Events
    suspend fun createGroupPoll(groupId: String, question: String, options: List<String>, isAnonymous: Boolean) {
        val id = "poll_${UUID.randomUUID()}"
        val userEmail = sessionManager.userEmail ?: "me@simchat.com"
        val userName = sessionManager.userName ?: "You"
        
        val poll = GroupPollEntity(
            id = id,
            groupId = groupId,
            creatorPhone = userEmail,
            creatorName = userName,
            question = question,
            optionsJson = options.joinToString("|"),
            votesJson = List(options.size) { "0" }.joinToString("|"),
            isAnonymous = isAnonymous
        )
        groupPollDao.insertPoll(poll)
        
        // Log notification announcement
        logGroupNotification(groupId, "New Poll started: '$question'", "ANNOUNCEMENT")
        sendMessage(groupId, "📊 Created a poll: \"$question\". Vote in Group Info!", "CLOUD", "CLOUD")
    }

    suspend fun createGroupEvent(groupId: String, title: String, description: String, dateText: String) {
        val id = "event_${UUID.randomUUID()}"
        val event = GroupEventEntity(
            id = id,
            groupId = groupId,
            creatorName = sessionManager.userName ?: "Admin",
            title = title,
            description = description,
            eventDateText = dateText,
            rsvpYesList = "",
            rsvpNoList = ""
        )
        groupEventDao.insertEvent(event)
        
        // Log event notification
        logGroupNotification(groupId, "New Scheduled Event: '$title'", "ANNOUNCEMENT")
        sendMessage(groupId, "🗓️ New Scheduled Event: \"$title\" on $dateText. RSVP in Group Info!", "CLOUD", "CLOUD")
    }

    suspend fun savePoll(poll: GroupPollEntity) {
        groupPollDao.insertPoll(poll)
    }

    suspend fun saveEvent(event: GroupEventEntity) {
        groupEventDao.insertEvent(event)
    }

    suspend fun togglePinMessage(msgId: String, isPinned: Boolean) {
        messageDao.updateMessagePinned(msgId, isPinned)
    }

    suspend fun addReaction(msgId: String, reaction: String) {
        messageDao.updateMessageReactions(msgId, reaction)
    }

    // Dynamic Multi-Role/Multi-User Group Chat Simulation Simulator
    private fun simulateGroupResponses(group: GroupEntity, userMessage: String) {
        scope.launch {
            // Find possible responder names from mock contacts list or group member registries
            val repliesAvailable = listOf(
                "Alice Monroe" to "Hey everyone! I highly support this! Let's schedule a call tomorrow. 👍",
                "Captain Jack" to "Acknowledged! Ready to deploy resources whenever admins give the signal. 🚀",
                "Sarah Connor" to "Is safety configuration enabled? Keep end-to-end encryption ACTIVE.",
                "Developer Support" to "Great progress! Hybrid SMS announcements are working fine for SIM fallback."
            )
            
            // Randomly delay response
            delay(2000)
            
            // Choose responder
            val (sender, replyText) = repliesAvailable.random()
            
            // Start typing indicator
            setTyping(group.id, true)
            delay(1500)
            setTyping(group.id, false)
            
            val replyId = "sim_grp_msg_${UUID.randomUUID()}"
            val replyMsg = MessageEntity(
                id = replyId,
                conversationId = group.id,
                senderIdOrPhone = sender,
                content = replyText,
                type = "CLOUD",
                status = "SENT",
                timestamp = System.currentTimeMillis()
            )
            
            messageDao.insertMessage(replyMsg)
            conversationDao.updateLastMessage(group.id, replyText, System.currentTimeMillis(), 1)
        }
    }

    suspend fun insertContact(contact: ContactEntity) {
        contactDao.insertContact(contact)
    }

    suspend fun deleteContact(contact: ContactEntity) {
        contactDao.deleteContact(contact)
    }

    suspend fun importSmsMessages() {
        // Real SMS migration from the Android system content telephony provider
        syncRealDeviceSms()
    }

    suspend fun wipeDatabase() {
        database.clearAllTables()
    }

    suspend fun deleteAllConversations() {
        database.conversationDao().deleteAllConversations()
        database.messageDao().deleteAllMessages()
    }

    suspend fun deleteMessage(msgId: String) {
        database.messageDao().deleteMessageById(msgId)
    }

    suspend fun toggleStarMessage(msgId: String, isStarred: Boolean) {
        database.messageDao().updateMessageStarred(msgId, isStarred)
    }

    suspend fun forwardMessage(convId: String, content: String, mode: String) {
        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // Create user forwarded message
        val userMsg = MessageEntity(
            id = msgId,
            conversationId = convId,
            senderIdOrPhone = "ME",
            content = content,
            type = mode,
            status = "SENT",
            simSlot = if (sessionManager.preferredCarrierSim == "SIM 1") 0 else 1,
            timestamp = now,
            isForwarded = true
        )
        database.messageDao().insertMessage(userMsg)

        val conv = database.conversationDao().getConversationById(convId)
        if (conv == null) {
            val fallbackTitle = database.contactDao().getContactById(convId)?.name ?: convId
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = convId,
                    title = fallbackTitle,
                    type = mode,
                    lastMessage = content,
                    timestamp = now,
                    unreadCount = 0,
                    carrierName = sessionManager.preferredCarrierSim
                )
            )
        } else {
            database.conversationDao().updateLastMessage(convId, content, now, 0)
        }
    }

    suspend fun handleIncomingCarrierSms(sender: String, body: String) {
        val cleanSender = sender.replace(Regex("[^+\\d]"), "")
        val isBlockedDirect = blockedDao.isBlocked(sender) > 0 || blockedDao.isBlocked(cleanSender) > 0
        val isBlockedInList = try {
            blockedDao.getBlockedList().first().any { blockedId ->
                blockedId == sender || 
                blockedId.replace(Regex("[^+\\d]"), "") == cleanSender
            }
        } catch (e: Exception) {
            false
        }
        if (isBlockedDirect || isBlockedInList) {
            android.util.Log.d("SimChatRepository", "Incoming SMS from blocked address '$sender' was ignored/suppressed by carrier.")
            return
        }

        val now = System.currentTimeMillis()
        val msgId = UUID.randomUUID().toString()

        // Insert message
        val incomingMsg = MessageEntity(
            id = msgId,
            conversationId = sender,
            senderIdOrPhone = sender,
            content = body,
            type = "SMS",
            status = "DELIVERED",
            simSlot = 0,
            timestamp = now
        )
        database.messageDao().insertMessage(incomingMsg)

        // Find or create Conversation
        val conv = database.conversationDao().getConversationById(sender)
        val contactName = database.contactDao().getContactById(sender)?.name ?: sender
        if (conv == null) {
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = sender,
                    title = contactName,
                    type = "SMS",
                    lastMessage = body,
                    timestamp = now,
                    unreadCount = 1,
                    carrierName = "SIM 1"
                )
            )
        } else {
            database.conversationDao().updateLastMessage(sender, body, now, 1)
        }

        // Show a standard Android notification
        showPushNotification(contactName, body)
    }

    private fun showPushNotification(contactName: String, body: String) {
        val channelId = "simchat_carrier_incoming"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "SimChat Incoming Carrier SMS",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when real carrier text messages are received"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(context, com.example.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntentFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = android.app.PendingIntent.getActivity(context, 0, intent, pendingIntentFlags)

        val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setContentTitle("SMS from $contactName")
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    suspend fun syncRealDeviceSms() {
        try {
            val uri = android.net.Uri.parse("content://sms")
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("_id", "address", "body", "date", "type"),
                null,
                null,
                "date DESC LIMIT 100"
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")
                val typeIdx = c.getColumnIndex("type")

                while (c.moveToNext()) {
                    val address = if (addressIdx != -1) c.getString(addressIdx) else null
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) else null
                    val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx != -1) c.getInt(typeIdx) else 1

                    if (!address.isNullOrBlank() && !body.isNullOrBlank()) {
                        val sender = if (type == 1) address else "ME"
                        val convId = address

                        val conv = database.conversationDao().getConversationById(convId)
                        if (conv == null) {
                            val contactName = database.contactDao().getContactById(convId)?.name ?: convId
                            database.conversationDao().insertConversation(
                                ConversationEntity(
                                    id = convId,
                                    title = contactName,
                                    type = "SMS",
                                    lastMessage = body,
                                    timestamp = date,
                                    unreadCount = 0,
                                    carrierName = "SIM 1"
                                )
                            )
                        }

                        val msgId = "real_sms_${c.getLong(c.getColumnIndex("_id"))}"
                        val msg = MessageEntity(
                            id = msgId,
                            conversationId = convId,
                            senderIdOrPhone = sender,
                            content = body,
                            type = "SMS",
                            status = "DELIVERED",
                            simSlot = 0,
                            timestamp = date
                        )
                        database.messageDao().insertMessage(msg)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SimChatRepository", "Failed to sync real device SMS Data Provider", e)
        }
    }

    suspend fun syncRealDeviceContacts() {
        try {
            val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )

            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (c.moveToNext()) {
                    val name = if (nameIdx != -1) c.getString(nameIdx) else null
                    val number = if (numberIdx != -1) c.getString(numberIdx) else null

                    if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                        val cleanNumber = number.replace("\\s".toRegex(), "")
                        val contact = ContactEntity(
                            id = cleanNumber,
                            name = name,
                            phone = cleanNumber,
                            email = "",
                            isCloud = false,
                            isFavorite = false
                        )
                        database.contactDao().insertContact(contact)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SimChatRepository", "Failed to sync on-device Contacts Contract", e)
        }
    }

    suspend fun getRealDeviceCallLogs(): List<CallLogItem> {
        val list = mutableListOf<CallLogItem>()
        try {
            val uri = android.provider.CallLog.Calls.CONTENT_URI
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.CallLog.Calls._ID,
                    android.provider.CallLog.Calls.CACHED_NAME,
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.DATE,
                    android.provider.CallLog.Calls.TYPE
                ),
                null,
                null,
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 50"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(android.provider.CallLog.Calls._ID)
                val nameIdx = c.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                val numberIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val typeIdx = c.getColumnIndex(android.provider.CallLog.Calls.TYPE)

                while (c.moveToNext()) {
                    val id = if (idIdx != -1) c.getString(idIdx) else ""
                    val name = if (nameIdx != -1) c.getString(nameIdx) ?: "" else ""
                    val number = if (numberIdx != -1) c.getString(numberIdx) ?: "" else ""
                    val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx != -1) c.getInt(typeIdx) else 1

                    val displayName = name.ifBlank { number }
                    list.add(CallLogItem(id, displayName, number, date, type))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SimChatRepository", "Failed to query real device Call Log", e)
        }
        return list
    }

    suspend fun handleIncomingFcmMessage(simNotif: com.example.viewmodel.SimulatedNotification) {
        showPushNotification("Cloud: ${simNotif.senderName}", simNotif.messageText)
        _incomingFcmFlow.emit(simNotif)
    }

    fun handleSimulatedNotificationPush(sender: String, body: String) {
        showPushNotification("SIM Carrier: $sender", body)
    }
}

data class CallLogItem(
    val id: String,
    val name: String,
    val phone: String,
    val timestamp: Long,
    val type: Int
)

