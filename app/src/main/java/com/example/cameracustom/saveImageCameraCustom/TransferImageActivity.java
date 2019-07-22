package com.example.cameracustom.saveImageCameraCustom;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.cameracustom.MainActivity;
import com.example.cameracustom.R;
import com.example.cameracustom.camerashow.MyDrawView;
import com.example.cameracustom.camerashow.ShowCamera;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

public class TransferImageActivity extends AppCompatActivity {


    private static final int RQS_IMAGE1 =1 ;
    private ImageView getImageCamera;
    private ImageView imageFrame;
    private ImageView imageSdCard;
    private ImageView imageCancel;
    private File folder;
    float rotate,x,y,scaleX,scaleY;
    int width,height;

    float scalediff;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private float oldDist = 1f;
    private float d = 0f;
    private float newRot = 0f;

    FrameLayout.LayoutParams parms;
    int startWidth;
    int startHeight;
    float dx = 0, dy = 0;
    float angle = 0;








    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_image);

        setupImages();



        String bitmap = this.getIntent().getStringExtra("Bitmap");

         rotate = this.getIntent().getFloatExtra("Rotate", 0.0f);
         width = this.getIntent().getIntExtra("Width", 0);
         height = this.getIntent().getIntExtra("height", 0);
         x = this.getIntent().getFloatExtra("1", 0.0f);
         y = this.getIntent().getFloatExtra("2", 0.0f);
         scaleX = this.getIntent().getFloatExtra("scale x", 0.0f);
         scaleY = this.getIntent().getFloatExtra("scale y", 0.0f);

        Bitmap bmp_result = ExifUtil.rotateBitmap(bitmap, BitmapFactory.decodeFile(bitmap));
        getImageCamera.setImageBitmap(bmp_result);




        setupClickedImage();

        imageFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final ImageView view = (ImageView) v;

                ((BitmapDrawable) view.getDrawable()).setAntiAlias(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:

                        parms = (FrameLayout.LayoutParams) view.getLayoutParams();
                        startWidth = parms.width;
                        startHeight = parms.height;
                        dx = event.getRawX() - parms.leftMargin;
                        dy = event.getRawY() - parms.topMargin;
                        mode = DRAG;
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        if (oldDist > 10f) {
                            mode = ZOOM;
                        }
                        d = rotation(event);
                        break;

                    case MotionEvent.ACTION_UP:
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;

                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {

                            x = event.getRawX();
                            y = event.getRawY();
                            parms.leftMargin = (int) (x - dx);
                            parms.topMargin = (int) (y - dy);
                            parms.rightMargin = 0;
                            parms.bottomMargin = 0;
                            parms.rightMargin = parms.leftMargin + (5 * parms.width);
                            parms.bottomMargin = parms.topMargin + (10 * parms.height);
                            view.setLayoutParams(parms);

                        } else if (mode == ZOOM) {

                            if (event.getPointerCount() == 2) {
                                newRot = rotation(event);
                                float r = newRot - d;
                                angle = r;

                                x = event.getRawX();
                                y = event.getRawY();

                                float newDist = spacing(event);
                                if (newDist > 10f) {

                                    float scale = newDist / oldDist * view.getScaleX();
                                    if (scale > 0.6) {
                                        scalediff = scale;
                                        view.setScaleX(scale);
                                        view.setScaleY(scale);
                                    }
                                }
                                view.animate().rotationBy(angle).setDuration(0).setInterpolator(new LinearInterpolator()).start();

                                x = event.getRawX();
                                y = event.getRawY();
                                parms.leftMargin = (int) ((x - dx) + scalediff);
                                parms.topMargin = (int) ((y - dy) + scalediff);
                                parms.rightMargin = 0;
                                parms.bottomMargin = 0;
                                parms.rightMargin = parms.leftMargin + (5 * parms.width);
                                parms.bottomMargin = parms.topMargin + (10 * parms.height);
                                view.setLayoutParams(parms);
                            }
                        }
                        break;
                }

                return true;

            }
        });

/*****setter on frame in activity*********************/

        imageFrame.setX(x);
        imageFrame.setY(y);
        imageFrame.setRotation(rotate);
        imageFrame.setScaleX(scaleX);
        imageFrame.setScaleY(scaleY);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);

        imageFrame.setLayoutParams(params);




    }


    public void setupImages() {
        getImageCamera = findViewById(R.id.iv_transfer_save);
        imageFrame = findViewById(R.id.iv_transfer_frame);
        imageSdCard = findViewById(R.id.iv_transfer_sdCard);
        imageCancel = findViewById(R.id.iv_transfer_cancel);
    }

    /*************save in sdCard or cancel take picture camera in activity*************/
    public void setupClickedImage() {
        imageCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startActivity(new Intent(getBaseContext(),MainActivity.class));
              finish();

            }
        });
        imageSdCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File pictureFile = getOutPutMediaFile();

                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(pictureFile);


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                Toast.makeText(TransferImageActivity.this,"عکس ذخیره شد",Toast.LENGTH_LONG).show();


            }
        });

    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private File getOutPutMediaFile() {
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return null;
        } else {
            folder = new File(Environment.getExternalStorageDirectory() + File.separator + "GUI");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String fileName = "image_" + String.valueOf(Calendar.getInstance().getTime().getTime()) + ".jpg";
            File outPutFile = new File(folder, fileName);
            return outPutFile;
        }
    }





}
