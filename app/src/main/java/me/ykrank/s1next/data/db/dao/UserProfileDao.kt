package me.ykrank.s1next.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.ykrank.s1next.data.db.dbmodel.UserProfile

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM UserProfile WHERE Uid = :uid LIMIT 1")
    fun getByUid(uid: String): UserProfile?

    @Query("SELECT * FROM UserProfile WHERE Uid IN (:uids)")
    fun getByUids(uids: List<String>): List<UserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userProfile: UserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userProfiles: List<UserProfile>)

    @Query("DELETE FROM UserProfile WHERE Uid = :uid")
    fun deleteByUid(uid: String)
}
