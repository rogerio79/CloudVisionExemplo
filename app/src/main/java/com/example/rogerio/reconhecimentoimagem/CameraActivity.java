package com.example.rogerio.reconhecimentoimagem;


//import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
/**
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
 **/
//import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

public class CameraActivity extends AppCompatActivity {

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private Camera mCamera;
    private CameraPreview mPreview;

    // variavel Cloud Vision
    private TextView mImageDetails;
    private ImageView mMainImage;
    private static final String CLOUD_VISION_API_KEY = "chave aqui";


    TextView texto;

    //  variaveis Cloud Vision

    // adicionar fragmento
    //FragmentManager fragmentManager;
    //FotoFragment frag = new FotoFragment();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        texto = (TextView)findViewById(R.id.textView);
        texto.setMovementMethod(new ScrollingMovementMethod());

        //fragmentManager = getSupportFragmentManager();
        //fragmentManager.beginTransaction().add(R.id.fragmento_viewer,frag).commit();

        Log.e("LOG e","preview");
        // criar instancia camera
        mCamera = getCameraInstance();
        mCamera.setDisplayOrientation(90);

        Button captureButton = (Button) findViewById(R.id.button_capture);

        // cria preview e seta na activity
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        try {
            Log.e("LOG e","preview");
            assert preview != null;

            preview.addView(mPreview);
        } catch(Exception e){
            Log.e("LOG e","preview");
        }


        assert captureButton != null;
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // pegar image da camera
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );
    }

    public void setaTexto(File arq) {
        ImageView miniatura;
        miniatura = (ImageView) findViewById(R.id.imageView);

        int width=miniatura.getWidth();
        int height=miniatura.getHeight();

        //File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        texto.setText(arq.getPath());

        Bitmap ThumbImage = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(arq.getPath()), width, height);
        ///URI caminho = arq.toURI();
        miniatura.setImageBitmap(ThumbImage);
       // Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), caminho);

        //Bitmap mImageBitmap = BitmapFactory.decodeFile(arq);
        //Bitmap scaled = Bitmap.createScaledBitmap(mImageBitmap, 100, 100, true);
        //mImageView.setImageBitmap(scaled);



        try {
            callCloudVision(ThumbImage);
        } catch (IOException e) {
            e.printStackTrace();
        }



    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            // erro
        }
        return c; // pode ser null se nao der para abrir
    }



    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // SurfaceHolder.Callback

            mHolder = getHolder();
            mHolder.addCallback(this);

            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            //  Surface criada
            try {

                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();

                mCamera.autoFocus(new Camera.AutoFocusCallback() {



                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        Toast.makeText(CameraActivity.this,"focado",Toast.LENGTH_SHORT).show();


                    }
                });
            } catch (IOException e) {
               // Log.d(TAG, "erro no preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.release();
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {


            if (mHolder.getSurface() == null){
                return;
            }


            try {
                mCamera.stopPreview();
            } catch (Exception e){

            }


            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
                //Toast.makeText(CameraActivity.this,"mudou",Toast.LENGTH_SHORT).show();

            } catch (Exception e){
                //Log.d(TAG, "erro iniciando preview: " + e.getMessage());
            }
        }
    }



    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                //Log.d(TAG, "Error creating media file, check storage permissions: " +
                  //      e.getMessage());
                Toast.makeText(CameraActivity.this,"pictureFile == null"+ Environment.getExternalStorageState(),Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                //Log.d(TAG, "File not found: " + e.getMessage());
                Toast.makeText(CameraActivity.this,"erro 2"+e.getMessage(),Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                //Log.d(TAG, "Error accessing file: " + e.getMessage());
                Toast.makeText(CameraActivity.this,"erro 3",Toast.LENGTH_SHORT).show();
            }

            //mCamera.release();
            //mCamera.stopPreview();
            //frag.setaImagem("eeeeuuuuuuu", frag.texto);
            setaTexto(pictureFile);
            mCamera.startPreview();
                        // *********************
        }


    };

    private static File getOutputMediaFile(int type){
        // funcao site android developer
       // if (Environment.getExternalStorageState() ) {

        //}

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(
                ), "DCIM/Camera");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.



        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }

 //  CLOUD VISION FUNCOES - site android developer
     public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

         int originalWidth = bitmap.getWidth();
         int originalHeight = bitmap.getHeight();
         int resizedWidth = maxDimension;
         int resizedHeight = maxDimension;

         if (originalHeight > originalWidth) {
             resizedHeight = maxDimension;
             resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
         } else if (originalWidth > originalHeight) {
             resizedWidth = maxDimension;
             resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
         } else if (originalHeight == originalWidth) {
             resizedHeight = maxDimension;
             resizedWidth = maxDimension;
         }
         return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
     }


/**
    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                1200);

                callCloudVision(bitmap);
                mMainImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                //Log.d(TAG, "Image picking failed because " + e.getMessage());
                //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            //Log.d(TAG, "Image picker gave us a null image.");
            //Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }
 **/

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Switch text to loading
        texto.setText(R.string.loading_message);

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(new
                            VisionRequestInitializer(CLOUD_VISION_API_KEY));
                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    //Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return convertResponseToString(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d("TAG", "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    Log.d("TAG", "failed to make API request because of other IOException " +
                           e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {
                texto.setText(result); // meu campo de texto de saida
            }
        }.execute();
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = "I found these things:\n\n";

        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message += String.format("%.3f: %s", label.getScore(), label.getDescription());
                message += "\n";
            }
        } else {
            message += "nothing";
        }

        return message;
    }




} //  fim CameraActivity
