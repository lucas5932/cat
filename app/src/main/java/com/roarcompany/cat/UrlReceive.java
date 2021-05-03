package com.roarcompany.cat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class UrlReceive extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receiveurl);

        Intent intent = getIntent();
        String s = intent.getStringExtra("test");
        Log.d("ss",s);

        Intent intent2 = new Intent(this, MainActivity.class);
        intent2.putExtra("test",s);
        startActivity(intent2);
    }
}
