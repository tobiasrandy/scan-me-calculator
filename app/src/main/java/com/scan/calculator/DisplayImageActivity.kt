package com.scan.calculator

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.scan.calculator.databinding.ActivityCameraBinding
import com.scan.calculator.databinding.ActivityDisplayImageBinding
import com.scan.calculator.databinding.ActivityMainBinding
import java.io.InputStream
import java.lang.StringBuilder

class DisplayImageActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityDisplayImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityDisplayImageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val imageUriString = intent.getStringExtra("imageUri")
        val imageUri = Uri.parse(imageUriString)

        Glide.with(this)
            .load(imageUri)
            .into(viewBinding.imageView)

        viewBinding.scanImageButton.setOnClickListener {
            detectText(imageUri)
        }
    }

    private fun detectText(imageUri: Uri) {
        val image = InputImage.fromBitmap(uriToBitmap(contentResolver, imageUri)!!, 0)
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result: Task<Text> = recognizer.process(image).addOnSuccessListener { text ->
            var result = StringBuilder()
            for(block in text.textBlocks) {
                val blockText = block.text
                val blockCornerPoint = block.cornerPoints
                val blockFrame = block.boundingBox
                for(line in block.lines) {
                    val lineText = line.text
                    val lineCornerPoint = line.cornerPoints
                    val lineRect = line.boundingBox
                    for(element in line.elements) {
                        val elementText = element.text
                        result.append(elementText)
                    }
                }

                val trimmedText = blockText.replace("\\s".toRegex(), "").trim()
                if(validate(trimmedText)) {
                    viewBinding.textView.text = trimmedText + "=" + calculate(trimmedText)
                } else {
                    Toast.makeText(this, "Text is not a math operation", Toast.LENGTH_SHORT)
                }
            }

        }. addOnFailureListener { e ->
            Toast.makeText(this, "Failed to detect text from image.." + e.message, Toast.LENGTH_SHORT)
        }
    }

    private fun uriToBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                BitmapFactory.decodeStream(inputStream).also { bitmap = it }
                inputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bitmap
    }

    private fun validate(text: String): Boolean {
        val validationRegex = """^[0-9+*/\-.() ]*$""".toRegex()
        return validationRegex.matches(text)
    }

    private fun calculate(text: String): Int {
        val regex = "([+\\-*/]?([0-9]+))".toRegex()
        val matches = regex.findAll(text).map { it.groupValues[1] }.toList()
        return evaluateExpression(matches)
    }

    private fun evaluateExpression(matches: List<String>): Int {
        val stack = mutableListOf<Int>()
        var operator = "+"

        for (i in matches) {
            if (i in setOf("+", "-", "*", "/")) {
                operator = i
            } else {
                val value = i.toInt()
                when (operator) {
                    "+" -> stack.add(value)
                    "-" -> stack.add(-value)
                    "*" -> stack[stack.size - 1] *= value
                    "/" -> stack[stack.size - 1] /= value
                }
            }
        }

        return stack.sum()
    }
}