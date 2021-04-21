package com.example.draw

import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    var ibBrushChooser: ImageButton? = null
    var dv: DrawingView? = null
    var ibCurrentColor: ImageButton? = null
    var llColor: LinearLayout?= null
    var ibImportBg: ImageButton?= null
    var ivBg: ImageView? = null
    var ibUndo: ImageButton? = null
    var ibSave: ImageButton? = null
    var fl: FrameLayout? = null

    val small_brush_size = 10f
    val medium_brush_size = 20f
    val large_brush_size = 30f
    val ACCESS_EXTERNAL_STORAGE_CODE = 1
    val IMAGE_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ibBrushChooser = findViewById(R.id.ib_brush_chooser)
        dv = findViewById(R.id.dv)
        llColor = findViewById(R.id.ll_color)
        ibCurrentColor = llColor!![0] as ImageButton
        ibImportBg = findViewById(R.id.ib_import_bg)
        ivBg = findViewById(R.id.iv_bg)
        ibUndo = findViewById(R.id.ib_undo)
        ibSave = findViewById(R.id.ib_save)
        fl = findViewById(R.id.fl)


        // init color and brush size
        dv!!.setColor((ibCurrentColor!!.background as ColorDrawable).color)
        dv!!.setBrushSize(small_brush_size)

        // set on click listener
        ibBrushChooser!!.setOnClickListener{
            showDialogBrush()
        }
        ibImportBg!!.setOnClickListener {
//            if(isReadStorageAllowed()) {
//                getImageFromGallery()
//            } else
//                requestReadPermission()
            getImageFromGallery()
        }
        ibUndo!!.setOnClickListener {
            dv!!.undo()
        }
        ibSave!!.setOnClickListener {
//            if(isReadStorageAllowed()) {
//                saveImageToGallery()
//            } else {
//                requestReadPermission()
//            }
            // before save
            CoroutineScope(IO).launch {
                saveImageCoroutine(getBitmapFromView(fl!!)).Execute()
            }
        }

    }

    private inner class saveImageCoroutine(val bitmap: Bitmap) {
        var dialog: Dialog? = null

        fun onPreExe() {
            dialog = Dialog(this@MainActivity)
            dialog!!.setTitle("waiting")
            dialog!!.setContentView(R.layout.dialog_waiting)
            dialog!!.show()
        }

        suspend fun inDoingBackground() {
            try {
                val IMAGES_FOLDER_NAME = "HaiDrawing"
                val name = "HaiDrawing_" + System.currentTimeMillis()/1000 + ".png"
                val resolver: ContentResolver = this@MainActivity.getContentResolver()
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/$IMAGES_FOLDER_NAME")
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                val fos = resolver.openOutputStream(imageUri!!)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                // flush() is to written down bytes buffed by fos
                fos!!.flush()
                fos.close()
                delay(1500)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun postExe() {
            Toast.makeText(this@MainActivity, "Image is saved", Toast.LENGTH_SHORT).show()
            dialog!!.dismiss()
        }

        suspend fun Execute() {
            withContext(Main) {
                onPreExe()
            }
            inDoingBackground()
            withContext(Main) {
                postExe()
            }
        }
    }

    private fun getImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK)
            if(requestCode == IMAGE_REQUEST_CODE) {
                if(data != null) {
                    val uri = data!!.data
                    ivBg!!.setImageURI(uri)
                } else {
                    Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show()
                }
            }

    }

    fun colorClickHandler(view: View) {
        ibCurrentColor!!.setImageResource(R.drawable.color)
        (view as ImageButton).setImageResource(R.drawable.selected_color)
        ibCurrentColor = view
        val color = (ibCurrentColor!!.background as ColorDrawable).color
        dv!!.setColor(color)
    }

    fun showDialogBrush() {
        val dialog = Dialog(this)

        dialog.setContentView(R.layout.dialog_brush)
        // set on click
        dialog.findViewById<ImageButton>(R.id.btn_small_brush).setOnClickListener {
            dv!!.setBrushSize(small_brush_size)
            dialog.hide()
        }
        dialog.findViewById<ImageButton>(R.id.btn_medium_brush).setOnClickListener {
            dv!!.setBrushSize(medium_brush_size)
            dialog.hide()
        }
        dialog.findViewById<ImageButton>(R.id.btn_large_brush).setOnClickListener {
            dv!!.setBrushSize(large_brush_size)
            dialog.hide()
        }
        dialog.setTitle("Choose brush size")
        dialog.show()
    }

    fun isReadStorageAllowed(): Boolean {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    fun requestReadPermission() {
        if(!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE).toString())) {
//            Toast.makeText(this, "Permission is needed!", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                ACCESS_EXTERNAL_STORAGE_CODE)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == ACCESS_EXTERNAL_STORAGE_CODE)
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                getImageFromGallery()
            else
                Toast.makeText(this, "Permission is not Granted! Please change in setting!", Toast.LENGTH_SHORT).show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        var returnedbitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(returnedbitmap)
        view.draw(canvas)
        return returnedbitmap
    }
}