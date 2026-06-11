package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.SimChatApplication
import com.example.data.SessionManager
import com.example.data.SimChatRepository
import com.example.data.CallLogItem
import com.example.data.db.*
import com.example.data.sms.SimSlotInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SimChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SimChatApplication
    val repository: SimChatRepository = app.repository
    val sessionManager: SessionManager = app.sessionManager

    // UI state flows from local DB
    val contacts: StateFlow<List<ContactEntity>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<ContactEntity>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<GroupEntity>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedList: StateFlow<List<String>> = repository.blockedList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messagesCount: StateFlow<Int> = repository.messagesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val starredMessages: StateFlow<List<MessageEntity>> = repository.starredMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val typingStates: StateFlow<Map<String, Boolean>> = repository.typingStates

    private val _callLogs = MutableStateFlow<List<CallLogItem>>(emptyList())
    val callLogs: StateFlow<List<CallLogItem>> = _callLogs.asStateFlow()

    // View states (Filter, Search query, Carrier selection)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All") // "All", "SMS", "Cloud", "Unread", "Starred"
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Splash)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _chatBackgroundTheme = MutableStateFlow(sessionManager.chatBackgroundTheme)
    val chatBackgroundTheme: StateFlow<String> = _chatBackgroundTheme.asStateFlow()

    private val _localWallpaperUri = MutableStateFlow(sessionManager.localWallpaperUri)
    val localWallpaperUri: StateFlow<String> = _localWallpaperUri.asStateFlow()

    private val _chatTextColorHex = MutableStateFlow(sessionManager.chatTextColorHex)
    val chatTextColorHex: StateFlow<String> = _chatTextColorHex.asStateFlow()

    private val _chatFontFamily = MutableStateFlow(sessionManager.chatFontFamily)
    val chatFontFamily: StateFlow<String> = _chatFontFamily.asStateFlow()

    fun updateChatBackgroundTheme(theme: String) {
        sessionManager.chatBackgroundTheme = theme
        _chatBackgroundTheme.value = theme
    }

    fun updateLocalWallpaperUri(uri: String) {
        sessionManager.localWallpaperUri = uri
        _localWallpaperUri.value = uri
    }

    fun updateChatTextColorHex(hex: String) {
        sessionManager.chatTextColorHex = hex
        _chatTextColorHex.value = hex
    }

    fun updateChatFontFamily(font: String) {
        sessionManager.chatFontFamily = font
        _chatFontFamily.value = font
    }

    private val _showUpgradeDialog = MutableStateFlow(false)
    val showUpgradeDialog: StateFlow<Boolean> = _showUpgradeDialog.asStateFlow()

    private val _verificationPendingAccount = MutableStateFlow<String?>(null)
    val verificationPendingAccount: StateFlow<String?> = _verificationPendingAccount.asStateFlow()

    fun setVerificationPendingAccount(account: String?) {
        _verificationPendingAccount.value = account
    }

    fun completeDeviceVerification(account: String) {
        sessionManager.setAccountFingerprint(account, sessionManager.deviceFingerprint)
        _verificationPendingAccount.value = null
    }

    fun simulateUnrecognizedDevice() {
        val manufacturer = listOf("GOOGLE", "SAMSUNG", "XIAOMI", "PLUSONE", "SONY").random()
        val model = listOf("PIXEL-9", "GALAXY-S25", "REDMI-NOTE", "NORD-CE", "XPERIA-PRO").random()
        val randomSuffix = (1000..9999).random().toString()
        sessionManager.deviceFingerprint = "${manufacturer}-${model}-$randomSuffix"
    }

    fun resetRegisteredDeviceForAccount(account: String) {
        sessionManager.setAccountFingerprint(account, "")
    }

    var autoShowGoogleChooserOnWelcome = false
    var autoShowPhoneEntryOnWelcome = false

    fun triggerUpgradeDialog(show: Boolean) {
        _showUpgradeDialog.value = show
    }

    private val _isPinRequired = MutableStateFlow(false)
    val isPinRequired: StateFlow<Boolean> = _isPinRequired.asStateFlow()

    // Simulated Incoming Call State Flow
    private val _isIncomingCallActive = MutableStateFlow(false)
    val isIncomingCallActive: StateFlow<Boolean> = _isIncomingCallActive.asStateFlow()

    private val _incomingCallName = MutableStateFlow("Unknown")
    val incomingCallName: StateFlow<String> = _incomingCallName.asStateFlow()

    private val _incomingCallPhone = MutableStateFlow("")
    val incomingCallPhone: StateFlow<String> = _incomingCallPhone.asStateFlow()

    private val _incomingCallSimName = MutableStateFlow("SIM 1")
    val incomingCallSimName: StateFlow<String> = _incomingCallSimName.asStateFlow()

    fun triggerIncomingCall(name: String = "Unknown", phone: String = "", sim: String = "SIM 1") {
        val cleanPhone = phone.replace(Regex("[^+\\d]"), "")
        val isBlocked = blockedList.value.any { blockedId ->
            blockedId == phone || 
            blockedId.replace(Regex("[^+\\d]"), "") == cleanPhone
        }
        if (isBlocked) {
            android.util.Log.d("SimChatViewModel", "Incoming call from blocked number '$phone' was blocked/suppressed by carrier.")
            return
        }
        _incomingCallName.value = name
        _incomingCallPhone.value = phone
        _incomingCallSimName.value = sim
        _isIncomingCallActive.value = true
    }

    fun answerIncomingCall() {
        _isIncomingCallActive.value = false
    }

    fun declineIncomingCall() {
        _isIncomingCallActive.value = false
    }

    // --- INTEGRATED ADVANCED NOTIFICATION ROUTING SYSTEM ---
    private val _notifications = MutableStateFlow<List<SimulatedNotification>>(emptyList())
    val notifications: StateFlow<List<SimulatedNotification>> = _notifications.asStateFlow()

    private val _activeHeadsUp = MutableStateFlow<SimulatedNotification?>(null)
    val activeHeadsUp: StateFlow<SimulatedNotification?> = _activeHeadsUp.asStateFlow()

    init {
        checkSession()
        initializeSimulatedNotifications()
        viewModelScope.launch {
            repository.incomingFcmFlow.collect { fcmNotif ->
                addNotification(fcmNotif)
            }
        }
    }

    // Authenticate / Session management
    fun checkSession() {
        viewModelScope.launch {
            if (sessionManager.isPinLocked) {
                _isPinRequired.value = true
            }
            if (!sessionManager.isSetupCompleted) {
                _sessionState.value = SessionState.Welcome
            } else if (sessionManager.isGuest) {
                _sessionState.value = SessionState.GuestMode
            } else if (sessionManager.userId != null) {
                _sessionState.value = SessionState.CloudMode(
                    uid = sessionManager.userId ?: "",
                    email = sessionManager.userEmail ?: "",
                    name = sessionManager.userName,
                    bio = sessionManager.userBio,
                    photo = sessionManager.userPhoto
                )
            } else {
                _sessionState.value = SessionState.Welcome
            }
        }
    }

    fun enterPin(pin: String): Boolean {
        if (sessionManager.pinCode == pin) {
            _isPinRequired.value = false
            return true
        }
        return false
    }

    fun loginGuest() {
        sessionManager.isSetupCompleted = true
        sessionManager.isGuest = true
        sessionManager.userId = null
        sessionManager.userEmail = null
        checkSession()
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _sessionState.value = SessionState.OtpVerification(email, pass)
        }
    }

    fun loginGoogleUser(email: String, name: String) {
        viewModelScope.launch {
            val uid = "google_${email.hashCode()}"
            sessionManager.isSetupCompleted = true
            sessionManager.isGuest = false
            sessionManager.userId = uid
            sessionManager.userEmail = email
            sessionManager.userName = name
            checkSession()
        }
    }

    fun loginPhoneUser(phone: String) {
        viewModelScope.launch {
            val uid = "phone_${phone.hashCode()}"
            sessionManager.isSetupCompleted = true
            sessionManager.isGuest = false
            sessionManager.userId = uid
            sessionManager.userEmail = phone
            sessionManager.userName = phone
            checkSession()
        }
    }

    fun isDeviceVerificationNeeded(emailOrPhone: String): Boolean {
        val registeredFp = sessionManager.getAccountFingerprint(emailOrPhone)
        if (registeredFp != null && registeredFp != sessionManager.deviceFingerprint) {
            return true
        }
        // If there is no registered fingerprint, bind the current device as trusted on initial success
        if (registeredFp == null) {
            sessionManager.setAccountFingerprint(emailOrPhone, sessionManager.deviceFingerprint)
        }
        return false
    }

    fun registerWithPassword(emailOrPhone: String, isGoogle: Boolean, name: String, pass: String) {
        viewModelScope.launch {
            if (isGoogle) {
                sessionManager.registerGoogleAccount(emailOrPhone, name, pass)
                sessionManager.setAccountFingerprint(emailOrPhone, sessionManager.deviceFingerprint)
                loginGoogleUser(emailOrPhone, name)
            } else {
                sessionManager.registerPhoneAccount(emailOrPhone, pass)
                sessionManager.setAccountFingerprint(emailOrPhone, sessionManager.deviceFingerprint)
                loginPhoneUser(emailOrPhone)
            }
            // Trigger automatic FCM Registration!
            runRegisterFcmOnSignUp()
        }
    }

    fun checkPasswordResult(emailOrPhone: String, pass: String): CheckLoginResult {
        val isValidEmail = sessionManager.checkEmailPassword(emailOrPhone, pass)
        val isValidPhone = sessionManager.checkPhonePassword(emailOrPhone, pass)
        if (isValidEmail || isValidPhone) {
            if (isDeviceVerificationNeeded(emailOrPhone)) {
                return CheckLoginResult.VerificationRequired(emailOrPhone)
            }
            // Trust it and perform direct login
            if (isValidEmail) {
                val storedName = sessionManager.getRegisteredEmails()
                    .find { it == emailOrPhone }?.let { emailOrPhone.substringBefore("@") } ?: "Google User"
                loginGoogleUser(emailOrPhone, storedName.replaceFirstChar { it.uppercase() })
            } else {
                loginPhoneUser(emailOrPhone)
            }
            return CheckLoginResult.Success
        }
        return CheckLoginResult.IncorrectPassword
    }

    fun checkPasswordLogin(emailOrPhone: String, pass: String): Boolean {
        val res = checkPasswordResult(emailOrPhone, pass)
        return res is CheckLoginResult.Success
    }

    fun updateUserPassword(emailOrPhone: String, newPass: String) {
        sessionManager.updatePassword(emailOrPhone, newPass)
    }

    fun verifyOtp(email: String, code: String, pass: String) {
        viewModelScope.launch {
            val uid = "user_${System.currentTimeMillis()}"
            _sessionState.value = SessionState.OtpVerifiedNeedProfile(email, pass, uid)
        }
    }

    fun completeProfileSetup(uid: String, email: String, fullName: String, displayName: String, bio: String, photo: String) {
        sessionManager.isSetupCompleted = true
        sessionManager.isGuest = false
        sessionManager.userId = uid
        sessionManager.userEmail = email
        sessionManager.userName = displayName.ifEmpty { fullName.ifEmpty { email.substringBefore("@") } }
        sessionManager.userBio = bio
        sessionManager.userPhoto = photo
        runRegisterFcmOnSignUp()
        checkSession()
    }

    fun loginCloud(email: String, pass: String) {
        viewModelScope.launch {
            val uid = "user_${System.currentTimeMillis()}"
            sessionManager.isSetupCompleted = true
            sessionManager.isGuest = false
            sessionManager.userId = uid
            sessionManager.userEmail = email
            sessionManager.userName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            checkSession()
        }
    }

    fun logout() {
        sessionManager.logout()
        checkSession()
    }

    fun deleteAccount() {
        viewModelScope.launch {
            sessionManager.logout()
            _sessionState.value = SessionState.Welcome
        }
    }

    // Filtered conversations Flow
    val filteredConversations: StateFlow<List<ConversationEntity>> = combine(
        repository.conversations,
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        var result = list.filter { !it.isLocked }

        // Applying Filter
        result = when (filter) {
            "SMS" -> result.filter { it.type == "SMS" && !it.isGroup }
            "Cloud" -> result.filter { it.type == "CLOUD" && !it.isGroup }
            "Groups" -> result.filter { it.isGroup }
            "Unread" -> result.filter { it.unreadCount > 0 }
            "Starred" -> result.filter { it.isStarred }
            else -> result
        }

        // Applying Search Query
        if (query.isNotEmpty()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.lastMessage.contains(query, ignoreCase = true)
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lockedConversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .map { list -> list.filter { it.isLocked } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFilter(filter: String) {
        _selectedFilter.value = filter
    }

    // SIM Slots available
    fun getSimSlots(): List<SimSlotInfo> {
        return app.smsWrapper.getAvailableSimSlots()
    }

    fun getPreferredSim(): String {
        return sessionManager.preferredCarrierSim
    }

    fun setPreferredSim(sim: String) {
        sessionManager.preferredCarrierSim = sim
    }

    // Specific messages stream for detail chat view
    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> {
        // Clear unreads reactively when we open the chat screen
        viewModelScope.launch {
            repository.clearUnreads(convId)
        }
        return repository.getMessagesForConversation(convId)
    }

    fun sendMessage(convId: String, content: String, mode: String) {
        viewModelScope.launch {
            repository.sendMessage(convId, content, mode, mode)
        }
    }

    fun blockUser(id: String) {
        viewModelScope.launch {
            repository.blockContact(id)
        }
    }

    fun unblockUser(id: String) {
        viewModelScope.launch {
            repository.unblockContact(id)
        }
    }

    fun toggleStarConversation(convId: String, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStarConversation(convId, isStarred)
        }
    }

    fun toggleLockConversation(convId: String, isLocked: Boolean) {
        viewModelScope.launch {
            repository.toggleLockConversation(convId, isLocked)
        }
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            repository.deleteConversation(convId)
        }
    }

    fun createGroup(name: String, desc: String, avatar: String) {
        viewModelScope.launch {
            repository.createGroup(name, desc, avatar)
        }
    }

    fun createPremiumGroup(
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
    ) {
        viewModelScope.launch {
            repository.createPremiumGroup(
                name = name,
                desc = desc,
                avatar = avatar,
                category = category,
                groupType = groupType,
                memberList = memberList,
                hiddenMembers = hiddenMembers,
                adminOnlyMessage = adminOnlyMessage,
                adminOnlyAnnounce = adminOnlyAnnounce,
                requireApproval = requireApproval,
                isEncrypted = isEncrypted
            )
        }
    }

    fun getMembersForGroup(groupId: String): Flow<List<GroupMemberEntity>> =
        repository.getMembersForGroup(groupId)

    fun getPollsForGroup(groupId: String): Flow<List<GroupPollEntity>> =
        repository.getPollsForGroup(groupId)

    fun getEventsForGroup(groupId: String): Flow<List<GroupEventEntity>> =
        repository.getEventsForGroup(groupId)

    fun getNotificationsForGroup(groupId: String): Flow<List<GroupNotificationLogEntity>> =
        repository.getNotificationsForGroup(groupId)

    fun updateGroupPhoto(groupId: String, photo: String) {
        viewModelScope.launch {
            repository.updateGroupPhoto(groupId, photo)
        }
    }

    fun updateGroupName(groupId: String, name: String) {
        viewModelScope.launch {
            repository.updateGroupName(groupId, name)
        }
    }

    fun updateGroupDescription(groupId: String, desc: String) {
        viewModelScope.launch {
            repository.updateGroupDescription(groupId, desc)
        }
    }

    fun updateGroupPrivacySettings(
        groupId: String,
        groupType: String,
        hiddenMembers: Boolean,
        adminOnlyMessage: Boolean,
        adminOnlyAnnounce: Boolean,
        requireApproval: Boolean
    ) {
        viewModelScope.launch {
            repository.updateGroupPrivacySettings(
                groupId = groupId,
                groupType = groupType,
                hiddenMembers = hiddenMembers,
                adminOnlyMessage = adminOnlyMessage,
                adminOnlyAnnounce = adminOnlyAnnounce,
                requireApproval = requireApproval
            )
        }
    }

    fun addGroupMember(groupId: String, phone: String, name: String, role: String = "MEMBER") {
        viewModelScope.launch {
            repository.addGroupMember(groupId, phone, name, role)
        }
    }

    fun removeGroupMember(groupId: String, phone: String, name: String) {
        viewModelScope.launch {
            repository.removeGroupMember(groupId, phone, name)
        }
    }

    fun promoteMember(groupId: String, phone: String, name: String, role: String) {
        viewModelScope.launch {
            repository.promoteMember(groupId, phone, name, role)
        }
    }

    fun muteMember(groupId: String, phone: String, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteMember(groupId, phone, isMuted)
        }
    }

    fun banMember(groupId: String, phone: String, isBanned: Boolean) {
        viewModelScope.launch {
            repository.banMember(groupId, phone, isBanned)
        }
    }

    fun createGroupPoll(groupId: String, question: String, options: List<String>, isAnonymous: Boolean) {
        viewModelScope.launch {
            repository.createGroupPoll(groupId, question, options, isAnonymous)
        }
    }

    fun createGroupEvent(groupId: String, title: String, description: String, dateText: String) {
        viewModelScope.launch {
            repository.createGroupEvent(groupId, title, description, dateText)
        }
    }

    fun savePoll(poll: GroupPollEntity) {
        viewModelScope.launch {
            repository.savePoll(poll)
        }
    }

    fun saveEvent(event: GroupEventEntity) {
        viewModelScope.launch {
            repository.saveEvent(event)
        }
    }

    fun togglePinMessage(msgId: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinMessage(msgId, isPinned)
        }
    }

    fun addReaction(msgId: String, reaction: String) {
        viewModelScope.launch {
            repository.addReaction(msgId, reaction)
        }
    }

    fun runRegisterFcmOnSignUp() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    sessionManager.fcmToken = token
                    android.util.Log.d("SimChatViewModel", "FCM token registered successfully on sign up: $token")
                } else {
                    android.util.Log.w("SimChatViewModel", "FCM token registration failed during signup step", task.exception)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SimChatViewModel", "FirebaseMessaging SDK exception thrown during auto registration", e)
        }
    }

    fun leaveGroup(id: String) {
        viewModelScope.launch {
            repository.leaveGroup(id)
        }
    }

    fun addNewContact(name: String, phone: String, email: String, isCloud: Boolean) {
        viewModelScope.launch {
            repository.insertContact(ContactEntity(phone, name, phone, email, isCloud))
        }
    }

    fun importSms() {
        viewModelScope.launch {
            repository.importSmsMessages()
        }
    }

    // Settings Profile updates
    fun updateProfile(name: String, bio: String, photo: String) {
        sessionManager.userName = name
        sessionManager.userBio = bio
        sessionManager.userPhoto = photo
        checkSession()
    }

    // Passcode security toggle
    fun setPasscode(pin: String?) {
        if (pin == null) {
            sessionManager.isPinLocked = false
            sessionManager.pinCode = null
        } else {
            sessionManager.isPinLocked = true
            sessionManager.pinCode = pin
        }
    }

    fun getPasscodeState(): Boolean {
        return sessionManager.isPinLocked
    }

    var lockedChatsPasskey: String?
        get() = sessionManager.lockedChatsPasskey
        set(value) { sessionManager.lockedChatsPasskey = value }

    var isLockedChatsBiometricEnabled: Boolean
        get() = sessionManager.isLockedChatsBiometricEnabled
        set(value) { sessionManager.isLockedChatsBiometricEnabled = value }

    var lockedChatsBiometricType: String?
        get() = sessionManager.lockedChatsBiometricType
        set(value) { sessionManager.lockedChatsBiometricType = value }

    var lockedChatsPattern: String?
        get() = sessionManager.lockedChatsPattern
        set(value) { sessionManager.lockedChatsPattern = value }

    var isScreenshotProtectionEnabled: Boolean
        get() = sessionManager.isScreenshotProtectionEnabled
        set(value) { sessionManager.isScreenshotProtectionEnabled = value }

    fun resetLockedFolderAndClearChats() {
        viewModelScope.launch {
            val lockedList = lockedConversations.value
            lockedList.forEach { conv ->
                repository.deleteConversation(conv.id)
            }
            sessionManager.lockedChatsPasskey = null
            sessionManager.isLockedChatsBiometricEnabled = false
            sessionManager.lockedChatsBiometricType = "FINGERPRINT"
            sessionManager.lockedChatsPattern = null
            sessionManager.isScreenshotProtectionEnabled = false
        }
    }

    // Settings Toggle
    val activeTheme: String get() = sessionManager.theme
    fun updateTheme(theme: String) {
        sessionManager.theme = theme
    }

    val isNotifsEnabled: Boolean get() = sessionManager.isNotificationsEnabled
    fun toggleNotifs(enabled: Boolean) {
        sessionManager.isNotificationsEnabled = enabled
    }

    var preferredCarrierSim: String
        get() = sessionManager.preferredCarrierSim
        set(value) {
            sessionManager.preferredCarrierSim = value
        }

    fun wipeDatabase() {
        viewModelScope.launch {
            repository.wipeDatabase()
        }
    }

    fun deleteAllConversations() {
        viewModelScope.launch {
            repository.deleteAllConversations()
        }
    }

    fun deleteMessage(msgId: String) {
        viewModelScope.launch {
            repository.deleteMessage(msgId)
        }
    }

    fun toggleStarMessage(msgId: String, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStarMessage(msgId, isStarred)
        }
    }

    fun forwardMessage(convId: String, content: String, mode: String) {
        viewModelScope.launch {
            repository.forwardMessage(convId, content, mode)
        }
    }

    fun syncDeviceSms() {
        viewModelScope.launch {
            repository.syncRealDeviceSms()
        }
    }

    fun syncDeviceContacts() {
        viewModelScope.launch {
            repository.syncRealDeviceContacts()
        }
    }

    fun loadDeviceCallLogs() {
        viewModelScope.launch {
            _callLogs.value = repository.getRealDeviceCallLogs()
        }
    }

    fun initializeSimulatedNotifications() {
        val list = listOf(
            SimulatedNotification(
                id = "notif_mpesa",
                category = "Transaction Messages",
                title = "💸 Payment Received",
                senderName = "M-PESA",
                messageText = "Payment Received! You have received Ksh 1,250.00 from JOHN MWANGI. Balance: Ksh 3,450.75. Transaction ID: QMPA7X12K3",
                timestamp = "9:41 AM",
                carrierSim = "SIM 1 • Safaricom",
                isUnread = true,
                transactionAmount = "Ksh 1,250.00",
                transactionId = "QMPA7X12K3",
                balance = "Ksh 3,450.75"
            ),
            SimulatedNotification(
                id = "notif_otp",
                category = "OTP Messages",
                title = "🔑 One-Time Passcode",
                senderName = "Google Security",
                messageText = "Your SimChat one-time security login authorization passcode is 492083. Do not disclose this code.",
                timestamp = "Just Now",
                carrierSim = "SIM 1",
                isUnread = true,
                otpCode = "492083",
                verificationCode = "492083"
            ),
            SimulatedNotification(
                id = "notif_incoming",
                category = "Incoming Calls",
                title = "📞 Incoming Call",
                senderName = "Jane Doe",
                messageText = "Jane Doe is calling you from Airtel SIM carrier slot 2...",
                timestamp = "Active",
                carrierSim = "Airtel SIM 2",
                isUnread = true
            ),
            SimulatedNotification(
                id = "notif_missed",
                category = "Missed Calls",
                title = "📞 Missed Call",
                senderName = "Jane Doe",
                messageText = "Jane Doe missed call log. Call was placed on 9:41 AM on Carrier SIM 1.",
                timestamp = "9:41 AM",
                carrierSim = "SIM 1 • Safaricom",
                isUnread = true
            ),
            SimulatedNotification(
                id = "notif_spam",
                category = "Spam Alerts",
                title = "⚠️ High Spam Risk",
                senderName = "Blocked Sender",
                messageText = "URGENT WINNER: Claim your free 1M USD prize now at http://scam.example.com/winner !!",
                timestamp = "Just Now",
                carrierSim = "SIM 1",
                isUnread = true,
                spamRiskScore = 95
            ),
            SimulatedNotification(
                id = "notif_delivery",
                category = "Delivery Messages",
                title = "📦 Package Delivery Update",
                senderName = "DHL Express",
                messageText = "Your package DHL-55928-XY is out for delivery! Current status: In Transit to Destination Hub.",
                timestamp = "9:41 AM",
                carrierSim = "SIM 2",
                isUnread = true,
                trackingNumber = "DHL-55928-XY",
                deliveryStatus = "In Transit to Destination Hub"
            ),
            SimulatedNotification(
                id = "notif_mention",
                category = "Mention Notifications",
                title = "📣 You were Mentioned",
                senderName = "Supervisor Team",
                messageText = "@ronoh Look at this brand new UI on our SimChat development group chat!",
                timestamp = "Just Now",
                carrierSim = "CLOUD",
                isUnread = true,
                mentionUser = "@ronoh",
                groupName = "SimChat Dev Group"
            ),
            SimulatedNotification(
                id = "notif_business",
                category = "Business Messages",
                title = "🏢 Business Official Channel",
                senderName = "Google Workspace",
                messageText = "Google Workspace: Your verification code is 109280. Google Security.",
                timestamp = "Just Now",
                carrierSim = "SIM 1",
                isUnread = true,
                businessName = "Google Workspace"
            ),
            SimulatedNotification(
                id = "notif_personal",
                category = "Personal Messages",
                title = "💬 Personal Conversation",
                senderName = "John Mwangi",
                messageText = "Hey, are you still up for coffee later this afternoon?",
                timestamp = "Just Now",
                carrierSim = "SIM 1",
                isUnread = true
            ),
            SimulatedNotification(
                id = "notif_cloud",
                category = "Cloud Messages",
                title = "☁️ Instant Cloud Chat",
                senderName = "Alex Rivers",
                messageText = "[Cloud Chat] Design review complete! The new animations feel extremely pristine and responsive.",
                timestamp = "9:41 AM",
                carrierSim = "CLOUD",
                isUnread = true
            ),
            SimulatedNotification(
                id = "notif_sms",
                category = "SMS Messages",
                title = "✉️ SMS Carrier Dispatch",
                senderName = "Safaricom",
                messageText = "Please verify your carrier SIM registration card soon. Dial *100# for more options.",
                timestamp = "Just Now",
                carrierSim = "SIM 1",
                isUnread = true
            ),
            SimulatedNotification(
                id = "notif_verify",
                category = "Verification Messages",
                title = "🛡️ Account Security Verification",
                senderName = "Security System",
                messageText = "Verify your email address by tapping this URL: https://simchat.example.com/verify/a02f92",
                timestamp = "Just Now",
                carrierSim = "SIM 1",
                isUnread = true,
                url = "https://simchat.example.com/verify/a02f92"
            ),
            SimulatedNotification(
                id = "notif_promo",
                category = "Promotional Messages",
                title = "🎁 Special Offer Saved",
                senderName = "SimChat Promo",
                messageText = "Get 50% discount on SimChat Premium! Offer valid for the next 24 hours. Tap to redeem!",
                timestamp = "Just Now",
                carrierSim = "SIM 1",
                isUnread = true
            ),
            SimulatedNotification(
                id = "notif_group",
                category = "Group Messages",
                title = "💬 Group Discussion",
                senderName = "Dev Board",
                messageText = "[Group Chat] SimChat dev team: let's launch the next-generation updates tomorrow.",
                timestamp = "Just Now",
                carrierSim = "CLOUD",
                isUnread = true,
                groupName = "SimChat Dev Group"
            ),
            SimulatedNotification(
                id = "notif_system",
                category = "System Alerts",
                title = "⚙️ System Configuration Service",
                senderName = "SimChat System",
                messageText = "System security update is available. Download to protect your offline database.",
                timestamp = "System",
                carrierSim = "Local Update",
                isUnread = true
            )
        )
        _notifications.value = list
    }

    fun triggerHeadsUpNotification(notif: SimulatedNotification) {
        _activeHeadsUp.value = notif
    }

    fun dismissHeadsUpNotification() {
        _activeHeadsUp.value = null
    }

    fun addNotification(notif: SimulatedNotification) {
        val currentList = _notifications.value.toMutableList()
        currentList.add(0, notif)
        _notifications.value = currentList
        triggerHeadsUpNotification(notif)

        // For simcard notifications, route them using Android standard Notification API
        if (notif.carrierSim != "CLOUD" && notif.carrierSim != "Local Update") {
            try {
                repository.handleSimulatedNotificationPush(notif.senderName, notif.messageText)
            } catch (e: Exception) {
                // Fallback gracefully if Context and UI scheduler are decoupled
            }
        }
    }

    fun markNotificationRead(id: String, isRead: Boolean) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isUnread = !isRead) else it
        }
    }

    fun replyNotificationInBackground(id: String, replyText: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) {
                it.copy(
                    isUnread = false,
                    backgroundReplyText = replyText,
                    messageText = "${it.messageText}\n[You replied in background]: $replyText"
                )
            } else it
        }
    }

    fun blockSenderNotification(senderName: String) {
        viewModelScope.launch {
            repository.blockContact(senderName)
        }
    }

    companion object {
        fun Factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SimChatViewModel(application) as T
            }
        }
    }
}

data class SimulatedNotification(
    val id: String,
    val category: String,
    val title: String,
    val senderName: String,
    val messageText: String,
    val timestamp: String,
    val carrierSim: String,
    val isUnread: Boolean = true,
    val otpCode: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val url: String? = null,
    val transactionAmount: String? = null,
    val transactionId: String? = null,
    val balance: String? = null,
    val trackingNumber: String? = null,
    val deliveryStatus: String? = null,
    val spamRiskScore: Int = 0,
    val businessName: String? = null,
    val verificationCode: String? = null,
    val groupName: String? = null,
    val mentionUser: String? = null,
    val backgroundReplyText: String? = null
)

sealed interface SessionState {
    object Splash : SessionState
    object Welcome : SessionState
    object GuestMode : SessionState
    data class OtpVerification(val email: String, val pass: String) : SessionState
    data class OtpVerifiedNeedProfile(val email: String, val pass: String, val uid: String) : SessionState
    data class CloudMode(
        val uid: String,
        val email: String,
        val name: String,
        val bio: String,
        val photo: String
    ) : SessionState
}

sealed interface CheckLoginResult {
    object Success : CheckLoginResult
    object IncorrectPassword : CheckLoginResult
    data class VerificationRequired(val account: String) : CheckLoginResult
}
