package com.phmb2.compressimage;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;

import id.zelory.compressor.Compressor;
import id.zelory.compressor.FileUtil;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_PHOTO_GALERY = 1;
    private static final int REQUEST_PHOTO_CAMERA = 2;

    private ImageView actualImage;
    private ImageView compressedImage;

    private TextView actualImageSize;
    private TextView compressedImageSize;

    private static File actualImageFile;
    private static File compressedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actualImage = (ImageView) findViewById(R.id.actual_image);
        compressedImage = (ImageView) findViewById(R.id.compressed_image);
        actualImageSize = (TextView) findViewById(R.id.actual_size);
        compressedImageSize = (TextView) findViewById(R.id.compressed_size);

        actualImage.setBackgroundColor(getRandomColor());
        clearImage();
    }

    public void cameraImage(View view)
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_PHOTO_CAMERA);
    }

    public void galeryImage(View view) {

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PHOTO_GALERY);
        }
        else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PHOTO_GALERY);
        }
    }

    public void compressImage(View view) {

        if (actualImageFile == null) {
            showError(getResources().getString(R.string.please_choose_image));
        } else {

            // Compress image in main thread
            //compressedImageFile = Compressor.getDefault(this).compressToFile(actualImageFile);
            //setCompressedImage();

            // Compress image to bitmap in main thread
            /*compressedImageView.setImageBitmap(Compressor.getDefault(this).compressToBitmap(actualImageFile));*/

            // Compress image using RxJava in background thread
            Compressor.getDefault(this)
                    .compressToFileAsObservable(actualImageFile)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<File>() {
                        @Override
                        public void call(File file) {
                            compressedImageFile = file;
                            setCompressedImage();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            showError(throwable.getMessage());
                        }
                    });
        }
    }

    public void customCompressImage(View view) {
        if (actualImageFile == null) {
            showError(getResources().getString(R.string.please_choose_image));
        } else {
            // Compress image in main thread using custom Compressor
            /*compressedImageFile = new Compressor.Builder(this)
                    .setMaxWidth(640)
                    .setMaxHeight(480)
                    .setQuality(75)
                    .setCompressFormat(Bitmap.CompressFormat.WEBP)
                    .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath())
                    .build()
                    .compressToFile(actualImageFile);

            setCompressedImage();*/

            // Compress image using RxJava in background thread with custom Compressor
            new Compressor.Builder(this)
                    .setMaxWidth(640)
                    .setMaxHeight(480)
                    .setQuality(75)
                    .setCompressFormat(Bitmap.CompressFormat.WEBP)
                    .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES).getAbsolutePath())
                    .build()
                    .compressToFileAsObservable(actualImageFile)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<File>() {
                        @Override
                        public void call(File file) {
                            compressedImageFile = file;
                            setCompressedImage();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            showError(throwable.getMessage());
                        }
                    });
        }
    }

    private void setCompressedImage()
    {
        compressedImage.setImageBitmap(BitmapFactory.decodeFile(compressedImageFile.getAbsolutePath()));
        compressedImageSize.setText(String.format("Size: %s", getReadableFileSize(compressedImageFile.length())));

        Toast.makeText(this, "Compressed image save in " + compressedImageFile.getPath(), Toast.LENGTH_LONG).show();
        Log.d("Compressor", "Compressed image save in " + compressedImageFile.getPath());
    }

    private void clearImage()
    {
        actualImage.setBackgroundColor(getRandomColor());
        compressedImage.setImageDrawable(null);
        compressedImage.setBackgroundColor(getRandomColor());
        compressedImageSize.setText(getResources().getString(R.string.size_image));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PHOTO_GALERY && resultCode == RESULT_OK) {
            if (data == null) {
                showError("Failed to open picture!");
                return;
            }
            try {
                actualImageFile = FileUtil.from(this, data.getData());
                actualImage.setImageBitmap(BitmapFactory.decodeFile(actualImageFile.getAbsolutePath()));
                actualImageSize.setText(String.format("Size: %s", getReadableFileSize(actualImageFile.length())));
                clearImage();
            } catch (IOException e) {
                showError("Failed to read picture data!");
                e.printStackTrace();
            }
        }
        /*else {
            if (requestCode == REQUEST_PHOTO_CAMERA && resultCode == RESULT_OK) {

                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");

                final String imageFilePath = createImagesDirectory(this);
                System.out.println(imageFilePath);

                Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", actualImageFile);
                actualImageFile = FileUtil.from(this, data.getData());

                imageBitmap = getBitmapFromUri(this, uri);

                actualImage.setImageBitmap(BitmapFactory.decodeFile(actualImageFile.getAbsolutePath()));
                actualImage.setImageBitmap(imageBitmap);
                actualImageSize.setText(String.format("Size: %s", getReadableFileSize(actualImageFile.length())));

                clearImage();
            }
        }*/
    }

    private static String createImagesDirectory(Context context){
        ContextWrapper cw = new ContextWrapper(context);
        actualImageFile = cw.getDir("Images_app", Context.MODE_PRIVATE);
        return actualImageFile.getAbsolutePath();
    }

    public void showError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private int getRandomColor() {
        Random rand = new Random();
        return Color.argb(100, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));

    }

    public String getReadableFileSize(long size)
    {
        if (size <= 0) {
            return "0";
        }

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private Bitmap getBitmapFromUri(Context context, Uri uri)
    {
        ParcelFileDescriptor parcelFileDescriptor;
        Bitmap image = null;

        try {
            parcelFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();

        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return image;
    }
}



