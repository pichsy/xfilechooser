package com.pichs.app.xfilechooser;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.pichs.common.widget.cardview.XCardButton;
import com.pichs.common.widget.utils.XTypefaceHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.tv1);
        XCardButton btn = findViewById(R.id.btn1);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XTypefaceHelper.setGlobalTypefaceFromAssets(getApplicationContext(), "leihong.ttf");
                XTypefaceHelper.setGlobalTypefaceStyle(getApplicationContext(), XTypefaceHelper.BOLD);
            }
        });




    }
}