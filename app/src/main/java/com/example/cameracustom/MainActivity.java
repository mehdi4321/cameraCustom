package com.example.cameracustom;


import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.cameracustom.camerashow.ShowCamera;
import com.example.cameracustom.saveImageCameraCustom.TransferImageActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Camera camera;
    private FrameLayout myFrame;
    private ShowCamera showCamera;
    private ImageView imageFrame = null;
    private ImageView clickCamera;
    private File folder;
    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;

    float scalediff;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private float oldDist = 1f;
    private float d = 0f;
    private float newRot = 0f;

    RelativeLayout.LayoutParams parms;
    int startWidth;
    int startHeight;
    float dx = 0, dy = 0, x = 0, y = 0;
    float angle = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViews();

/******how place image on mainactivity in camera**************/
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(200, 200);
        layoutParams.leftMargin = 100;
        layoutParams.topMargin = 100;
        imageFrame.setLayoutParams(layoutParams);


/********how rotate and zoom and move image frame on camera********/
        imageFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final ImageView view = (ImageView) v;

                ((BitmapDrawable) view.getDrawable()).setAntiAlias(true);
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:

                        parms = (RelativeLayout.LayoutParams) view.getLayoutParams();
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



/********take a picture and send imageframe to activity transfer by rotate and scale and move... and save image in file*******/
        final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File pictureFile = getOutPutMediaFile();


                if (pictureFile == null) {
                    return;
                } else {
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(pictureFile);
                        fileOutputStream.write(data);
                        fileOutputStream.close();

                        camera.startPreview();
                        if (pictureFile.exists()) {

                            int width = imageFrame.getLayoutParams().width;
                            int height = imageFrame.getLayoutParams().height;
                            Intent intent = new Intent(MainActivity.this, TransferImageActivity.class);
                            intent.putExtra("Bitmap", pictureFile.getAbsolutePath());
                            intent.putExtra("Rotate", imageFrame.getRotation());
                            intent.putExtra("Width", width);
                            intent.putExtra("height", height);
                            intent.putExtra("scale x", imageFrame.getScaleX());
                            intent.putExtra("scale y", imageFrame.getScaleY());
                            intent.putExtra("1", imageFrame.getX());
                            intent.putExtra("2", imageFrame.getY());
                            startActivity(intent);
                            finish();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        rawCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("Log", "onPictureTaken - raw");
            }
        };
        shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                Log.i("Log", "onShutter'd");
            }
        };
        clickCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        if (camera != null) {
                            camera.takePicture(shutterCallback, rawCallback, pictureCallback);

                        }
                    }
                });


            }
        });

 /*************show camera on activity********/
        camera=Camera.open();

        showCamera = new ShowCamera(this, camera);
        myFrame.addView(showCamera);
    }

    public void setupViews() {
        myFrame = findViewById(R.id.fl_main_frameLayout);
        imageFrame = findViewById(R.id.iv_main_frame);
        clickCamera = findViewById(R.id.iv_main_clickCamera);
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
