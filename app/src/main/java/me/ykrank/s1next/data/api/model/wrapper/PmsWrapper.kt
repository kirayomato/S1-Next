package me.ykrank.s1next.data.api.model.wrapper

import android.text.TextUtils
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.ykrank.androidtools.util.LooperUtil
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.collection.Pms

@JsonIgnoreProperties(ignoreUnknown = true)
class PmsWrapper : BaseDataWrapper<Pms>() {

    /**
     * 完善每条私信的收信人
     *
     * @param me 自己
     * @param toUsername 对方用户名
     */
    fun setMsgToUsername(me: User, toUsername: String?): PmsWrapper {
        LooperUtil.enforceOnWorkThread()
        val pmList = data!!.list
        if (pmList.isNullOrEmpty()) {
            return this
        }
        for (pm in pmList) {
            pm.msgTo = if (TextUtils.equals(pm.msgToId, me.uid)) {
                me.name
            } else {
                toUsername
            }
        }
        return this
    }
}
