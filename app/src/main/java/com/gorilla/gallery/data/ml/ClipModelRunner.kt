package com.gorilla.gallery.data.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ClipModelRunner(private val context: Context) {

    private var imageInterpreter: Interpreter? = null
    private var textInterpreter: Interpreter? = null

    init {
        try {
            imageInterpreter = Interpreter(loadModelFile(context, "clip_image.tflite"))
            textInterpreter = Interpreter(loadModelFile(context, "clip_text.tflite"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun embedImage(bitmap: Bitmap): FloatArray {
        val inputSize = 224
        val imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())
        
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val intValues = IntArray(inputSize * inputSize)
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.width, 0, 0, scaledBitmap.width, scaledBitmap.height)
        
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val pixel = intValues[i * inputSize + j]
                imgData.putFloat((((pixel shr 16) and 0xFF) - 127.5f) / 127.5f)
                imgData.putFloat((((pixel shr 8) and 0xFF) - 127.5f) / 127.5f)
                imgData.putFloat(((pixel and 0xFF) - 127.5f) / 127.5f)
            }
        }
        
        val output = Array(1) { FloatArray(512) }
        
        imageInterpreter?.run(imgData, output) ?: run {
            return FloatArray(512) { 0f }
        }
        
        scaledBitmap.recycle()
        return l2Normalize(output[0])
    }

    fun embedText(query: String): FloatArray {
        val output = Array(1) { FloatArray(512) }
        return FloatArray(512) { 0f }
    }

    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fd = context.assets.openFd(modelFile)
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

    fun close() {
        imageInterpreter?.close()
        textInterpreter?.close()
    }
}
