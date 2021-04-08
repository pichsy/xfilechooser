# 最新地址, 该项目已移到gitee
# [https://gitee.com/pichs/xfilechooser](https://gitee.com/pichs/xfilechooser)

# XFileChooser

xfilechooser 系统分享和文件选择的封装，兼容多多
FileUriUtils 可兼容获取真实路径。

### 引入

      
    implementation 'com.github.pichsy:xfilechooser:1.0'
    
    ...


### 用法

    ```
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
                            .setTextContent("谢谢你，给个star好吗？")
                            .setTitle("你是好人")
                            .forcedUseSystemChooser(true)
                            .setOnActivityResult(1901)
                            // .setTargetPackage("com.twitter.android")
                            // .setShareToComponent("com.twitter.android","xxx.xxxActivity")
                            .build()
                            .share();
                }
            });
    
    
    ```
