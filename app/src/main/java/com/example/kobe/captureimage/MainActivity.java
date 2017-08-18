package com.example.kobe.captureimage;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;


import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;




public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Button btnCapture;
    private Button btnSave;
    private LinearLayout imageViewLayout;
    private EditText editText;

    private static final String DEFAULT_IP = "192.168.20.12";
    private static String MY_LOG="MY_LOG";
    private static final int CODE_FOR_WRITE_PERMISSION = 339;
    WifiManager.MulticastLock lock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUiLayout();
        WifiManager wifiManager = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        lock = wifiManager.createMulticastLock("wifitest");
        lock.setReferenceCounted(true);
        lock.acquire();
        receiveUDPMulticast();
        Log.d("KobeLog", "start");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(MY_LOG, "onDestroy");
        if(lock!=null){
            Log.d(MY_LOG, "wifi multicast lock release ");
            lock.release();
        }
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
                int w = imageView.getWidth();
                int h = w*9/16;//image 9:16
                Log.d(MY_LOG, "set image width="+w+",high="+h);
                imageView.setMinimumHeight(h);
            }
        });


        editText = (EditText) findViewById(R.id.etBannedIpAddress);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) + source.subSequence(start, end) + destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        editText.setFilters(filters);
        editText.setText(DEFAULT_IP);

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

    private void receiveUDPMulticast(){

        final String groupIP = "224.1.1.1";
        final int groupPort = 4321;


        Thread thread = new Thread(){
            @Override
            public void run() {

                MulticastSocket s = null;

                try {
                    Log.d(MY_LOG, "multicast subThread start");
                    String msg = "Hello";
                    // 1
                    s = new MulticastSocket(groupPort);
                    // 2
                    InetAddress group = InetAddress.getByName(groupIP);
                    if(group.isMulticastAddress()==false){
                        Log.e(MY_LOG, "IP address is not in 224.0.0.0~239.255.255.255");
                        return;
                    }
                    s.joinGroup(group);
                    //DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(),
                    //        group, groupPort);
                    //s.send(hi);

                    while(true) {
                        // get their responses!
                        byte[] buf = new byte[1024*8];
                        DatagramPacket recv = new DatagramPacket(buf, buf.length);
                        s.receive(recv);
                        //Log.d(MY_LOG, "len="+recv.getLength());

                        if(recv.getLength() == 6){
                            // header
                            Log.d(MY_LOG, String.format(">>>>> header: (%02x)(%02x)(%02x)(%02x)(%02x)(%02x)", buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]));

                        }
                        else {
                            //String result = new String(buf, recv.getOffset(), recv.getLength());
                            Log.d(MY_LOG, "===== data receive len(" + recv.getLength() + "), offset(" + recv.getOffset() + ")");
                        }
                    }
                }catch (IOException ioex){

                    Log.e(MY_LOG, "multicast error 1:"+ ioex.getMessage());
                }

                finally {
                    if(s != null) {
                        s.close();
                        lock.release();
                    }
                }
            }
        };

        thread.start();
    }

    public class Task extends AsyncTask<Void,Void,String> {

        private Bitmap  bitmap = null;
        private Context context;
        private ProgressDialog pDialog;
        private String IMAGE_GET_ADDRESS = "http://"+editText.getText()+":8080/reqimg";
        //private String IMAGE_GET_ADDRESS = "http://192.168.20.12:8080/reqimg";


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
                Log.d(MY_LOG, "IP address="+IMAGE_GET_ADDRESS);
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


