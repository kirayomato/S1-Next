package me.ykrank.s1next.data.api.model.collection

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.common.base.Objects
import me.ykrank.s1next.data.api.model.Account
import me.ykrank.s1next.data.api.model.Friend

class Friends : Account() {
    @JsonProperty("list")
    var friendList: List<Friend>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Friends) return false
        if (!super.equals(other)) return false
        return Objects.equal(friendList, other.friendList)
    }

    override fun hashCode(): Int {
        return Objects.hashCode(super.hashCode(), friendList)
    }
}
