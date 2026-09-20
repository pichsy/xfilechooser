package com.pichs.filechooser;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import androidx.core.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * create by pichs
 * UriUtils
 * 最强大的Uri工具类没有之一
 * 注意: {
 * 请注意你配置的FileProvider的必须是
 * android:authorities="${applicationId}.fileprovider"
 * 最好业界统一
 * }
 */
@SuppressWarnings("ALL")
public class FileChooseUriUtils {

    private static final String TAG = "Share";

    @StringDef({ContentType.IMAGE, ContentType.AUDIO, ContentType.VIDEO, ContentType.FILE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface UriType {
    }

    public static Uri getUriFromImage(Context context, String name) {
        try {
            File file = new File(new URI(name));
            return getUri(context, ContentType.IMAGE, file);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Uri getUriFromImage(Context context, String name, String authority) {
        try {
            File file = new File(new URI(name));
            return getUri(context, ContentType.IMAGE, file, authority);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Uri getUriFromFile(Context context, File file) {
        return getUriFromFile(context, null, file);
    }

    /**
     * 获取File的Uri
     *
     * @param context 上下文
     * @param file    文件
     * @return Uri
     */
    public static Uri getUriFromFile(Context context, String authority, File file) {
        if (authority == null || authority.trim().length() == 0) {
            authority = context.getPackageName() + ".provider";
        }
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                return FileProvider.getUriForFile(context, authority, file);
            }
            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Uri getUri(Context context, @UriType String fileType, File file) {
        return getUri(context, fileType, file, null);
    }

    /**
     * Get file uri
     */
    public static Uri getUri(Context context, @UriType String fileType, File file, @Nullable String authority) {
        if (file == null || !file.exists()) {
            return null;
        }
        Uri uri = null;
        if (Build.VERSION.SDK_INT < 24) {
            uri = Uri.fromFile(file);
        } else {
            if (TextUtils.isEmpty(fileType)) {
                fileType = ContentType.FILE;
            }
            if (ContentType.IMAGE.equals(fileType)) {
                uri = getImageContentUri(context, file);
            } else if (ContentType.VIDEO.equals(fileType)) {
                uri = getVideoContentUri(context, file);
            } else if (ContentType.AUDIO.equals(fileType)) {
                uri = getAudioContentUri(context, file);
            } else if (ContentType.FILE.equals(fileType)) {
                uri = getFileContentUri(context, file);
            }
        }
        if (uri == null) {
            uri = getUriFromFile(context, authority, file);
        }
        if (uri == null) {
            uri = forceGetFileUri(file);
        }
        return uri;
    }

    /**
     * 判断是否是rootUri
     */
    private static boolean isRootUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        if (!ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            return false;
        }
        List<String> paths = uri.getPathSegments();
        return paths != null && "root".equals(paths.get(0));
    }

    /**
     * 获取文件的真实路劲 ，终级版 （超多适配...）
     */
    public static String getFileRealPath(final Context context, final Uri uri) {
        if (context == null) {
            Log.e(TAG, "getFileRealPath current activity is null.");
            return null;
        }
        if (uri == null) {
            Log.e(TAG, "getFileRealPath uri is null.");
            return null;
        }

        // 1. 直接就是一个文件路径的情况（例如 "file:///..." 或纯路径字符串）
        if (new File(uri.toString()).exists()) {
            return uri.toString();
        }

        // 2. 优先使用增强的 uri2File 转换（内部包含缓存拷贝兜底，
        //    兼容 Android 10+ 分区存储下 MediaStore.DATA 为空的情况）
        final File uriFile = uri2File(context, uri);
        if (uriFile != null) {
            return uriFile.getAbsolutePath();
        }

        String realPath = null;
        if (Build.VERSION.SDK_INT >= 19) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        realPath = Environment.getExternalStorageDirectory() + "/" + split[1];
                    } else if ("home".equalsIgnoreCase(type)) {
                        realPath = Environment.getExternalStorageDirectory() + "/documents/" + split[1];
                    }
                } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("raw:")) {
                        realPath = id.substring(4);
                    } else {
                        final String[] contentUriPrefixesToTry = new String[]{
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads",
                                "content://downloads/all_downloads"
                        };
                        for (String download_uri : contentUriPrefixesToTry) {
                            try {
                                final Uri contentUri = ContentUris.withAppendedId(Uri.parse(download_uri), Long.valueOf(id));
                                realPath = getDataColumn(context, contentUri, null, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (TextUtils.isEmpty(realPath)) {
                            // path could not be retrieved using ContentResolver, therefore copy file to accessible cache using streams
                            String fileName = getFileName(context, uri, id);
                            File cacheDir = new File(context.getCacheDir(), "documents");
                            if (!cacheDir.exists()) {
                                cacheDir.mkdirs();
                            }
                            File file = generateFileName(fileName, cacheDir);
                            if (file != null) {
                                realPath = file.getAbsolutePath();
                                saveFileFromUri(context, uri, realPath);
                            }
                        }
                    }
                } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    } else {
                        contentUri = MediaStore.Files.getContentUri("external");
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    try {
                        realPath = getDataColumn(context, contentUri, selection, selectionArgs);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                realPath = uri.getPath();
            } else if (isRootUri(uri)) {
                String[] pathArr = uri.toString().split("root");
                if (pathArr.length > 1) {
                    realPath = pathArr[1];
                }
            } else if ("es.fileexplorer.filebrowser.ezfilemanager.externalstorage.documents".equals(uri.getAuthority())) {
                final String path = uri.getPath();
                if (path != null) {
                    final String[] split = path.split(":");
                    if (split.length > 1) {
                        realPath = Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                }
            } else if ("com.tencent.mtt.fileprovider".equals(uri.getAuthority())) {
                final String path = uri.getPath();
                if (path != null) {
                    final String[] split = path.split("QQBrowser");
                    if (split.length > 1) {
                        realPath = Environment.getExternalStorageDirectory() + split[1];
                    }
                }
            } else if ("com.google.android.apps.photos.content".equals(uri.getAuthority())) {
                return uri.getLastPathSegment();
            }

        }
        // 终极路径查询....
        if (TextUtils.isEmpty(realPath)) {
            realPath = queryRealPath(context, uri);
        }
        // 3. 最后兜底：无法解析出真实路径时，把内容拷贝到应用缓存目录，
        //    保证调用方始终能拿到一个可读的文件路径
        if (TextUtils.isEmpty(realPath)) {
            final File cacheFile = copyUri2Cache(context, uri);
            if (cacheFile != null) {
                realPath = cacheFile.getAbsolutePath();
            }
        }
        return realPath;
    }

    /**
     * uri 转 File（对齐 xbase 的 UriHelper.uri2File）
     * 优先尝试直接解析出真实文件，解析不出来时拷贝到缓存。
     */
    private static File uri2File(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        File file = uri2FileReal(context, uri);
        if (file == null) {
            file = copyUri2Cache(context, uri);
        }
        return file;
    }

    /**
     * 直接解析 uri 对应的真实文件（不拷贝）。对齐 xbase 的 UriHelper.uri2FileReal。
     */
    private static File uri2FileReal(Context context, Uri uri) {
        String authority = uri.getAuthority();
        String scheme = uri.getScheme();
        String path = uri.getPath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && path != null) {
            // 不同厂商的 FileProvider 会生成各种自定义前缀
            final String[] externals = {"/external/", "/external_path/"};
            for (String external : externals) {
                if (path.startsWith(external)) {
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                            + path.replace(external, "/"));
                    if (file.exists()) {
                        return file;
                    }
                }
            }
            File file = null;
            if (path.startsWith("/files_path/")) {
                file = new File(context.getApplicationContext().getFilesDir().getAbsolutePath()
                        + path.replace("/files_path/", "/"));
            } else if (path.startsWith("/cache_path/")) {
                file = new File(context.getApplicationContext().getCacheDir().getAbsolutePath()
                        + path.replace("/cache_path/", "/"));
            } else if (path.startsWith("/external_files_path/")) {
                File dir = context.getApplicationContext().getExternalFilesDir(null);
                file = new File(dir != null ? dir.getAbsolutePath() : "",
                        path.replace("/external_files_path/", "/"));
            } else if (path.startsWith("/external_cache_path/")) {
                File dir = context.getApplicationContext().getExternalCacheDir();
                file = new File(dir != null ? dir.getAbsolutePath() : "",
                        path.replace("/external_cache_path/", "/"));
            }
            if (file != null && file.exists()) {
                return file;
            }
        }

        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(scheme)) {
            return path == null ? null : new File(path);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if ("com.android.externalstorage.documents".equals(authority)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return new File(Environment.getExternalStorageDirectory() + "/" + split[1]);
                } else {
                    // SD 卡等非主存储，通过 StorageVolume 反射解析真实路径
                    return getFileForExternalStorage(context, type, split);
                }
            } else if ("com.android.providers.downloads.documents".equals(authority)) {
                String id = DocumentsContract.getDocumentId(uri);
                if (TextUtils.isEmpty(id)) {
                    return null;
                }
                if (id.startsWith("raw:")) {
                    return new File(id.substring(4));
                } else if (id.startsWith("msf:")) {
                    id = id.split(":")[1];
                }
                long availableId;
                try {
                    availableId = Long.parseLong(id);
                } catch (Exception e) {
                    return null;
                }
                final String[] contentUriPrefixesToTry = {
                        "content://downloads/public_downloads",
                        "content://downloads/all_downloads",
                        "content://downloads/my_downloads"
                };
                for (String contentUriPrefix : contentUriPrefixesToTry) {
                    try {
                        final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), availableId);
                        File file = getFileFromUri(context, contentUri, null, null);
                        if (file != null) {
                            return file;
                        }
                    } catch (Exception ignore) {
                        // 该前缀不存在或 id 无效，继续尝试下一个
                    }
                }
                return null;
            } else if ("com.android.providers.media.documents".equals(authority)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                } else {
                    return null;
                }
                return getFileFromUri(context, contentUri, "_id=?", new String[]{split[1]});
            } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)) {
                return getFileFromUri(context, uri, null, null);
            } else {
                return null;
            }
        } else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(scheme)) {
            return getFileFromUri(context, uri, null, null);
        }
        return null;
    }

    /**
     * 针对特殊 authority 或通过 MediaStore.DATA 列解析文件。对齐 xbase 的 getFileFromUri。
     */
    private static File getFileFromUri(Context context, Uri uri, String selection, String[] selectionArgs) {
        final String authority = uri.getAuthority();
        if ("com.google.android.apps.photos.content".equals(authority)) {
            if (!TextUtils.isEmpty(uri.getLastPathSegment())) {
                return new File(uri.getLastPathSegment());
            }
        } else if ("com.tencent.mtt.fileprovider".equals(authority)) {
            final String path = uri.getPath();
            if (!TextUtils.isEmpty(path) && path.startsWith("/QQBrowser")) {
                return new File(Environment.getExternalStorageDirectory(),
                        path.substring("/QQBrowser".length()));
            }
        } else if ("com.huawei.hidisk.fileprovider".equals(authority)) {
            final String path = uri.getPath();
            if (!TextUtils.isEmpty(path)) {
                return new File(path.replace("/root", ""));
            }
        }
        Cursor cursor = null;
        try {
            cursor = context.getApplicationContext().getContentResolver().query(
                    uri, new String[]{MediaStore.Files.FileColumns.DATA}, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                if (columnIndex > -1) {
                    return new File(cursor.getString(columnIndex));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 通过反射 StorageVolume 解析 SD 卡等非主存储的真实路径。
     * 对齐 xbase 中对 com.android.externalstorage.documents 非 primary 的处理。
     */
    private static File getFileForExternalStorage(Context context, String uuid, String[] split) {
        try {
            android.os.storage.StorageManager storageManager =
                    (android.os.storage.StorageManager) context.getApplicationContext()
                            .getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = storageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getState = storageVolumeClazz.getMethod("getState");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Method isEmulated = storageVolumeClazz.getMethod("isEmulated");
            Object result = getVolumeList.invoke(storageManager);
            int length = java.lang.reflect.Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = java.lang.reflect.Array.get(result, i);
                String state = (String) getState.invoke(storageVolumeElement);
                boolean mounted = Environment.MEDIA_MOUNTED.equals(state)
                        || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
                if (!mounted) {
                    continue;
                }
                if ((Boolean) isPrimary.invoke(storageVolumeElement)
                        && (Boolean) isEmulated.invoke(storageVolumeElement)) {
                    continue;
                }
                String volumeUuid = (String) getUuid.invoke(storageVolumeElement);
                if (volumeUuid != null && volumeUuid.equals(uuid)) {
                    String volumePath = (String) getPath.invoke(storageVolumeElement);
                    return new File(volumePath + "/" + split[1]);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * 把 uri 对应的内容拷贝到应用缓存目录，返回拷贝后的临时文件。
     * 用于 Android 10+ 分区存储下无法直接拿到真实路径时的兜底方案。
     */
    private static File copyUri2Cache(Context context, Uri uri) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getApplicationContext().getContentResolver().openInputStream(uri);
            if (is == null) {
                return null;
            }
            File file = new File(context.getApplicationContext().getCacheDir(),
                    System.currentTimeMillis() + ".tmp");
            bos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            bos.flush();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException ignore) {
                // ignore
            }
        }
    }

    /**
     * 查询真实路径，针对某些手机走系统的文件管理系统，所产生的uri
     */
    private static String queryRealPath(Context context, Uri uri) {
        String filePath = null;
        try {
            if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
                Cursor cursor = context.getContentResolver().query(uri,
                        new String[]{MediaStore.Files.FileColumns.DATA}, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                    }
                    cursor.close();
                }
            } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                filePath = uri.getPath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filePath;
    }


    /**
     * forceGetFileUri
     */
    private static Uri forceGetFileUri(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                @SuppressLint("PrivateApi")
                Method rMethod = StrictMode.class.getDeclaredMethod("disableDeathOnFileUriExposure");
                rMethod.invoke(null);
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        return Uri.parse("file://" + file.getAbsolutePath());
    }

    /**
     * getFileContentUri
     */
    private static Uri getFileContentUri(Context context, File file) {
        String volumeName = "external";
        String filePath = file.getAbsolutePath();
        String[] projection = new String[]{MediaStore.Files.FileColumns._ID};
        Uri uri = null;

        Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri(volumeName), projection,
                MediaStore.Images.Media.DATA + "=? ", new String[]{filePath}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                uri = MediaStore.Files.getContentUri(volumeName, id);
            }
            cursor.close();
        }

        return uri;
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     */
    private static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        Uri uri = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/images/media");
                uri = Uri.withAppendedPath(baseUri, "" + id);
            }

            cursor.close();
        }

        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, filePath);
            uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }

        return uri;
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     */
    private static Uri getVideoContentUri(Context context, File videoFile) {
        Uri uri = null;
        String filePath = videoFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID}, MediaStore.Video.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/video/media");
                uri = Uri.withAppendedPath(baseUri, "" + id);
            }

            cursor.close();
        }

        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DATA, filePath);
            uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        }

        return uri;
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     */
    private static Uri getAudioContentUri(Context context, File audioFile) {
        Uri uri = null;
        String filePath = audioFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID}, MediaStore.Audio.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                Uri baseUri = Uri.parse("content://media/external/audio/media");
                uri = Uri.withAppendedPath(baseUri, "" + id);
            }

            cursor.close();
        }
        if (uri == null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.DATA, filePath);
            uri = context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        }

        return uri;
    }

    /**
     * 在数据库中查询路径
     */
    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) throws Exception {
        Cursor cursor = null;
        final String[] projection = {MediaStore.Files.FileColumns.DATA};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 获取文件名字
     */
    private static String getFileName(@NonNull Context context, Uri uri, String id) {
        String mimeType = context.getContentResolver().getType(uri);
        String filename = null;
        if (mimeType == null) {
            filename = id;
        } else {
            Cursor returnCursor = context.getContentResolver().query(uri, null,
                    null, null, null);
            if (returnCursor != null) {
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                returnCursor.moveToFirst();
                filename = returnCursor.getString(nameIndex);
                returnCursor.close();
            }
        }
        return filename;
    }

    /**
     * 创建文件，名字有就增加 (1 ++)
     */
    public static File generateFileName(@Nullable String name, File directory) {
        if (name == null) {
            return null;
        }
        File file = new File(directory, name);
        if (file.exists()) {
            String fileName = name;
            String extension = "";
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex);
                extension = name.substring(dotIndex);
            }
            int index = 0;
            while (file.exists()) {
                index++;
                name = fileName + '(' + index + ')' + extension;
                file = new File(directory, name);
            }
        }
        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }
        return file;
    }

    /**
     * 拷贝文件到缓存中
     */
    private static void saveFileFromUri(Context context, Uri uri, String destinationPath) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            if (is != null) {
                is.read(buf);
                do {
                    bos.write(buf);
                } while (is.read(buf) != -1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 保存bitmap为图片 并返回Uri
     *
     * @param context  上下文
     * @param bitmap   Bitmap
     * @param fileName 文件名字
     * @return Uri
     */
    public static Uri saveBitmapAndReturnUri(Context context, Bitmap bitmap, String fileName) {
        if (null == bitmap) {
            return null;
        }
        File fileDir = getAppExternalFileDir(context);
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        File destFile = new File(fileDir, fileName);
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));
            //压缩保存到本地
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return getUri(context, ContentType.IMAGE, destFile);
    }

    /**
     * 获取app ExternalFileDir 路经
     *
     * @param context 上下文
     * @return File
     */
    private static File getAppExternalFileDir(Context context) {
        File file = null;
        try {
            if (context != null) {
                file = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
