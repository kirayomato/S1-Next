package me.ykrank.s1next.view.page.post.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import me.ykrank.s1next.R
import me.ykrank.s1next.view.activity.BaseActivity

class PostSharePreviewActivity : BaseActivity() {

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_share_preview)
        title = getString(R.string.post_share_preview_title)
        imageUri = readImageUri()
        val uri = imageUri
        if (uri == null) {
            showSnackbar(R.string.post_share_image_failed)
            finish()
            return
        }
        findViewById<ImageView>(R.id.post_share_preview_image).apply {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            setImageURI(uri)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_post_share_preview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share -> {
                shareImage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun readImageUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(ARG_IMAGE_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(ARG_IMAGE_URI)
        }
    }

    private fun shareImage() {
        val uri = imageUri ?: return
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/png"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.clipData = ClipData.newUri(contentResolver, "Post share image", uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, getString(R.string.menu_title_share)))
    }

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"

        fun start(context: Context, imageUri: Uri) {
            val intent = Intent(context, PostSharePreviewActivity::class.java)
            intent.putExtra(ARG_IMAGE_URI, imageUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }
    }
}
