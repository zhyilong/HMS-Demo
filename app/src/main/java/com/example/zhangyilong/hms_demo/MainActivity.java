package com.example.zhangyilong.hms_demo;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.hw_push).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.hw_push:
                Intent pushIntent = new Intent();
                pushIntent.setClass(this, HuaweiPushActivity.class);
                pushIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(pushIntent);
                break;


            default:
                break;
        }
    }
}
