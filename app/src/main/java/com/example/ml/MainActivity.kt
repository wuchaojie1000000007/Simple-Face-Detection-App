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
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val imageView by lazy { binding.imageView }
    private val predictButton by lazy { binding.predictButton }
    private val labelButton by lazy { binding.labelButton }
    private val labelText by lazy { binding.labelText }
    private val localizeButton by lazy { binding.localizeButton }

    private val imageLabeler: ImageLabeler by lazy { getMyImageLabeler() }
    private val faceDetector by lazy { getMyFaceDetector() }
    private val objectDetector by lazy { getMyObjectDetector() }

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

        labelButton.setOnClickListener {
            val result = imageLabeler.process(image)
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "List size is: ${it.size}\n" +
                                "For the first label\n" +
                                "Image label is ${it[0].text}\n" +
                                "Confidence is ${it[0].confidence}\n" +
                                "Index is ${it[0].index}"
                    )
                    labelText.text = it[0].text
                }.addOnFailureListener {
                    Log.d(TAG, "$it")
                }
        }

        predictButton.setOnClickListener {
            val result = faceDetector.process(image)
                .addOnSuccessListener {
                    bitmap.apply {
                        imageView.setImageBitmap(drawWithRectangleFaces(it))
                    }
                }.addOnFailureListener {
                    Log.d(TAG, "$it")
                }
        }

        localizeButton.setOnClickListener {
            objectDetector.process(image)
                .addOnSuccessListener {
                    bitmap.apply {
                        imageView.setImageBitmap(drawWithRectangleObjects(it))
                    }
                }
        }
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


    private fun Bitmap.drawWithRectangleFaces(faces: List<Face>): Bitmap {
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

    private fun Bitmap.drawWithRectangleObjects(objects: List<DetectedObject>): Bitmap {

        // Init bitmap, canvas, paint
        val bitmap = copy(config, true)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            textSize = 100f
            strokeWidth = 4f
            isAntiAlias = true
        }

        // For each detected object, crop the bitmap, feed to Image Labeling model to get label,
        // then draw the bounding box and label
        objects.forEach {
            val bounds = it.boundingBox
            val croppedBitmap =
                Bitmap.createBitmap(this, bounds.left, bounds.top, bounds.width(), bounds.height())
            val croppedImage = InputImage.fromBitmap(croppedBitmap, 0)

            imageLabeler.process(croppedImage)
                .addOnSuccessListener { labels ->
                    Log.d(TAG, "label size is ${labels.size}\nfirst label is ${labels[0].text}")

                    if (labels.size > 0 && labels[0].text.isNotEmpty()) {

                        canvas.drawRect(bounds, paint)
                        canvas.drawText(
                            labels[0].text,
                            bounds.left.toFloat(),
                            bounds.top.toFloat(),
                            paint
                        )
                    }
                }.addOnFailureListener {
                    Log.d(TAG, "$it")
                }
        }
        return bitmap
    }

    private fun getMyFaceDetector(): FaceDetector {
        val faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        return FaceDetection.getClient(faceDetectorOptions)
    }

    private fun getMyImageLabeler() = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    private fun getMyObjectDetector(): ObjectDetector {
        val objectDetectorOptions = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()
        return ObjectDetection.getClient(objectDetectorOptions)
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}