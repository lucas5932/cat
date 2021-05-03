package com.roarcompany.cat;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.MODE_PRIVATE;


public class AndroidBridge {
    private String TAG = "AndroidBridge";
    final public Handler handler = new Handler();

    private WebView mAppView;
    private MainActivity mContext;
    private long time = 0;

    //기본 생성자
    public AndroidBridge(WebView _mAppView, MainActivity _mContext) {
        mAppView = _mAppView;
        mContext = _mContext;
    }

    @JavascriptInterface
    public void call_log(final String _message){
        Log.d(TAG, _message);

        handler.post(new Runnable() {
            @Override
            public void run() {
                mAppView.loadUrl("javascript:alert('["+_message+"] 라고 로그를..');");
            }
        });
    }

    @JavascriptInterface
    public void getToken(String idx){

        SharedPreferences pref = mContext.getSharedPreferences("pref", MODE_PRIVATE);
        String token =  pref.getString("token", "");
        Log.d(TAG, token);

        handler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "토큰 전송");
                mAppView.loadUrl("javascript:tokenPocket('"+token+"','"+idx+"');");
            }
        });
    }

    @JavascriptInterface
    public void pageFinish(){

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - time >= 2000) {
                    time = System.currentTimeMillis();
                    Toast.makeText(mContext, "뒤로 버튼을 한번 더 누르면 종료합니다.", Toast.LENGTH_SHORT).show();
                    Log.d("fin","뒤로 1");
                } else if (System.currentTimeMillis() - time < 2000) {
                    mContext.finish();
                    Log.d("fin", "뒤로 2");
                    return;

                }
            }
        });
    }

    @JavascriptInterface
    public void clipBoard(String idx){
        ClipboardManager clipboard = (ClipboardManager)mContext.getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("link", "https://roarcompany.page.link/?link=https://ucat-dev.com/Board/viewPage?idx="+idx+"&apn=com.roarcompany.cat");
        clipboard.setPrimaryClip(clip);

        Toast.makeText(mContext, "링크가 복사되었습니다.", Toast.LENGTH_LONG).show();

    }
}
