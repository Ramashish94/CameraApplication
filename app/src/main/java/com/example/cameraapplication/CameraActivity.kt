package com.example.cameraapplication

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.example.cameraapplication.ImagePickerActivity.showImagePickerOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_camera.*
import okhttp3.MultipartBody
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.net.URLEncoder
import java.util.*

class CameraActivity : AppCompatActivity(), ImagePickerActivity.PickerOptionListener,
    HandlePathOzListener.SingleUri {
    val REQUEST_IMAGE = 100
    var mPickerOptionListener: ImagePickerActivity.PickerOptionListener? = null
    private lateinit var handlePathOz: HandlePathOz


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        handlePathOz = HandlePathOz(this, this)
        setView()
        mPickerOptionListener = this
    }

    fun setView() {

        img_profile_pic.setOnClickListener(View.OnClickListener {
            Dexter.withContext(getApplicationContext())
                .withPermissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                        // check for permanent denial of any permission
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                            // show alert dialog navigating to Settings
                            showSettingsDialog()
                        }
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            showImagePickerOptions()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        list: List<PermissionRequest>,
                        permissionToken: PermissionToken
                    ) {
                    }
                })
                .withErrorListener { dexterError ->
                    showSettingsDialog()
                    Toast.makeText(
                        this@CameraActivity,
                        "Error occurred! $dexterError",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .check()

        })
    }

    private fun showImagePickerOptions() {
        ImagePickerActivity.showImagePickerOptions(
            this@CameraActivity,
            mPickerOptionListener
        )
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(
            this@CameraActivity,
            R.style.AlertDialogCustomSettings
        )
        builder.setTitle("Need Permissions")
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.")
        builder.setPositiveButton(
            "GOTO SETTINGS"
        ) { dialog, which ->
            dialog.cancel()
            openSettings()
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        builder.show()
    }

    // navigating user to app settings
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", this@CameraActivity.getPackageName(), null)
        intent.data = uri
        startActivityForResult(intent, 101)
    }


    override fun onTakeCameraSelected() {
        launchCameraIntent()
    }

    override fun onChooseGallerySelected() {
        launchGalleryIntent()
    }

    override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
        Log.d("hi", pathOz.path)
        //uploadPic(pathOz.path)
    }

    private fun launchCameraIntent() {
        val intent: Intent = Intent(
            this@CameraActivity,
            ImagePickerActivity::class.java
        )
        intent.putExtra(
            ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
            ImagePickerActivity.REQUEST_IMAGE_CAPTURE
        )

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)

        // setting maximum bitmap width and height
        intent.putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true)
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, 1000)
        intent.putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, 1000)
        startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun launchGalleryIntent() {
        val intent: Intent = Intent(
            this@CameraActivity,
            ImagePickerActivity::class.java
        )
        intent.putExtra(
            ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION,
            ImagePickerActivity.REQUEST_GALLERY_IMAGE
        )

        // setting aspect ratio
        intent.putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1) // 16x9, 1x1, 3:4, 3:2
        intent.putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)
        startActivityForResult(intent, REQUEST_IMAGE)
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request we're responding to
        if (requestCode == 102) {
        }

        // handle result of pick image chooser
//        if (requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
//            val imageUri = CropImage.getPickImageResultUri(this, data)
//
//            // For API >= 23 we need to check specifically that we have permissions to read external storage.
//            if (CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)) {
//                // request permissions and handle the result in onRequestPermissionsResult()
//                mCropImageUri = imageUri
//                requestPermissions(
//                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
//                    0
//                )
//            } else {
//                // no permissions required or already grunted, can start crop image activity
//                startCropImageActivity(imageUri)
//            }
//        }
//
//        // handle result of CropImageActivity
//        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
//            val result = CropImage.getActivityResult(data)
//            if (resultCode == RESULT_OK) {
//                val str_image_uri = result.uri.toString()
//                setImageFromStorage(str_image_uri)
//                uploadPic(str_image_uri)
//            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
//                Toast.makeText(
//                    this,
//                    "Cropping failed: $result",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }


        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                val uri = data!!.getParcelableExtra<Uri>("path")
                try {
                    // You can update this bitmap to your server
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

                    // loading profile image from local cache
//                    loadProfile(uri.toString())
                    val str_image_uri = uri.toString()
                    handlePathOz.getRealPath(uri!!)
                    setImageFromStorage(str_image_uri)

                    saveImageToInternalStorage(bitmap)

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    }

    fun setImageFromStorage(imgpath: String?) {
        if (imgpath != null) {
            Glide.with(this).asBitmap().load(Uri.parse(imgpath))
                .placeholder(R.drawable.default_user_dark).centerInside()
                .into(object : BitmapImageViewTarget(img_profile_pic) {
                    override fun setResource(resource: Bitmap?) {
                        val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(
                            this@CameraActivity.getResources(),
                            resource
                        )
                        circularBitmapDrawable.cornerRadius = 20f
                        img_profile_pic.setImageDrawable(circularBitmapDrawable)
                    }
                })
        }
    }


    private fun saveImageToInternalStorage(takenImage: Bitmap?) {
        val fos: OutputStream
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image_" + ".jpg")
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "Image/jpg")
                contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + "srlImage"
                )
                val imageUri =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri)!!)!!
                takenImage?.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                Objects.requireNonNull<OutputStream?>(fos)
                Toast.makeText(applicationContext, "Image saved", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Image not saved", Toast.LENGTH_SHORT).show()
        }

    }


    /*fun uploadPic(imagestoragepath: String) {
//        var file1: File? = null
//        val bitmap = decodeUriToBitmap(this@UserDetailsActivity, Uri.parse(imagestoragepath))
//        if (bitmap != null) {
//            file1 = saveToExternal(bitmap)
//        }

        val imageFileuri = File(imagestoragepath)

        val fileBody = ProgressRequestBodyImage(imageFileuri, this@UserDetailsActivity)

        val videofile = MultipartBody.Part.createFormData(
            "image",
            URLEncoder.encode(imageFileuri!!.name, "utf-8"),
            fileBody
        )


        val loginService = ApiClientVideo.buildService(ApiInterface::class.java)


        val requestCall =
            loginService.uploadPic(
                sharedPreferenceUtility!!.getString(AppConstants.TOKEN),
                videofile
            )

        requestCall.enqueue(object : Callback<UploadPicResponse> {

            override fun onResponse(
                call: Call<UploadPicResponse>,
                response: Response<UploadPicResponse>
            ) {


                try {
                    if (response.isSuccessful) {
                        progressDialog!!.dismiss()
                        val loginEmailResponse = response.body()
                        if (response.code() == 200) {
                            if (loginEmailResponse!!.status == 200) {
                                Toast.makeText(
                                    this@UserDetailsActivity,
                                    "profile photo uploaded successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showSnackBar(
                                    getString(R.string.something_went_wrong)
                                )
                            }

                        } else if (response.code() == 403) {
                            progressDialog!!.dismiss()
                            showSnackBar(
                                getString(R.string.something_went_wrong)
                            )

                        } else {
                            progressDialog!!.dismiss()
                            showSnackBar(
                                getString(R.string.something_went_wrong)
                            )

                        }
                    }
                } catch (e: Exception) {
                    progressDialog!!.dismiss()
                    e.printStackTrace()
                    showSnackBar(
                        getString(R.string.something_went_wrong)
                    )
                }

            }

            override fun onFailure(call: Call<UploadPicResponse>, t: Throwable) {
                progressDialog!!.dismiss()
                showSnackBar(
                    getString(R.string.something_went_wrong)
                )
            }
        })
    }*/

}