// language: kotlin, file: DetectionEngine.kt, target: Android 26+
package com.smith.screenbot

import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DetectionEngine(context: Context) : Closeable {
    private val interpreter: Interpreter

    init {
        val modelBuffer = context.assets.open("model.tflite").use { stream ->
            val bytes = stream.readBytes()
            ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
                put(bytes)
                rewind()
            }
        }
        interpreter = Interpreter(modelBuffer)
        try {
            val input = interpreter.getInputTensor(0)
            val output = interpreter.getOutputTensor(0)
            val expectedInput = intArrayOf(1, 640, 640, 3)
            val expectedOutput = intArrayOf(1, 7, 8400)
            require(input.shape().contentEquals(expectedInput)) {
                "Input shape ${input.shape().contentToString()}, expected ${expectedInput.contentToString()}"
            }
            require(input.dataType() == DataType.FLOAT32) {
                "Input type ${input.dataType()}, expected FLOAT32"
            }
            require(output.shape().contentEquals(expectedOutput)) {
                "Output shape ${output.shape().contentToString()}, expected ${expectedOutput.contentToString()}"
            }
            require(output.dataType() == DataType.FLOAT32) {
                "Output type ${output.dataType()}, expected FLOAT32"
            }
        } catch (error: Throwable) {
            interpreter.close()
            throw IllegalArgumentException("Incompatible model.tflite: ${error.message}", error)
        }
    }

    override fun close() = interpreter.close()
}
