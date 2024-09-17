//package com.example.summarization
//import java.io.IOException
//import com.example.summarization.databinding.ActivityMainBinding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.mlkit.vision.common.InputImage

import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
        private const val REQUEST_CODE_IMAGE_PICKER = 1
        private const val REQUEST_CODE_PDF_PICKER = 2
    }

    private lateinit var image: InputImage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setListener()
    }

    fun setListener() {
        rl_select_img.setOnClickListener {
            openImagePicker() // Existing logic for selecting image
        }

        rl_select_pdf.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/pdf"
            startActivityForResult(intent, REQUEST_CODE_PDF_PICKER)
        }
    }

    private fun openImagePicker() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .start(REQUEST_CODE_IMAGE_PICKER)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_IMAGE_PICKER -> {
                    val fileUri = data?.data
                    if (fileUri != null) {
                        processImage(fileUri)
                    }
                }
                REQUEST_CODE_PDF_PICKER -> {
                    val pdfUri = data?.data
                    if (pdfUri != null) {
                        convertPdfToImages(pdfUri)
                    }
                }
                else -> {
                    // Handle other cases (optional)
                }
            }
        } else {
            // Handle failed results (optional)
        }
    }

    private fun processImage(fileUri: Uri) {
        binding.tvConvertedText.text = ""
        binding.progressBar.visibility = View.VISIBLE
        try {
            image = InputImage.fromFilePath(this, fileUri)
            val recognizer = TextRecognition.getClient()
            val result = recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    Log.d(TAG, "processImage: success")
                    val resultText = visionText.text
                    Log.d(TAG, "processImage: extractedText: $resultText")

                    if (TextUtils.isEmpty(resultText)) {
                        binding.progressBar.visibility = View.GONE
                        binding.tvConvertedText.text = resources.getString(R.string.no_text_found)
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.tvConvertedText.text = resultText
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "processImage: failure: ${e.message}")
                }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun convertPdfToImages(pdfUri: Uri) {
        val pdfDocument = PDDocument.load(contentResolver.openInputStream(pdfUri))
        val pdfRenderer = PDFRenderer(pdfDocument)

        val totalPages = pdfDocument.numberOfPages
        val extractedImages = mutableListOf<Uri>()

        for (pageIndex in 0 until totalPages) {
            val image = pdfRenderer.renderImage(pageIndex)
            val tempFile = File.createTempFile("pdf_page_${pageIndex}", ".jpg")
            FileOutputStream(tempFile).use { fos ->
                image.writeJPEG(fos, 100)
            }
            extractedImages.add(Uri.fromFile(tempFile))
        }

        pdfDocument.close()

        // Process each extracted image
        for (imageUri in extractedImages) {
            processImage(imageUri)
        }

        // Display the last page image (optional)
        val lastPageImageUri = extractedImages.lastOrNull()
        if (lastPageImageUri != null) {
            iv_selected_img.setImageURI(lastPageImageUri)
        }
    }

    private fun setViewVisibility() {
        binding.ivSelectedImg.visibility = View.VISIBLE
        binding.tvImgDetails.visibility = View.VISIBLE
        binding.scrollView.visibility = View.VISIBLE
        binding.llNoselectMessage.visibility = View.GONE
    }
}
