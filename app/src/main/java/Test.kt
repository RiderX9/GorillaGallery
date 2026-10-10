import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import androidx.compose.ui.Modifier

@Composable
fun test() {
    val context = LocalContext.current
    val req = ImageRequest.Builder(context)
        .data("uri")
        .setParameter("signature", "123")
        .memoryCacheKey("123")
        .diskCacheKey("123")
        .build()
        
    ZoomableAsyncImage(
        model = req,
        contentDescription = null,
        modifier = Modifier
    )
}
