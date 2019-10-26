package com.example.dx.xposedhook;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {

    TextView text_hookstatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_hookstatus = findViewById(R.id.hookstatus);
        if (HookMe.isHooked()) {
            text_hookstatus.setText("模块已加载");
            text_hookstatus.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }
}

