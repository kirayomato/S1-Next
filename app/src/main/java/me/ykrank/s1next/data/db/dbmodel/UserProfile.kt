package me.ykrank.s1next.data.db.dbmodel

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "UserProfile",
    indices = [
        Index(value = ["Uid"], name = "IDX_UserProfile_Uid", unique = true),
    ]
)
class UserProfile {
    @PrimaryKey
    @ColumnInfo(name = "Uid")
    var uid: String = ""

    @ColumnInfo(name = "Username")
    var username: String? = null

    @ColumnInfo(name = "HomeUsername")
    var homeUsername: String? = null

    @ColumnInfo(name = "SignHtml")
    var signHtml: String? = null

    @ColumnInfo(name = "Friends")
    var friends: Int? = null

    @ColumnInfo(name = "Replies")
    var replies: Int? = null

    @ColumnInfo(name = "Threads")
    var threads: Int? = null

    @ColumnInfo(name = "GroupTitle")
    var groupTitle: String? = null

    @ColumnInfo(name = "OnlineHour")
    var onlineHour: Int? = null

    @ColumnInfo(name = "RegDate")
    var regDate: Long? = null

    @ColumnInfo(name = "LastVisitDate")
    var lastVisitDate: Long? = null

    @ColumnInfo(name = "LastActiveDate")
    var lastActiveDate: Long? = null

    @ColumnInfo(name = "LastPostDate")
    var lastPostDate: Long? = null

    @ColumnInfo(name = "Goose")
    var goose: Int? = null

    @ColumnInfo(name = "StatsJson")
    var statsJson: String? = null

    @ColumnInfo(name = "ManagerJson")
    var managerJson: String? = null

    @ColumnInfo(name = "UpdatedAt")
    var updatedAt: Long = 0

    @ColumnInfo(name = "LastRequestAt")
    var lastRequestAt: Long = 0

    constructor()

    constructor(
        uid: String,
        username: String?,
        homeUsername: String?,
        signHtml: String?,
        friends: Int?,
        replies: Int?,
        threads: Int?,
        groupTitle: String?,
        onlineHour: Int?,
        regDate: Long?,
        lastVisitDate: Long?,
        lastActiveDate: Long?,
        lastPostDate: Long?,
        goose: Int?,
        statsJson: String?,
        managerJson: String?,
        updatedAt: Long,
        lastRequestAt: Long,
    ) {
        this.uid = uid
        this.username = username
        this.homeUsername = homeUsername
        this.signHtml = signHtml
        this.friends = friends
        this.replies = replies
        this.threads = threads
        this.groupTitle = groupTitle
        this.onlineHour = onlineHour
        this.regDate = regDate
        this.lastVisitDate = lastVisitDate
        this.lastActiveDate = lastActiveDate
        this.lastPostDate = lastPostDate
        this.goose = goose
        this.statsJson = statsJson
        this.managerJson = managerJson
        this.updatedAt = updatedAt
        this.lastRequestAt = lastRequestAt
    }
}
