import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.content.Intent
import android.net.Uri
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.summarization.databinding.ActivityMainBinding
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.example.summarization.R
//import com.google.mlkit.vision.text.TextRecognition
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
        private const val REQUEST_CODE_IMAGE_PICKER = 1
        private const val REQUEST_CODE_PDF_PICKER = 2
    }

    private lateinit var image: InputImage
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListener()
    }

    fun setListener() {
        binding.rlSelectImg.setOnClickListener {
            openImagePicker() // Existing logic for selecting image
        }

        binding.rlSelectPdf.setOnClickListener {
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

                    if (resultText.isEmpty()) {
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
        try {
            // Open the PDF as a ParcelFileDescriptor
            val input = contentResolver.openFileDescriptor(pdfUri, "r") ?: return
            val pdfRenderer = PdfRenderer(input)

            // List to hold rendered images
            val extractedImages = mutableListOf<Uri>()

            // Loop through each page of the PDF
            val totalPages = pdfRenderer.pageCount
            for (pageIndex in 0 until totalPages) {
                val page = pdfRenderer.openPage(pageIndex)

                // Create a bitmap for the page image
                val bitmap = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )

                // Render the page onto the bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                // Save bitmap to a temp file and add its URI to the list
                val tempFile = File.createTempFile("pdf_page_${pageIndex}", ".jpg")
                FileOutputStream(tempFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                extractedImages.add(Uri.fromFile(tempFile))
            }

            pdfRenderer.close()
            input.close()

            // Process each extracted image
            for (imageUri in extractedImages) {
                processImage(imageUri)
            }

            // Optionally display the last page image
            val lastPageImageUri = extractedImages.lastOrNull()
            if (lastPageImageUri != null) {
                binding.ivSelectedImg.setImageURI(lastPageImageUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Error rendering PDF: ${e.message}")
        }
    }

    private fun setViewVisibility() {
        binding.ivSelectedImg.visibility = View.VISIBLE
        binding.tvImgDetails.visibility = View.VISIBLE
        binding.scrollView.visibility = View.VISIBLE
        binding.llNoselectMessage.visibility = View.GONE
    }
}
