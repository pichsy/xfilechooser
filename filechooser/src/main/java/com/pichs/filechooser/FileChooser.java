package com.pichs.filechooser;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * // 打开相册，做权限判断
 * FileChooser.get()
 * .with(context)
 * .asBitmap()
 * //.authority()
 * .listener(callback)
 * .gallery()
 * .open();
 */
@SuppressWarnings("ALL")
public class FileChooser {

    private Map<Activity, FileChooserBuilder> mActivityFileChooserBuilderMap = new WeakHashMap<>();
    private Map<Fragment, FileChooserBuilder> mFragmentFileChooserBuilderMap = new WeakHashMap<>();

    private FileChooser() {
    }

    private static FileChooser INSTANCE = new FileChooser();

    public static FileChooser get() {
        return INSTANCE;
    }

    public FileChooserBuilder with(Activity activity) {
        if (mActivityFileChooserBuilderMap.containsKey(activity)) {
            return mActivityFileChooserBuilderMap.get(activity);
        }
        FileChooserBuilder builder = new FileChooserBuilder(activity);
        mActivityFileChooserBuilderMap.put(activity, builder);
        return builder;
    }

    public FileChooserBuilder with(Fragment fragment) {
        if (mFragmentFileChooserBuilderMap.containsKey(fragment)) {
            return mFragmentFileChooserBuilderMap.get(fragment);
        }
        FileChooserBuilder builder = new FileChooserBuilder(fragment);
        mFragmentFileChooserBuilderMap.put(fragment, builder);
        return builder;
    }


    public void onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
        FileChooserBuilder fileChooserBuilder = mActivityFileChooserBuilderMap.get(activity);
        if (fileChooserBuilder != null) {
            fileChooserBuilder.onActivityResult(requestCode, resultCode, data);
        }
    }


    public void onActivityResult(Fragment fragment, int requestCode, int resultCode, @Nullable Intent data) {
        FileChooserBuilder fileChooserBuilder = mFragmentFileChooserBuilderMap.get(fragment);
        if (fileChooserBuilder != null) {
            fileChooserBuilder.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 释放资源和对象
     */
    public void release(Activity activity) {
        try {
            mActivityFileChooserBuilderMap.remove(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放资源和对象
     */
    public void release(Fragment fragment) {
        try {
            mFragmentFileChooserBuilderMap.remove(fragment);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //////////////////////callback interface///////////////////////////////////
    public interface OnFileChooseCallBack {
        void onCallBack(Uri uri, Bitmap bitmap, String message);
    }

    public static class FileChooserBuilder {

        private static final String SUFFIX_FILE_NAME_CAMERA = "_CAMERA_IMG.jpg";
        private static final String SUFFIX_FILE_NAME_CROP = "_CROP_IMG.jpg";
        private static final int FILE_CHOOSER_GALLERY_REQUEST_CODE = 102;
        private static final int FILE_CHOOSER_CAMERA_REQUEST_CODE = 103;
        private static final int FILE_CHOOSER_CROP_REQUEST_CODE = 104;
        private static final int FILE_CHOOSER_FILE_REQUEST_CODE = 105;
        private final static List<String> JEPG_SUFFIX_LIST = Arrays.asList("jpg", "png", "jpeg");
        // 0 代表activity请求，其他表示fragment
        private int type;
        private int chooseType = 0;
        private WeakReference<Activity> mActivityWeakReference;
        private WeakReference<Fragment> mFragmentWeakReference;
        private WeakReference<Context> mContextWeakReference;

        private int chooser_requestCode_gallery = FILE_CHOOSER_GALLERY_REQUEST_CODE;
        private int chooser_requestCode_camera = FILE_CHOOSER_CAMERA_REQUEST_CODE;
        private int chooser_requestCode_crop = FILE_CHOOSER_CROP_REQUEST_CODE;
        private int chooser_requestCode_file = FILE_CHOOSER_FILE_REQUEST_CODE;
        private OnFileChooseCallBack mFileChooseCallBack;
        private Uri mCameraOutputUri;
        private File mCropFile;
        private boolean asBitmap = false;
        private boolean isCrop = false;
        private int mCropWidth = 200;
        private String authority;

        public FileChooserBuilder(Activity activity) {
            this.type = 0;
            this.mActivityWeakReference = new WeakReference<>(activity);
            this.mContextWeakReference = new WeakReference<Context>(activity);
            authority = mActivityWeakReference.get().getPackageName() + ".fileprovider";
        }

        public FileChooserBuilder(Fragment fragment) {
            this.type = 1;
            this.mFragmentWeakReference = new WeakReference<>(fragment);
            this.mContextWeakReference = new WeakReference<>(fragment.getContext());
            authority = mContextWeakReference.get().getPackageName() + ".fileprovider";
        }

        /**
         * 用来判断是Activity中调用的，还是Fragment中调用的
         *
         * @return
         */
        public int getType() {
            return type;
        }

        /**
         * 是否启用剪裁，不启用则会返回选择的图片，或拍照图片
         */
        public FileChooserBuilder withCrop() {
            isCrop = true;
            return this;
        }

        /**
         * @param width 剪裁宽度，默认200，正方形，可自己设置（这个只支持正方形剪裁）
         *              有其他剪裁功能请自行解决
         */
        public FileChooserBuilder cropWidth(int width) {
            this.mCropWidth = width;
            return this;
        }

        /**
         * 是否作为bitmap返回，调用此方法，则会返回bitmap
         */
        public FileChooserBuilder asBitmap() {
            asBitmap = true;
            return this;
        }

        /**
         * 设置目的：请求码，正常可以不设置，如果和你的其他功能的请求吗有冲突，可用此方法重新设置。
         * 1、相册的请求码 -- 默认：102
         */
        public FileChooserBuilder requestCodeForGallery(int galleryRequestCode) {
            this.chooser_requestCode_gallery = galleryRequestCode;
            return this;
        }

        /**
         * 设置目的：同 1
         * 2、相机的请求码 -- 默认：103
         */
        public FileChooserBuilder requestCodeForCamera(int cameraRequestCode) {
            this.chooser_requestCode_camera = cameraRequestCode;
            return this;
        }

        /**
         * 设置目的：同 1
         * 3、剪裁的请求码 -- 默认：104
         */
        public FileChooserBuilder requestCodeForCrop(int cropRequestCode) {
            this.chooser_requestCode_crop = cropRequestCode;
            return this;
        }

        /**
         * 设置目的：同 1
         * 4、文件选择的请求码 -- 默认：105
         */
        public FileChooserBuilder requestCodeForFile(int fileRequestCode) {
            this.chooser_requestCode_file = fileRequestCode;
            return this;
        }


        /**
         * 相册拍照后保存的路径
         *
         * @param uri 默认为app 对外公有路径/storage/emulated/0/Android/data/com.xxx/files/Pictures/
         *            # 不可为私有路径
         *            原因：系统不能访问和写入你的app的私有路径
         */
        public FileChooserBuilder cameraOutputUri(Uri uri) {
            if (uri != null) {
                this.mCameraOutputUri = uri;
            }
            return this;
        }

        /**
         * 剪裁文件路径 全路径 ，带后缀的/xxx/xxx/xxx.jpg
         * 不能为cache私有路径... 必须为公有路径，或者外置卡路径
         * 原因：系统不能访问和写入你的app的私有路径
         */
        public FileChooserBuilder cropOutputFile(File file) {
            if (file != null) {
                this.mCropFile = file;
            }
            return this;
        }

        /**
         * @param fileProviderAuthority FileProvider 的 authority属性
         *                              在清单文件中注册,此参数需要与之保持
         *                              一致
         * @Default 默认为：context.getPackageName() + ".fileprovider"
         */
        public FileChooserBuilder authority(String fileProviderAuthority) {
            this.authority = fileProviderAuthority;
            return this;
        }

        /**
         * 重置一些不传参的变量
         * isCrop，asBitmap 等
         * 无连续切换（剪裁和非剪裁）状态的特殊需求，此方法不需要使用
         */
        public FileChooserBuilder reset() {
            asBitmap = false;
            isCrop = false;
            return this;
        }

        /**
         * 设置监听事件，用来接收返回结果
         *
         * @param callBack 返回结果监听对象
         */
        public FileChooserBuilder listener(@NonNull OnFileChooseCallBack callBack) {
            this.mFileChooseCallBack = callBack;
            return this;
        }

        /**
         * 选择打开方式 （相册）默认
         */
        public FileChooserBuilder gallery() {
            this.chooseType = 0;
            return this;
        }

        /**
         * 选择打开方式 （相机）
         */
        public FileChooserBuilder camera() {
            this.chooseType = 1;
            return this;
        }

        /**
         * 选择打开方式 （文件系统）
         */
        public FileChooserBuilder file() {
            this.chooseType = 2;
            return this;
        }

        /**
         * 打开触发开关
         */
        public void open() {
            if (chooseType == 1) {
                openCamera();
            } else if (chooseType == 2) {
                openFileChooser();
            } else {
                openGallery();
            }
        }

        /**
         * 进行剪裁
         */
        private void startCropPhoto(Uri uri) {
            if (uri == null) {
                return;
            }
            try {
                if (mCropFile == null) {
                    mCropFile = new File(getCropFileDir(), System.currentTimeMillis() + SUFFIX_FILE_NAME_CROP);
                }
                final Intent intent = getCropImageIntent(uri, mCropWidth, mCropFile);
                if (type == 0) {
                    mActivityWeakReference.get().startActivityForResult(intent, chooser_requestCode_crop);
                } else {
                    mFragmentWeakReference.get().startActivityForResult(intent, chooser_requestCode_crop);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * 图库选择
         */
        private void openGallery() {
            try {
                //调用相册
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (type == 0) {
                    mActivityWeakReference.get().startActivityForResult(intent, chooser_requestCode_gallery);
                } else {
                    mFragmentWeakReference.get().startActivityForResult(intent, chooser_requestCode_gallery);
                }
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }

        /**
         * 打开相机拍照
         */
        private void openCamera() {
            Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (mCameraOutputUri == null) {
                this.mCameraOutputUri = getCameraFileUri(mContextWeakReference.get());
            }
            if (mCameraOutputUri == null) {
                if (mFileChooseCallBack != null) {
                    mFileChooseCallBack.onCallBack(null, null, "failed: mCameraOutputUri is null");
                    return;
                }
            }
            takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraOutputUri);
            if (type == 0) {
                mActivityWeakReference.get().startActivityForResult(takeIntent, chooser_requestCode_camera);
            } else {
                mFragmentWeakReference.get().startActivityForResult(takeIntent, chooser_requestCode_camera);
            }
        }

        /**
         * 打开文件选择器
         */
        private void openFileChooser() {
            Intent intent = new Intent();
            if (isCrop) {
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            } else {
                intent.setAction(Intent.ACTION_GET_CONTENT);
            }
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            if (type == 0) {
                mActivityWeakReference.get().startActivityForResult(intent, chooser_requestCode_file);
            } else {
                mFragmentWeakReference.get().startActivityForResult(intent, chooser_requestCode_file);
            }
        }

        /**
         * 返回结果的处理
         */
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == chooser_requestCode_gallery) {
                // 相册
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        if (isCrop) {
                            startCropPhoto(uri);
                            return;
                        }
                        if (mFileChooseCallBack != null) {
                            Bitmap bitmap = null;
                            if (asBitmap) {
                                try {
                                    bitmap = BitmapFactory.decodeFile(FileUriUtils.getFileRealPath(mContextWeakReference.get(), uri));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            mFileChooseCallBack.onCallBack(uri, bitmap, "success");
                        }
                    } else {
                        //部分手机可能直接存放在bundle中
                        if (data.hasExtra("data")) {
                            Bitmap bitmap = data.getParcelableExtra("data");
                            Uri outputUri = FileUriUtils.saveBitmapAndReturnUri(mContextWeakReference.get(), bitmap, System.currentTimeMillis() + SUFFIX_FILE_NAME_CAMERA);
                            if (isCrop) {
                                startCropPhoto(outputUri);
                                return;
                            }
                            if (mFileChooseCallBack != null) {
                                mFileChooseCallBack.onCallBack(outputUri, bitmap, "success");
                            }
                        } else if (mFileChooseCallBack != null) {
                            mFileChooseCallBack.onCallBack(null, null, "failed: Can't get file from Intent(:data)");
                        }
                    }
                }
            } else if (requestCode == chooser_requestCode_camera) {
                // 相机
                if (data != null && data.hasExtra("data")) {
                    Bitmap bitmap = data.getParcelableExtra("data");
                    // 创建bitmap
                    Uri outputUri = FileUriUtils.saveBitmapAndReturnUri(mContextWeakReference.get(), bitmap, System.currentTimeMillis() + SUFFIX_FILE_NAME_CAMERA);
                    if (isCrop) {
                        startCropPhoto(outputUri);
                        return;
                    }
                    if (mFileChooseCallBack != null) {
                        mFileChooseCallBack.onCallBack(outputUri, bitmap, "success");
                    }
                } else {
                    if (isCrop) {
                        startCropPhoto(mCameraOutputUri);
                        return;
                    }
                    if (mFileChooseCallBack != null) {
                        Bitmap bitmap = null;
                        if (asBitmap) {
                            try {
                                bitmap = BitmapFactory.decodeFile(FileUriUtils.getFileRealPath(mContextWeakReference.get(), mCameraOutputUri));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        mFileChooseCallBack.onCallBack(mCameraOutputUri, bitmap, "success");
                    }
                }
            } else if (requestCode == chooser_requestCode_file) {
                // 文件系统
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri == null) {
                        if (mFileChooseCallBack != null) {
                            mFileChooseCallBack.onCallBack(null, null, "failed: Uri is null");
                        }
                        return;
                    }

                    // 需要判断是否是图片
                    String fileRealPath = FileUriUtils.getFileRealPath(mContextWeakReference.get(), uri);
                    boolean isImageFile = false;
                    if (fileRealPath != null) {
                        // 如果后缀是jpg，jpeg，png这三种格式则是图片
                        String suffix = fileRealPath.substring(fileRealPath.lastIndexOf(".") + 1);
                        isImageFile = JEPG_SUFFIX_LIST.contains(suffix);
                    }

                    if (isCrop) {
                        if (isImageFile) {
                            startCropPhoto(uri);
                            return;
                        }
                        mFileChooseCallBack.onCallBack(null, null, "failed: Can not crop a file which its type is not jpeg. please choose a jpeg file and try again");
                        return;
                    }

                    if (mFileChooseCallBack != null) {
                        if (isImageFile) {
                            Bitmap bitmap = null;
                            if (asBitmap) {
                                try {
                                    bitmap = BitmapFactory.decodeFile(FileUriUtils.getFileRealPath(mContextWeakReference.get(), uri));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            mFileChooseCallBack.onCallBack(uri, bitmap, "success");
                        } else {
                            mFileChooseCallBack.onCallBack(uri, null, "success");
                        }
                    }
                }
            } else if (requestCode == chooser_requestCode_crop) {
                // 剪裁
                if (mCropFile != null) {
                    // 返回CropUri
                    if (mFileChooseCallBack != null) {
                        Uri outputUri = FileUriUtils.getUri(mContextWeakReference.get(),ContentType.IMAGE, mCropFile);
                        Bitmap bitmap = null;
                        if (asBitmap) {
                            try {
                                bitmap = BitmapFactory.decodeFile(mCropFile.getAbsolutePath());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        mFileChooseCallBack.onCallBack(outputUri, bitmap, "success");
                    }
                }
            }
        }

        /**
         * 获取剪裁图片的启动 Intent
         *
         * @param photoUri       拍照、图库 选择图片的Uri
         * @param cropWidth      剪裁的宽度（正方形）
         * @param cropOutputFile 剪裁图片的输出文件
         * @return Intent
         * #bug记录：在华为手机上必须使用Uri.fromFile(cropOutputFile)来生成uri，否则会报错 "无法保存经过剪裁的图片"
         */
        private Intent getCropImageIntent(Uri photoUri, int cropWidth, File cropOutputFile) {
            Intent intent = new Intent("com.android.camera.action.CROP");
            if (Build.VERSION.SDK_INT >= 24) {
                Uri outputUri = Uri.fromFile(cropOutputFile);
                intent.setDataAndType(photoUri, "image/*");
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } else {
                Uri outputUri = Uri.fromFile(cropOutputFile);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    //这个方法是处理4.4以上图片返回的Uri对象不同的处理方法
                    String srcFile = FileUriUtils.getFileRealPath(mContextWeakReference.get(), photoUri);
                    if (srcFile != null) {
                        intent.setDataAndType(Uri.fromFile(new File(srcFile)), "image/*");
                    }
                } else {
                    intent.setDataAndType(photoUri, "image/*");
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, outputUri);
            }
            intent.putExtra("crop", "true");
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", cropWidth);
            intent.putExtra("outputY", cropWidth);
            intent.putExtra("noFaceDetection", false);
            intent.putExtra("return-data", false);
            intent.putExtra("outputFormat", "JPEG");
            return intent;
        }

        /**
         * 获取媒体Uri
         *
         * @param context 上下文
         * @return Uri
         * 文件名后缀>> 完整文件名格式： System.currentTimeMillis() + SUFFIX_FILE_NAME_CAMERA
         */
        private Uri getCameraFileUri(Context context) {
            try {
                File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File mediaStorageDir = null;
                if (externalFilesDir != null) {
                    String storagePath = externalFilesDir.getAbsolutePath();
                    mediaStorageDir = new File(storagePath);
                }
                if (mediaStorageDir == null) {
                    return null;
                }
                if (!mediaStorageDir.exists()) {
                    if (!mediaStorageDir.mkdirs()) {
                        return null;
                    }
                }
                File mediaFile = new File(mediaStorageDir.getPath() + File.separator + System.currentTimeMillis() + SUFFIX_FILE_NAME_CAMERA);
                if (Build.VERSION.SDK_INT >= 24) {
                    return FileProvider.getUriForFile(context, authority, mediaFile);
                }
                return Uri.fromFile(mediaFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 默认 /storage/emulated/0/Android/data/com.xxx/files/Pictures/
         * 拿不到就放外置卡中：/storage/emulated/0/Pictures/
         */
        private File getCropFileDir() {
            File file = null;
            try {
                if (mContextWeakReference.get() != null) {
                    file = mContextWeakReference.get().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (file == null) {
                file = new File(Environment.getExternalStorageDirectory() + File.separator + "Pictures");
            }
            return file;
        }

    }
}