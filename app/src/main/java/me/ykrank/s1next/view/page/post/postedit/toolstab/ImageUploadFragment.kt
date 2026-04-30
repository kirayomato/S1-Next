package me.ykrank.s1next.view.page.post.postedit.toolstab

import android.os.Bundle
import android.view.View
import com.github.ykrank.androidautodispose.AndroidRxDispose
import com.github.ykrank.androidlifecycle.event.FragmentEvent
import com.github.ykrank.androidtools.extension.toast
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.LooperUtil
import com.github.ykrank.androidtools.util.RxJavaUtil
import com.github.ykrank.androidtools.widget.EventBus
import com.github.ykrank.androidtools.widget.uploadimg.ImageUploadManager
import com.github.ykrank.androidtools.widget.uploadimg.ImageUploadError
import com.github.ykrank.androidtools.widget.uploadimg.LibImageUploadFragment
import com.github.ykrank.androidtools.widget.uploadimg.ModelImageUpload
import me.ykrank.s1next.App
import me.ykrank.s1next.data.User
import me.ykrank.s1next.widget.net.Image
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.view.event.PostAddImageEvent
import me.ykrank.s1next.view.page.post.postedit.BasePostEditFragment
import me.ykrank.s1next.widget.uploadimg.ForumImageUploadManager
import okhttp3.OkHttpClient
import javax.inject.Inject

class ImageUploadFragment : LibImageUploadFragment() {

    @Inject
    internal lateinit var mEventBus: EventBus

    @Inject
    @Image
    internal lateinit var mOkHttpClient: OkHttpClient

    @Inject
    internal lateinit var mUser: User

    @Inject
    internal lateinit var mS1Service: S1Service

    private var uploadManager: ForumImageUploadManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.appComponent.inject(this)
    }

    override val imageClickListener: (View, ModelImageUpload) -> Unit =
            { view, model -> model.url?.also { mEventBus.postDefault(PostAddImageEvent(it)) } }

    override fun provideImageUploadManager(): ImageUploadManager {
        val fid = (parentFragment as? BasePostEditFragment)?.getForumId() ?: 0
        val tid = (parentFragment as? BasePostEditFragment)?.getThreadId()
        val pid = (parentFragment as? BasePostEditFragment)?.getPostId()

        return uploadManager ?: ForumImageUploadManager(
            _okHttpClient = mOkHttpClient,
            user = mUser,
            fid = fid,
            s1Service = mS1Service,
        ).also {
            it.setTid(tid)
            it.setPid(pid)
            uploadManager = it
        }
    }

    override fun delPickedImage(model: ModelImageUpload?) {
        if (model != null) {
            // 如果没有 aid，直接移除（图片可能未上传成功）
            if (model.aid.isNullOrEmpty()) {
                removeUploadedImage(model)
                return
            }
            
            (imageUploadManager as? ForumImageUploadManager)?.apply {
                // 设置tid和pid（可能从model中获取）
                if (model.tid != null) setTid(model.tid)
                if (model.pid != null) setPid(model.pid)
            }
            imageUploadManager.delUploadedImage(model)
                .compose(RxJavaUtil.iOSingleTransformer())
                .doAfterTerminate {
                    LooperUtil.workInMainThread {
                        removeUploadedImage(model)
                    }
                }
                .to(AndroidRxDispose.withSingle(this, FragmentEvent.DESTROY))
                .subscribe({
                    context?.toast(it.msg)
                    if (!it.success) {
                        L.report(ImageUploadError("Delete image error: $model, $it"))
                    }
                }, L::report)
        }
    }

    companion object {
        val TAG: String = ImageUploadFragment::class.java.simpleName

        fun newInstance(): ImageUploadFragment {
            return ImageUploadFragment()
        }
    }
}