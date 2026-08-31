package com.example.data.repository

import com.example.data.db.AttendanceDao
import com.example.data.db.populateInitialData
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.GroupEntity
import com.example.data.model.MemberEntity
import com.example.data.model.MemberStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val dao: AttendanceDao) {

    val allGroups: Flow<List<GroupEntity>> = dao.getAllGroups()
    val allUsers: Flow<List<UserEntity>> = dao.getAllUsers()
    val allMembers: Flow<List<MemberEntity>> = dao.getAllMembers()
    val allAttendance: Flow<List<AttendanceRecordEntity>> = dao.getAllAttendance()
    val allEquipment: Flow<List<com.example.data.model.EquipmentEntity>> = dao.getAllEquipment()
    val allDailyUpdates: Flow<List<com.example.data.model.DailyUpdateEntity>> = dao.getAllDailyUpdates()
    val allExecutiveContacts: Flow<List<com.example.data.model.ExecutiveContactEntity>> = dao.getAllExecutiveContacts()
    val allNoticeReceipts: Flow<List<com.example.data.model.NoticeReceiptEntity>> = dao.getAllNoticeReceipts()

    fun getReceiptsForNotice(noticeId: Long): Flow<List<com.example.data.model.NoticeReceiptEntity>> =
        dao.getReceiptsForNotice(noticeId)

    fun getMembersByGroup(groupId: Long): Flow<List<MemberEntity>> = dao.getMembersByGroup(groupId)

    fun getEquipmentByGroup(groupId: Long): Flow<List<com.example.data.model.EquipmentEntity>> =
        dao.getEquipmentByGroup(groupId)

    fun getDailyUpdatesForGroup(groupId: Long): Flow<List<com.example.data.model.DailyUpdateEntity>> =
        dao.getDailyUpdatesForGroup(groupId)

    fun getAttendanceByGroupAndDate(groupId: Long, date: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceByGroupAndDate(groupId, date)

    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceByDate(date)

    fun getAttendanceForGroup(groupId: Long): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForGroup(groupId)

    suspend fun ensureInitialized() {
        if (dao.getGroupCount() == 0 || dao.getUserCount() == 0) {
            populateInitialData(dao)
        }
    }

    suspend fun authenticate(loginName: String, passwordAttempt: String): UserEntity? {
        val trimmedName = loginName.trim()
        val trimmedPass = passwordAttempt.trim()

        ensureInitialized()

        var user = dao.getUserByLoginName(trimmedName)

        // Fallback checks for admin / default roles if needed
        if (user == null) {
            if (trimmedName.equals("admin", ignoreCase = true) || trimmedName.equals("باش باشقۇرغۇچى", ignoreCase = true)) {
                user = dao.getUserByLoginName("admin")
            }
        }

        if (user == null) {
            // Check all users in db for match
            val allList = dao.getAllUsersList()
            user = allList.find {
                it.loginName.trim().equals(trimmedName, ignoreCase = true) ||
                it.displayName.trim().equals(trimmedName, ignoreCase = true)
            }
        }

        if (user == null) return null

        val storedHash = user.passwordHash.trim()
        val attempt = trimmedPass

        // If user has no password set (empty string), allow direct login
        if (storedHash.isEmpty()) {
            return user
        }

        return if (storedHash == attempt) user else null
    }

    suspend fun removeUserPassword(userId: Long) {
        dao.updatePassword(userId, "")
    }

    suspend fun saveAttendanceRecord(
        memberId: Long,
        groupId: Long,
        date: String,
        status: AttendanceStatus,
        note: String = ""
    ) {
        dao.insertOrUpdateAttendance(
            AttendanceRecordEntity(
                memberId = memberId,
                groupId = groupId,
                date = date,
                status = status,
                note = note,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun markAllPresent(groupId: Long, date: String, members: List<MemberEntity>) {
        val records = members.filter { it.status == MemberStatus.ACTIVE }.map { member ->
            AttendanceRecordEntity(
                memberId = member.id,
                groupId = groupId,
                date = date,
                status = AttendanceStatus.PRESENT,
                note = "",
                timestamp = System.currentTimeMillis()
            )
        }
        dao.insertAttendanceBatch(records)
    }

    suspend fun addMember(
        name: String,
        groupId: Long,
        subGroup: Int = 1,
        contactAddress: String = "",
        telegramContact: String = "",
        whatsappContact: String = "",
        otherContact: String = "",
        joinDate: String = "",
        status: MemberStatus = MemberStatus.ACTIVE
    ): Long {
        return dao.insertMember(
            MemberEntity(
                name = name.trim(),
                groupId = groupId,
                subGroup = subGroup,
                contactAddress = contactAddress.trim(),
                telegramContact = telegramContact.trim(),
                whatsappContact = whatsappContact.trim(),
                otherContact = otherContact.trim(),
                joinDate = joinDate,
                status = status
            )
        )
    }

    suspend fun addMembersBatch(members: List<MemberEntity>) {
        dao.insertMembers(members)
    }

    suspend fun updateMember(member: MemberEntity) {
        dao.updateMember(member)
    }

    suspend fun deleteMember(member: MemberEntity) {
        dao.deleteMember(member)
    }

    suspend fun resetUserPassword(userId: Long, newPasswordPlain: String) {
        dao.updatePassword(userId, newPasswordPlain.trim())
    }

    suspend fun updateUserCredentials(userId: Long, loginName: String, passwordPlain: String, displayName: String) {
        dao.updateUserCredentials(userId, loginName.trim(), passwordPlain.trim(), displayName.trim())
    }

    suspend fun updateGroup(group: GroupEntity) {
        dao.updateGroup(group)
    }

    suspend fun addGroup(
        name: String,
        code: String,
        description: String = "",
        subLeader1: String = "",
        subLeader1Contact: String = "",
        subLeader1Telegram: String = "",
        subLeader1Whatsapp: String = "",
        subLeader2: String = "",
        subLeader2Contact: String = "",
        subLeader2Telegram: String = "",
        subLeader2Whatsapp: String = "",
        leaderLoginName: String = "",
        leaderPasswordPlain: String = "123456"
    ): Long {
        val groupId = dao.insertGroup(
            GroupEntity(
                name = name.trim(),
                code = code.trim(),
                description = description.trim(),
                subLeader1 = subLeader1.trim(),
                subLeader1Contact = subLeader1Contact.trim(),
                subLeader1Telegram = subLeader1Telegram.trim(),
                subLeader1Whatsapp = subLeader1Whatsapp.trim(),
                subLeader2 = subLeader2.trim(),
                subLeader2Contact = subLeader2Contact.trim(),
                subLeader2Telegram = subLeader2Telegram.trim(),
                subLeader2Whatsapp = subLeader2Whatsapp.trim(),
                isSuspended = false,
                lastActiveTime = System.currentTimeMillis()
            )
        )
        val loginName = if (leaderLoginName.isNotBlank()) leaderLoginName.trim() else "lead$groupId"
        dao.insertUser(
            UserEntity(
                groupId = groupId,
                loginName = loginName,
                passwordHash = if (leaderPasswordPlain.isNotBlank()) leaderPasswordPlain.trim() else "123456",
                displayName = "${name.trim()} مەسئۇلى",
                role = UserRole.GROUP_LEAD
            )
        )
        return groupId
    }

    suspend fun deleteGroup(groupId: Long) {
        dao.deleteUserByGroupId(groupId)
        dao.deleteGroupById(groupId)
    }

    suspend fun setGroupSuspended(groupId: Long, isSuspended: Boolean) {
        dao.updateGroupSuspension(groupId, isSuspended)
    }

    suspend fun recordGroupActive(groupId: Long) {
        dao.updateGroupLastActive(groupId, System.currentTimeMillis())
    }

    // --- Equipment Operations ---
    suspend fun addEquipment(
        groupId: Long,
        name: String,
        totalCount: Int,
        readyCount: Int,
        notReadyCount: Int,
        notes: String = ""
    ): Long {
        return dao.insertEquipment(
            com.example.data.model.EquipmentEntity(
                groupId = groupId,
                name = name.trim(),
                totalCount = totalCount,
                readyCount = readyCount,
                notReadyCount = notReadyCount,
                notes = notes.trim()
            )
        )
    }

    suspend fun updateEquipment(equipment: com.example.data.model.EquipmentEntity) {
        dao.updateEquipment(equipment)
    }

    suspend fun deleteEquipment(equipment: com.example.data.model.EquipmentEntity) {
        dao.deleteEquipment(equipment)
    }

    suspend fun deleteEquipmentById(id: Long) {
        dao.deleteEquipmentById(id)
    }

    suspend fun updateGroupSubLeaders(
        groupId: Long,
        leader1: String,
        contact1: String = "",
        tele1: String = "",
        wa1: String = "",
        other1: String = "",
        leader2: String,
        contact2: String = "",
        tele2: String = "",
        wa2: String = "",
        other2: String = ""
    ) {
        dao.updateGroupSubLeaders(
            groupId = groupId,
            leader1 = leader1.trim(),
            contact1 = contact1.trim(),
            tele1 = tele1.trim(),
            wa1 = wa1.trim(),
            other1 = other1.trim(),
            leader2 = leader2.trim(),
            contact2 = contact2.trim(),
            tele2 = tele2.trim(),
            wa2 = wa2.trim(),
            other2 = other2.trim()
        )
    }

    // --- Daily Updates Operations ---
    suspend fun addDailyUpdate(
        groupId: Long,
        groupName: String,
        authorName: String,
        title: String,
        content: String,
        date: String,
        priority: String = "NORMAL"
    ): Long {
        return dao.insertDailyUpdate(
            com.example.data.model.DailyUpdateEntity(
                groupId = groupId,
                groupName = groupName,
                authorName = authorName.trim(),
                title = title.trim(),
                content = content.trim(),
                date = date,
                priority = priority,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateDailyUpdate(update: com.example.data.model.DailyUpdateEntity) {
        dao.updateDailyUpdate(update)
    }

    suspend fun deleteDailyUpdate(update: com.example.data.model.DailyUpdateEntity) {
        dao.deleteDailyUpdate(update)
    }

    suspend fun deleteDailyUpdateById(id: Long) {
        dao.deleteReceiptsForNotice(id)
        dao.deleteDailyUpdateById(id)
    }

    // --- Notice Tracking & Receipts ---
    suspend fun markNoticeDelivered(noticeId: Long, groupId: Long) {
        val existing = dao.getReceipt(noticeId, groupId)
        if (existing == null) {
            dao.insertOrUpdateReceipt(
                com.example.data.model.NoticeReceiptEntity(
                    noticeId = noticeId,
                    groupId = groupId,
                    isDelivered = true,
                    deliveredTimestamp = System.currentTimeMillis(),
                    isAcknowledged = false,
                    acknowledgedTimestamp = 0L
                )
            )
        }
    }

    suspend fun acknowledgeNotice(noticeId: Long, groupId: Long) {
        val existing = dao.getReceipt(noticeId, groupId)
        val now = System.currentTimeMillis()
        if (existing != null) {
            dao.insertOrUpdateReceipt(
                existing.copy(
                    isDelivered = true,
                    isAcknowledged = true,
                    acknowledgedTimestamp = now
                )
            )
        } else {
            dao.insertOrUpdateReceipt(
                com.example.data.model.NoticeReceiptEntity(
                    noticeId = noticeId,
                    groupId = groupId,
                    isDelivered = true,
                    deliveredTimestamp = now,
                    isAcknowledged = true,
                    acknowledgedTimestamp = now
                )
            )
        }
    }

    // --- Executive Contacts Operations ---
    suspend fun addExecutiveContact(
        name: String,
        title: String,
        phone: String = "",
        telegram: String = "",
        whatsapp: String = "",
        radioComms: String = "",
        other: String = "",
        notes: String = ""
    ): Long {
        return dao.insertExecutiveContact(
            com.example.data.model.ExecutiveContactEntity(
                name = name.trim(),
                title = title.trim(),
                phone = phone.trim(),
                telegram = telegram.trim(),
                whatsapp = whatsapp.trim(),
                radioComms = radioComms.trim(),
                other = other.trim(),
                notes = notes.trim()
            )
        )
    }

    suspend fun updateExecutiveContact(contact: com.example.data.model.ExecutiveContactEntity) {
        dao.updateExecutiveContact(contact)
    }

    suspend fun deleteExecutiveContact(contact: com.example.data.model.ExecutiveContactEntity) {
        dao.deleteExecutiveContact(contact)
    }

    suspend fun deleteExecutiveContactById(id: Long) {
        dao.deleteExecutiveContactById(id)
    }

    suspend fun resetDatabase() {
        dao.clearAllAttendance()
        dao.clearAllMembers()
        dao.clearAllEquipment()
        dao.clearAllUsers()
        dao.clearAllGroups()
        populateInitialData(dao)
    }
}
