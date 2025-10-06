package com.example.auto_saver

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.appbar.MaterialToolbar
import java.io.File

class PhotoViewerActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivPhoto: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        toolbar = findViewById(R.id.toolbar)
        ivPhoto = findViewById(R.id.iv_photo)

        setupToolbar()
        loadPhoto()
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadPhoto() {
        val photoPath = intent.getStringExtra("PHOTO_PATH")
        if (photoPath != null) {
            val file = File(photoPath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )
                ivPhoto.setImageURI(uri)
            }
        }
    }
}

