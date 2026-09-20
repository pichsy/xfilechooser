package com.pichs.app.xfilechooser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.pichs.common.widget.cardview.XCardButton;
import com.pichs.common.widget.view.XImageView;
import com.pichs.filechooser.ContentType;
import com.pichs.filechooser.FileChooseUriUtils;
import com.pichs.filechooser.FileChooser;
import com.pichs.filechooser.FileShare;

public class MainActivity extends AppCompatActivity {

    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final XImageView ivPreview = findViewById(R.id.iv1);
        final TextView tvResult = findViewById(R.id.tvResult);
        XCardButton btnPick = findViewById(R.id.btn1);
        XCardButton btnShare = findViewById(R.id.share1);

        // 选择图片：结果自动回调，无需 onActivityResult
        btnPick.setOnClickListener(v ->
                FileChooser.get().with(MainActivity.this)
                        .gallery()
                        .asBitmap()
                        .listener((uri, bitmap, message) -> {
                            if (uri != null) {
                                mUri = uri;
                                // 优先展示 Bitmap，其次展示 Uri
                                if (bitmap != null) {
                                    ivPreview.setImageBitmap(bitmap);
                                } else {
                                    ivPreview.setImageURI(uri);
                                }
                                String realPath = FileChooseUriUtils.getFileRealPath(MainActivity.this, uri);
                                tvResult.setText("message: " + message + "\nrealPath: " + realPath);
                            } else {
                                tvResult.setText("result: " + message);
                            }
                        })
                        .open());

        // 分享已选择的图片
        btnShare.setOnClickListener(v -> {
            if (mUri == null) {
                Toast.makeText(MainActivity.this, "请先选择一张图片", Toast.LENGTH_SHORT).show();
                return;
            }
            FileShare.with(MainActivity.this)
                    .addShareFileUri(mUri)
                    .setContentType(ContentType.IMAGE)
                    .setTextContent("分享一张图片")
                    .setTitle("分享")
                    .forcedUseSystemChooser(true)
                    .build()
                    .share();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1810);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
