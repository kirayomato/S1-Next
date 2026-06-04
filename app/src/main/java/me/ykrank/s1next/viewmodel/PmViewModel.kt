package me.ykrank.s1next.viewmodel

import android.view.View
import androidx.lifecycle.LifecycleOwner
import me.ykrank.s1next.data.api.model.Pm
import me.ykrank.s1next.view.activity.UserHomeActivity


class PmViewModel(val lifecycleOwner: LifecycleOwner) {

    var pm: Pm? = null

    fun onAvatarClick(v: View) {
        val uid = pm?.authorId
        val name = pm?.author
        if (uid != null) {
            UserHomeActivity.start(v.context, uid, name, v)
        }
    }
}
