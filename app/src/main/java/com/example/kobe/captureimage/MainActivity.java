package com.example.kobe.captureimage;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;


import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;




public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button btnCapture;
    private Button btnSave;
    private LinearLayout imageViewLayout;
    private static String MY_LOG="MY_LOG";
    private static final int CODE_FOR_WRITE_PERMISSION = 339;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUiLayout();
        Log.d("KobeLog", "start");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CODE_FOR_WRITE_PERMISSION){
            if (permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    &&grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //用户同意使用write
                Log.d(MY_LOG, "Great , user confirm write permission ok");
                saveImage();
            }else{
                //用户不同意，自行处理即可
                finish();
            }
        }
    }

    private void setUiLayout(){
        imageView = (ImageView)findViewById(R.id.imageView);
        btnCapture = (Button) findViewById(R.id.BtnCapture);
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestHttpGetInBackgroumd();
            }
        });

        btnSave = (Button) findViewById(R.id.BtnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveImageWrapper();
            }
        });


        imageViewLayout = (LinearLayout)findViewById(R.id.imageViewLayout);
        imageViewLayout.post(new Runnable() {
            @Override
            public void run() {
                int w = imageViewLayout.getWidth();
                int h = w*9/16;//image 9:16
                Log.d(MY_LOG, "set image width="+w+",high="+h);
                imageViewLayout.setMinimumHeight(h);
            }
        });
    }

    void saveImage(){

        Log.d(MY_LOG, "save image start");
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap savedBitmap = drawable.getBitmap();
        if(savedBitmap != null){
            Date now = new Date();
            String datetime=new SimpleDateFormat("yyyyMMdd_HHmmss").format(now);
            Log.d(MY_LOG, "image title as dateTime="+datetime);
            String r = MediaStore.Images.Media.insertImage(getContentResolver(), savedBitmap, datetime , datetime);
            Log.d(MY_LOG, "insert image result="+r);
            Toast.makeText(MainActivity.this, "Saved Success ", Toast.LENGTH_SHORT).show();
        }
        else
            Log.d(MY_LOG,"imageview is null, nothing to do");
    }


    private void requestHttpGetInBackgroumd()
    {
        new Task(this).execute();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        Log.d(MY_LOG, "Config changed. maybe Rotate");
        super.onConfigurationChanged(newConfig);
    }

    private void saveImageWrapper() {
        int hasWriteContactsPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d(MY_LOG, "shouldShowRequestPermissionRationale is true");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        CODE_FOR_WRITE_PERMISSION);
                return;
            }

            Log.d(MY_LOG, "shouldShowRequestPermissionRationale is false");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    CODE_FOR_WRITE_PERMISSION);
            return;
        }
        saveImage();
    }

    public class Task extends AsyncTask<Void,Void,String> {

        private Bitmap  bitmap = null;
        private Context context;
        private ProgressDialog pDialog;
        private static final String IMAGE_GET_ADDRESS = "http://192.168.20.12:8080/reqimg";

        public Task(Context context){
            this.context = context;
        }
        @Override
        public void onPreExecute() {
            super.onPreExecute();
            //設定顯示訊息視窗
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        public String doInBackground(Void... arg0) {

            URL url = null;
            BufferedReader reader = null;
            StringBuilder stringBuilder;

            try
            {
                // create the HttpURLConnection
                url = new URL(IMAGE_GET_ADDRESS);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // 使用甚麼方法做連線
                connection.setRequestMethod("GET");
                Log.d(MY_LOG, "1");
                // 是否添加參數(ex : json...等)
                //connection.setDoOutput(true);

                // 設定TimeOut時間
                connection.setReadTimeout(3*1000);
                connection.connect();
                Log.d(MY_LOG, "2");
                // 伺服器回來的參數
                bitmap = BitmapFactory.decodeStream(connection.getInputStream());

            }
            catch (Exception e)
            {
                Log.e(MY_LOG, "error 1="+e.getMessage());
                e.printStackTrace();
            }
            finally
            {
                // close the reader; this can throw an exception too, so
                // wrap it in another try/catch block.
                if (reader != null)
                {
                    try
                    {
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        Log.e(MY_LOG, "error 2="+e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        public void onPostExecute(String result) {
            super.onPostExecute(result);

            if(bitmap == null){
                Toast.makeText(context, "Load image fail. Please check wift or server status", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(context, "Loaded Success ", Toast.LENGTH_SHORT).show();
                imageView.setImageBitmap(bitmap);
            }

          //imageView.setImageResource(R.drawable.test);
            if(pDialog != null){
                if(pDialog.isShowing()){
                    pDialog.dismiss();
                }
            }
        }
    }
}


