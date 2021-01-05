package com.pichs.app.xfilechooser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.pichs.common.widget.cardview.XCardButton;
import com.pichs.common.widget.utils.XTypefaceHelper;
import com.pichs.common.widget.view.XImageView;
import com.pichs.filechooser.ContentType;
import com.pichs.filechooser.FileChooser;
import com.pichs.filechooser.FileShare;

public class MainActivity extends AppCompatActivity {

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        XCardButton btn = findViewById(R.id.btn1);
        XCardButton share1 = findViewById(R.id.share1);
        XImageView iv1 = findViewById(R.id.iv1);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileChooser.get().with(MainActivity.this)
                        .authority(getPackageName() + "fileprovider")
                        .withCrop()
                        .gallery()
                        .listener(new FileChooser.OnFileChooseCallBack() {
                            @Override
                            public void onCallBack(Uri uri, Bitmap bitmap, String message) {
                                if (uri != null) {
                                    mUri = uri;
                                    iv1.setImageURI(uri);
                                }
                            }
                        })
                        .open();
            }
        });


        share1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileShare.with(MainActivity.this)
                        .addShareFileUri(mUri)
                        .setContentType(ContentType.IMAGE)
                        .setTextContent("谢谢你，这么的无私的支持我玩游戏")
                        .setTitle("阿里嘎都")
                        .forcedUseSystemChooser(true)
                        .setOnActivityResult(1901)
//                        .setTargetPackage()
//                        .setShareToComponent()
                        .build()
                        .share();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1810);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FileChooser.get().with(this).onActivityResult(requestCode, resultCode, data);
    }
}