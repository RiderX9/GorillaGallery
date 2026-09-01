package com.gorilla.gallery.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FaceEmbeddingExtractor(context: Context) {

    private val interpreter: Interpreter
    private val inputSize = INPUT_SIZE
    private val imgData: ByteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
    private val intValues = IntArray(inputSize * inputSize)

    init {
        imgData.order(ByteOrder.nativeOrder())
        val model = loadModelFile(context)
        interpreter = Interpreter(model)
    }

    /**
     * Given the full image bitmap and a list of detected faces, crop and align each face
     * then produce a 192-dim embedding. Returns a list of (faceIndex, embedding) pairs.
     */
    fun extractAll(bitmap: Bitmap, faces: List<Face>): List<Pair<Int, FloatArray>> {
        return faces.mapIndexedNotNull { index, face ->
            val faceBitmap = cropAndAlign(bitmap, face) ?: return@mapIndexedNotNull null
            val embedding = runModel(faceBitmap)
            faceBitmap.recycle()
            if (embedding != null) index to embedding else null
        }
    }

    /**
     * Crop a single face from the image using the bounding box, and align using eye landmarks
     * if available. Returns a 112x112 bitmap, or null if cropping fails.
     */
    private fun cropAndAlign(bitmap: Bitmap, face: Face): Bitmap? {
        val box = face.boundingBox
        val left = box.left.coerceIn(0, bitmap.width - 1)
        val top = box.top.coerceIn(0, bitmap.height - 1)
        val right = box.right.coerceIn(left + 1, bitmap.width)
        val bottom = box.bottom.coerceIn(top + 1, bitmap.height)

        val faceWidth = (right - left).coerceAtLeast(1)
        val faceHeight = (bottom - top).coerceAtLeast(1)

        val expandX = (faceWidth * 0.2f).toInt()
        val expandY = (faceHeight * 0.2f).toInt()
        val srcLeft = (left - expandX).coerceAtLeast(0)
        val srcTop = (top - expandY).coerceAtLeast(0)
        val srcRight = (right + expandX).coerceAtMost(bitmap.width)
        val srcBottom = (bottom + expandY).coerceAtMost(bitmap.height)

        val destBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)

        if (leftEye != null && rightEye != null) {
            val eyesCenterX = (leftEye.position.x + rightEye.position.x) / 2f
            val eyesCenterY = (leftEye.position.y + rightEye.position.y) / 2f
            val dX = rightEye.position.x - leftEye.position.x
            val dY = rightEye.position.y - leftEye.position.y
            val angle = Math.toDegrees(Math.atan2(dY.toDouble(), dX.toDouble())).toFloat()
            val faceSize = maxOf(srcRight - srcLeft, srcBottom - srcTop).toFloat()
            val scale = (inputSize * 0.75f) / faceSize

            val matrix = Matrix()
            matrix.preTranslate(-eyesCenterX, -eyesCenterY)
            matrix.postScale(scale, scale)
            matrix.postRotate(-angle)
            matrix.postTranslate(inputSize / 2f, inputSize / 2f)

            canvas.drawBitmap(bitmap, matrix, paint)
        } else {
            val src = android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom)
            val dst = android.graphics.Rect(0, 0, inputSize, inputSize)
            canvas.drawBitmap(bitmap, src, dst, paint)
        }

        return destBitmap
    }

    private fun runModel(bitmap: Bitmap): FloatArray? {
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        imgData.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixel = intValues[i * inputSize + j]
                imgData.putFloat((((pixel shr 16) and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat((((pixel shr 8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat(((pixel and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        val output = Array(1) { FloatArray(OUTPUT_SIZE) }
        return try {
            interpreter.run(imgData, output)
            l2Normalize(output[0])
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        const val INPUT_SIZE = 112
        const val OUTPUT_SIZE = 192
        const val MODEL_FILE = "mobile_face_net.tflite"
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f

        private fun loadModelFile(context: Context): MappedByteBuffer {
            val fd = context.assets.openFd(MODEL_FILE)
            val stream = FileInputStream(fd.fileDescriptor)
            val channel = stream.channel
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        }

        private fun l2Normalize(embedding: FloatArray): FloatArray {
            var sum = 0f
            for (v in embedding) sum += v * v
            val norm = kotlin.math.sqrt(sum)
            if (norm > 0f) {
                for (i in embedding.indices) embedding[i] /= norm
            }
            return embedding
        }
    }
}
