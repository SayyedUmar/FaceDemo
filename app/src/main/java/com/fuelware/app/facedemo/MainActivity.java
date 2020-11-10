package com.fuelware.app.facedemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_1 = 12141;
    private static final int CAMERA_REQUEST_2 = 12142;
    private static final int GALLERY_REQUEST = 12477;
    @ViewById
    ImageView imageView1;
    @ViewById
    ImageView imageView2;
    @ViewById
    Button btnUpload1;
    @ViewById
    Button btnUpload2;
    @ViewById
    Button btnCompare;
    private Bitmap photo1, photo2;


    private File photoFile;


    @Click(R.id.btnUpload1)
    void setBtnUpload1Click () {
        openCamera(CAMERA_REQUEST_1);
    }

    @Click(R.id.btnUpload2)
    void setBtnUpload2Click () {
        openCamera(CAMERA_REQUEST_2);
    }

    @Click(R.id.btnCompare)
    void btnCompareClick () {
        if (photo1 == null) {
            MLog.showLongToast(getApplicationContext(), "Select Image 1");
        } else if (photo2 == null) {
            MLog.showLongToast(getApplicationContext(), "Select Image 2");
        } else {
            onCaptureImageResult();
        }
    }

    private void openCamera(int requestCode) {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.CAMERA)
                .onGranted(permissions -> {
                    AndPermission.with(this)
                            .runtime()
                            .permission(Permission.WRITE_EXTERNAL_STORAGE)
                            .onGranted(permissions1 -> {
                                openCameraIntent(requestCode);
                            })
                            .onDenied(permissions1 -> {
                                MLog.showToast(getApplicationContext(), "Write External Storage permission denied");
                            })
                            .start();
                })
                .onDenied(permissions -> {
                    MLog.showToast(getApplicationContext(), "Camera permission denied");
                })
                .start();
    }

    private void openCameraIntent(int requestCode) {
//        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(cameraIntent, requestCode);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoFile = MUtils.getCameraFile(this);
        if (cameraIntent.resolveActivity(getPackageManager()) != null && photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID+".provider", photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(cameraIntent, requestCode);
        }
    }

    private void openGalleryIntent () {
        String[] supportedMimeTypes = {"image/*", "application/pdf"};
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        intent.setType("image/*|application/pdf");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Select Either Image orFile"), GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_1 && resultCode == Activity.RESULT_OK) {
//            photo1 = (Bitmap) data.getExtras().get("data");
            Glide.with(this).load(photoFile.getAbsolutePath()).into(imageView1);
            uploadImage(photoFile, null);
        } else if (requestCode == CAMERA_REQUEST_2 && resultCode == Activity.RESULT_OK) {
            photo2 = (Bitmap) data.getExtras().get("data");
            imageView2.setImageBitmap(photo2);
        } else if (requestCode == GALLERY_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri contentURI = data.getData();
            String fileExtension = MUtils.getExtension(this, contentURI);
            if(!fileExtension.equalsIgnoreCase("pdf")) {
                handleImageUpload(contentURI);
            } else {
                handlePdfUpload(contentURI);
            }

        }
    }

    private void handlePdfUpload(Uri uri) {

        try {
            InputStream in = null;
            OutputStream out = null;
            File pdfFolder = new File(getExternalFilesDir(null), "Pictures"); // check th
            if (!pdfFolder.exists()) {
                pdfFolder.mkdirs();
            }
            Date date = new Date();
            final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
            File newFile = new File(pdfFolder + "/" + timeStamp + ".pdf");

            try {
                in = getContentResolver().openInputStream(uri);
                out = new FileOutputStream(newFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null){
                    out.close();
                }
            }
            Uri apkURI = FileProvider.getUriForFile(this, getPackageName() + ".provider", newFile);
            File result = new File(apkURI.toString());
        }catch (Exception e) {e.printStackTrace();}
    }

    private void handleImageUpload(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
//                    bitmaps.add(bitmap);
//                    convertToPDF(bitmaps, data);
    }

    @Background
    void onCaptureImageResult() {
            ByteArrayOutputStream bytes1 = new ByteArrayOutputStream();
            ByteArrayOutputStream bytes2 = new ByteArrayOutputStream();
            photo1.compress(Bitmap.CompressFormat.JPEG, 100, bytes1);
            photo2.compress(Bitmap.CompressFormat.JPEG, 100, bytes2);
            File file1 = new File(getCacheDir().getPath() + "/","_Profile" + System.currentTimeMillis() + ".jpg");
            File file2 = new File(getCacheDir().getPath() + "/","_Profile_" + System.currentTimeMillis() +"_"+ System.currentTimeMillis() + ".jpg");
            FileOutputStream fo1, fo2;
            try {
                file1.createNewFile();
                file2.createNewFile();

                fo1 = new FileOutputStream(file1);
                fo1.write(bytes1.toByteArray());
                fo1.close();

                fo2 = new FileOutputStream(file2);
                fo2.write(bytes2.toByteArray());
                fo2.close();

                uploadImage(file1, file2);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    @MainThread
    void uploadImage(File file1, File file2) {
        RequestBody fbody1 = RequestBody.create(MediaType.parse("image/*"), file1);
//        RequestBody fbody2 = RequestBody.create(MediaType.parse("image/*"), file2);
        MultipartBody.Part fileToUpload1 = MultipartBody.Part.createFormData("image1", file1.getName(), fbody1);
//        MultipartBody.Part fileToUpload2 = MultipartBody.Part.createFormData("image2", file2.getName(), fbody2);
        Call<ResponseBody> call = APIClient.getApiService().uploadSnapImage(fileToUpload1);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if(response.isSuccessful()) {
                        String bool = response.body().string();
                        if (bool.equalsIgnoreCase("true"))
                            MLog.showToast(getApplicationContext(), "Matched successfully.");
                        else
                            MLog.showToast(getApplicationContext(), "Not Matched.");

//                        JSONObject res = new JSONObject(response.body().string());
                    } else {
                        MLog.showToast(getApplicationContext(),"Something went wrong, Try again.");
                    }
                }catch(Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                MLog.showToast(getApplicationContext(), t.getMessage());
            }
        });
    }
}
