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
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val imageView by lazy { binding.imageView }
    private val predictButton by lazy { binding.predictButton }
    private val labelButton by lazy { binding.labelButton }
    private val labelText by lazy { binding.labelText }
    private val fileName = "man.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get bitmap from assets folder
        val bitmap: Bitmap? = assetToBitmap(fileName)

        // Use the bitmap to set imageView
        bitmap?.apply { imageView.setImageBitmap(this) }

        // Get InputImage for the ML model from the bitmap
        val image = InputImage.fromBitmap(bitmap!!, 0)

        // Get Face Detection model
        val faceDetector = getFaceDetector()

        predictButton.setOnClickListener {
            val result = faceDetector.process(image)
                .addOnSuccessListener {
                    bitmap.apply {
                        imageView.setImageBitmap(drawWithRectangle(it))
                    }
                }.addOnFailureListener {
                    Log.d("MainActivity", "$it")
                }
        }

        // Get Image Labeling model
        val imageLabeler = getImageLabeler()

        labelButton.setOnClickListener {
            val result = imageLabeler.process(image)
                .addOnSuccessListener {
                    labelText.text = it[0].text
                    Log.d("MainActivity", "${it[0]}")
                }.addOnFailureListener {
                    Log.d("MainActivity", "$it")
                }
        }
    }

    private fun Bitmap.drawWithRectangle(faces: List<Face>): Bitmap {
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
        } catch (e: IOException) {
            null
        }
    }

    private fun getFaceDetector(): FaceDetector {
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        return FaceDetection.getClient(faceDetectorOptions)
    }

    private fun getImageLabeler() = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
}