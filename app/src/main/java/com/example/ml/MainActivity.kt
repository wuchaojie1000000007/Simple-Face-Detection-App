package com.example.ml

import android.content.Context
import android.graphics.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.ml.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val imageView by lazy { binding.imageView }
    private val predictButton by lazy { binding.predictButton }
    private val fileName = "man.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bitmap: Bitmap? = assetToBitmap(fileName)
        bitmap?.apply {
            imageView.setImageBitmap(this)
        }

        predictButton.setOnClickListener {
            val faceDetectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()
            val detector = FaceDetection.getClient(faceDetectorOptions)
            val image = InputImage.fromBitmap(bitmap!!, 0)

            val result = detector.process(image)
                .addOnSuccessListener {
                    bitmap.apply {
                        imageView.setImageBitmap(drawWithRectangle(it))
                    }
                }.addOnFailureListener {
                    Log.d("MainActivity", "$it")
                }
        }
    }

    private fun Bitmap.drawWithRectangle(faces: List<Face>): Bitmap? {
        val bitmap = copy(config, true)
        val canvas = Canvas(bitmap)
        faces.forEach {
            val bounds = it.boundingBox
            Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 4.0f
                isAntiAlias = true
                canvas.drawRect(bounds, this)
            }
        }
        return bitmap
    }

    private fun Context.assetToBitmap(fileName: String): Bitmap? {
        return try {
            assets.open(fileName).let {
                BitmapFactory.decodeStream(it)

            }
            /*with(assets.open(fileName)) {
                BitmapFactory.decodeStream(this)
            }*/
        } catch (e: IOException) {
            null
        }
    }
}