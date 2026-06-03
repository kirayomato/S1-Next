package me.ykrank.s1next.view.dialog.requestdialog

import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.view.dialog.ProgressDialogFragment
import me.ykrank.s1next.view.event.RequestDialogSuccessEvent
import javax.inject.Inject

/**
 * Dialog to send request.
 * Do before dialog dismiss
 */
abstract class BaseRequestDialogFragment<D> : ProgressDialogFragment<D>() {

    @Inject
    internal lateinit var eventBus: EventBus

    protected fun onRequestSuccess(msg: String?) {
        eventBus.postDefault(RequestDialogSuccessEvent(this, msg))
    }

    protected fun onRequestError(msg: String?) {
        if (msg != null) {
            showToastText(msg)
        }
    }
}
