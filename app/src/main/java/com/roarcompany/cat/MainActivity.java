package com.roarcompany.cat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kakao.sdk.common.KakaoSdk;
import com.facebook.FacebookSdk;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import bolts.AppLinks;
import retrofit2.http.Tag;

public class MainActivity extends AppCompatActivity {

    private ValueCallback<Uri> filePathCallbackNormal;
    private ValueCallback<Uri[]> filePathCallbackLollipop;
    private final static int FILECHOOSER_NORMAL_REQ_CODE = 2001;
    private final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2002;
    private Uri cameraImageUri = null;
    private long time = 0;
    private WebView webView;
    private TextView textView;

    Boolean check = true;

    private static final String TAG = "CATJARANG";
    private static final String SEGMENT_CHECK = "check";
    private static final String KEY_CODE = "key";

    final public Handler handler = new Handler();


    public static final String USER_AGENT = "Mozilla/5.0 53(Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/5.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19";

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri facebook = getIntent().getData();
        webView.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);
        textView.setText("targetUrl : "+facebook);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        textView  = findViewById(R.id.textView);
        webView = (WebView) findViewById(R.id.webView);

        String ori_userAgent = webView.getSettings().getUserAgentString(); //userAgent ????????????
        WebSettings webSettings = webView.getSettings();

        getDeepLink();

        webView.setDownloadListener(new MyWebViewClient());

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);


        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }

        int status = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(status == NetworkStatus.TYPE_NOT_CONNECTED){ //???????????? ?????? ??????
            webView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
            textView.setText("??????????????? ???????????? ?????? ????????????.\n?????? ????????? ??????????????????.");
        }else {
            webView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
            KakaoSdk.init(this, "86fd7ab8a88bb250053735e63c0af81e");

            webView.setWebViewClient(new WebViewClient() {
                public boolean shouldOverrideUrlLoading(WebView view, String url) {

                    if(url.equals("http://test.ucat-dev.com/")){
                        webView.clearHistory();
                    }

                    if(url != null && url.startsWith("https://accounts.google.com")){
                        webSettings.setUserAgentString(USER_AGENT); //?????? ???????????? ??? userAgent ?????????
                    }else{
                        webSettings.setUserAgentString(ori_userAgent); //?????? ????????? ?????? ???????????? ????????? ??? ?????? agent??? ?????????
                    }

                    if (url != null && url.startsWith("intent://")) {
                        try {
                            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                            if (existPackage != null) {
                                startActivity(intent);
                            } else {

                                Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                                marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()));
                                startActivity(marketIntent);
                            }
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (url != null && url.startsWith("market://")) {
                        try {
                            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                            if (intent != null) {
                                startActivity(intent);
                            }
                            return true;
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    } else if (url != null && url.startsWith("intent:")) {
                        final int customUrlStartIndex = "intent:".length();
                        final int customUrlEndIndex = url.indexOf("#Intent;");
                        if(customUrlEndIndex < 0){
                            return false;
                        }else {
                            final String customUrl = url.substring(customUrlStartIndex, customUrlEndIndex);
                            try{
                                Intent customIntent = new Intent(Intent.ACTION_VIEW);
                                customIntent.setData(Uri.parse(customUrl));
                                startActivity(customIntent);
                            }catch (ActivityNotFoundException e){
                                final int packagesStartIndex = customUrlEndIndex + "#Intent;".length();
                                final int packagesEndIndex = url.indexOf(";end;");
                                final String packageName = url.substring(packagesStartIndex, packagesEndIndex < 0 ? url.length() : packagesEndIndex);

                                Intent packageIntent = new Intent(Intent.ACTION_VIEW);
                                packageIntent.setData(Uri.parse("market://details?id=" + packageName));
                                startActivity(packageIntent);
                            }
                            return true;
                        }
                    }
                    view.loadUrl(url);
                    return false;

                }
            });

            webView.setWebChromeClient(new WebChromeClient() {

                @Override
                public Bitmap getDefaultVideoPoster() {
                    return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
                }

                // ????????????????????? alert???
                @Override
                public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("??????")
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok,
                                    (dialog, which) -> result.confirm())
                            .setCancelable(false)
                            .create()
                            .show();
                    return true;
                }

                // ????????????????????? confirm???
                @Override
                public boolean onJsConfirm(WebView view, String url, String message,
                                           final JsResult result) {
                    new AlertDialog.Builder(view.getContext())
                            .setTitle("??????")
                            .setMessage(message)
                            .setPositiveButton("Yes",
                                    (dialog, which) -> result.confirm())
                            .setNegativeButton("No",
                                    (dialog, which) -> result.cancel())
                            .setCancelable(false)
                            .create()
                            .show();
                    return true;
                }


                public boolean onJsPrompt (WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                    EditText et = new EditText(view.getContext());

                    new AlertDialog.Builder(view.getContext())
                            .setTitle("??????")
                            .setMessage(message)
                            .setView(et)
                            .setPositiveButton("??????", (dialog, which) -> result.confirm(et.getText().toString()))
                            .setNegativeButton("??????",
                                    (dialog, which) -> result.cancel())
                            .setCancelable(false)
                            .create()
                            .show();
                    return true;
                }

                // For Android 5.0+
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                public boolean onShowFileChooser(
                        WebView webView, ValueCallback<Uri[]> filePathCallback,
                        WebChromeClient.FileChooserParams fileChooserParams) {
                    Log.d("MainActivity", "5.0+");

                    if(filePathCallbackLollipop  != null){
                        filePathCallbackLollipop.onReceiveValue(null);
                        filePathCallbackLollipop = null;
                    }
                    filePathCallbackLollipop = filePathCallback;

                    boolean isCapture = fileChooserParams.isCaptureEnabled();
                    runCamera(isCapture);
                    return true;
                }
            });

            checkVerify(); //?????? ?????? ??????


            webSettings.setJavaScriptEnabled(true); //?????????????????? ??????
            webSettings.setDomStorageEnabled(true);  // ?????? ????????? ??????
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //?????????????????? ?????? ????????? ??????
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setSupportMultipleWindows(true); //?????? ????????? ??????
            webSettings.setUseWideViewPort(true); //?????? ????????? ????????? ??????
            webSettings.setLoadWithOverviewMode(true); // ???????????? ??????
            webSettings.setCacheMode(webSettings.LOAD_NO_CACHE); //???????????? ?????????
            webSettings.setDomStorageEnabled(true); //??????????????? ??????
            webSettings.setSaveFormData(true);
            webSettings.setTextZoom(100);


            AndroidBridge androidBridge = new AndroidBridge(webView, MainActivity.this);
            webView.addJavascriptInterface(androidBridge, "Android" );


            if (Build.VERSION.SDK_INT >= 21) {
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }

            Intent intent = getIntent();
            String u = intent.getStringExtra("test");            if(u != null ){
                webView.loadUrl(u);
            }else {
                webView.loadUrl("https://test.ucat-dev.com");
            }

        }

        FirebaseMessaging.getInstance().subscribeToTopic("ALL");

    }

    private void getDeepLink(){
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
//                            webView.setVisibility(View.GONE);
//                            textView.setVisibility(View.VISIBLE);
//                            textView.setText("deepLink : "+deepLink);
                            webView.loadUrl(deepLink.toString());
                        }else{
                            if(getIntent().getData() != null){
                                Uri facebook = getIntent().getData();
                                String shareUrl = facebook.getQueryParameter("uri");
                                String shareIdx = facebook.getQueryParameter("shareIdx");
                                webView.loadUrl(shareUrl+"?idx="+shareIdx+"&app=1");
                            }
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getDynamicLink:onFailure", e);
                    }
                });
    }

    //??????????????? ????????? ??? ????????? ?????? ????????? ????????? ??? ??????
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult()", "data = " + data);
        Log.d("onActivityResult() ","resultCode = " + Integer.toString(requestCode));

        switch (requestCode)
        {
            case FILECHOOSER_NORMAL_REQ_CODE:
                if (resultCode == RESULT_OK)
                {
                    if (filePathCallbackNormal == null) return;
                    Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                    filePathCallbackNormal.onReceiveValue(result);
                    filePathCallbackNormal = null;
                }
                break;
            case FILECHOOSER_LOLLIPOP_REQ_CODE:
                if (resultCode == RESULT_OK)
                {
                    if (filePathCallbackLollipop == null) return;
                    if (data == null)
                        data = new Intent();
                    if (data.getData() == null)
                        data.setData(cameraImageUri);

                    if(data.getClipData() != null){
                       int count = data.getClipData().getItemCount();
                       Uri[] uris = new Uri[count];
                       for(int i = 0; i < count; i++){
                           uris[i] = data.getClipData().getItemAt(i).getUri();
                       }
                       Log.d("onActivityResult()", "uris : " + uris);
                       filePathCallbackLollipop.onReceiveValue(uris);
                    }else if(data.getData() != null){
                        filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    }

                    filePathCallbackLollipop = null;
                }
                else
                {
                    if (filePathCallbackLollipop != null)
                    {
                        filePathCallbackLollipop.onReceiveValue(null);
                        filePathCallbackLollipop = null;
                    }

                    if (filePathCallbackNormal != null)
                    {
                        filePathCallbackNormal.onReceiveValue(null);
                        filePathCallbackNormal = null;
                    }
                }
                break;
            default:

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    //????????? ?????? ??????
    private void runCamera(boolean _isCapture){

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        File path = getFilesDir();
        File file = new File(path, "captureImg.jpg");
        // File ????????? URI ??? ?????????.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
        {
            cameraImageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);
        }
        else
        {
            cameraImageUri = Uri.fromFile(file);
        }
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        if (!_isCapture)
        { // ???????????? ?????????, ????????? ?????? ????????? ?????? ???..
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            //pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickIntent.setType("image/* video/*");

            String pickTitle = "????????????";
            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);

            // ????????? intent ???????????????..
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{intentCamera});
            startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
        else
        {// ?????? ????????? ??????..
            startActivityForResult(intentCamera, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//
//        if ((keyCode==KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
//            webView.goBack();
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//    }


    @Override
    public void onBackPressed(){
        //https://ucat-dev.com/Login/main
        if(webView.getUrl().equals("https://test.ucat-dev.com/home") || webView.getUrl().equals("https://test.ucat-dev.com/")) {
            if (System.currentTimeMillis() - time >= 2000) {
                time = System.currentTimeMillis();
                Toast.makeText(getApplicationContext(), "?????? ????????? ?????? ??? ????????? ???????????????.", Toast.LENGTH_SHORT).show();
            } else if (System.currentTimeMillis() - time < 2000) {
                finish();
                return;
            }
        }else{
            Log.d("fin", "?????? 333");
            webView.goBack();
        }
    }

    //?????? ?????? ?????? ??????
    @TargetApi(Build.VERSION_CODES.M)
    public void checkVerify() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            Log.d("checkVerify() : ","if??? ?????????");

            //????????? ?????? ???????????? ?????? ?????? ?????? ??????
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.CAMERA)) {

                Toast.makeText(getApplicationContext(),"?????? ?????? ????????? ????????? ????????? ????????? ??????????????? ???????????? ???????????? ?????????????????????.",Toast.LENGTH_SHORT).show();

            } else {
//                Log.d("checkVerify() : ","????????? ??? ???????????? ?????? ??????");
                // ????????? ??? ???????????? ?????? ??????
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.INTERNET, Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    //?????? ?????? ????????? ?????? ?????? ??????
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //Log.d("onRequestPermissionsResult() : ","?????????");

        if (requestCode == 1)
        {
            if (grantResults.length > 0)
            {
                for (int i=0; i<grantResults.length; ++i)
                {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    {
                        // ?????????, ????????? ??? ???????????? ??????????????? ????????? ?????? ????????? ??????
                        new AlertDialog.Builder(this).setTitle("??????").setMessage("????????? ?????????????????? ?????? ????????? ??? ????????????.")
                                .setPositiveButton("??????", (dialog, which) -> {
                                    dialog.dismiss();
                                    finish();
                                }).setNegativeButton("?????? ??????", (dialog, which) -> {
                                    dialog.dismiss();
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                    getApplicationContext().startActivity(intent);
                                }).setCancelable(false).show();

                        return;
                    }
                }
//                Toast.makeText(this, "Succeed Read/Write external storage !", Toast.LENGTH_SHORT).show();

            }
        }
    }

    private class MyWebViewClient extends WebViewClient implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
            Log.d(TAG, "***** onDownloadStart()");

            Log.d(TAG,"***** onDownloadStart() - url : "+url);
            Log.d(TAG,"***** onDownloadStart() - userAgent : "+userAgent);
            Log.d(TAG,"***** onDownloadStart() - contentDisposition : "+contentDisposition);
            Log.d(TAG,"***** onDownloadStart() - mimeType : "+mimeType);

            if (url.startsWith("data:")) {  //when url is base64 encoded data
                String path = createAndSaveFileFromBase64Url(url);
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            String cookies = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("cookie", cookies);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("File downloading...");
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
            request.setTitle(filename);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), "????????? ?????????????????????", Toast.LENGTH_LONG).show();
        }


    }

    public String createAndSaveFileFromBase64Url(String url) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
        String filename = System.currentTimeMillis() + "." + filetype;
        File file = new File(path, filename);
        try {
            if(!path.exists())
                path.mkdirs();
            if(!file.exists())
                file.createNewFile();

            String base64EncodedString = url.substring(url.indexOf(",") + 1);
            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
            OutputStream os = new FileOutputStream(file);
            os.write(decodedBytes);
            os.close();
            ???
            Toast.makeText(getApplicationContext(), "???????????? ??????", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
            Toast.makeText(getApplicationContext(), "???????????? ??????", Toast.LENGTH_LONG).show();
        }

        return file.toString();
    }

}