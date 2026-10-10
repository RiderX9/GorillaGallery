import coil.imageLoader
import coil.request.ImageRequest
import android.content.Context
import android.net.Uri

suspend fun preload2(context: Context, uri: Uri, modified: Long, trigger: Int) {
    val req = ImageRequest.Builder(context)
        .data(uri)
        .setParameter("date_modified", modified)
        .memoryCacheKey("${uri}_${modified}_${trigger}")
        .diskCacheKey("${uri}_${modified}_${trigger}")
        .build()
    context.imageLoader.execute(req)
}
