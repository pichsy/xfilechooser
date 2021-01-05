package com.pichs.filechooser;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 调用系统分享的工具类
 * 适配了一些手机
 * FileShare.with(context)
 * .setTitle("标题")
 * .setTextContent("内容")
 * .setContentType(FileShare.UriType.IMAGE)
 * .addShareFileUri(uri)
 * .setOnActivityResult(120)
 * .build()
 * .share();
 */
@SuppressWarnings("ALL")
public class FileShare {

    private static final String TAG = "FileShare";

    /**
     * ShareContentType 选择分享的类型
     * Support Share Content Types.
     * 两种： 文本类型，文件类型（包含图片视频等)）
     */
    @StringDef({ContentType.FILE,
            ContentType.IMAGE,
            ContentType.TEXT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface UriType {
    }

    /**
     * Current activity
     */
    private Activity activity;

    /**
     * Share content type
     */
    private @UriType
    String contentType;

    /**
     * Share title
     */
    private String title;

    /**
     * Share file Uri
     */
    private Set<Uri> shareFileUriList;

    /**
     * Share content text
     */
    private String contentText;

    /**
     * Share to special component PackageName
     */
    private String componentPackageName;

    /**
     * Share to special component ClassName
     */
    private String componentClassName;

    /**
     * Share to special component ClassName
     */
    private String targetPackage;

    /**
     * Share complete onActivityResult requestCode
     */
    private int requestCode;

    /**
     * Forced Use System Chooser
     */
    private boolean forcedUseSystemChooser;

    private FileShare(@NonNull Builder builder) {
        this.activity = builder.activity;
        this.contentType = builder.contentType;
        this.title = builder.title;
        this.shareFileUriList = builder.shareFileUriList;
        this.contentText = builder.textContent;
        this.componentPackageName = builder.componentPackageName;
        this.componentClassName = builder.componentClassName;
        this.requestCode = builder.requestCode;
        this.forcedUseSystemChooser = builder.forcedUseSystemChooser;
        this.targetPackage = builder.targetPackage;
    }

    /**
     * share
     */
    public void share() {
        if (checkShareParam()) {
            Intent shareIntent = createShareIntent();

            if (shareIntent == null) {
                Log.e(TAG, "share failed! share Intent is null");
                return;
            }

            if (title == null) {
                title = "";
            }

            if (forcedUseSystemChooser) {
                shareIntent = Intent.createChooser(shareIntent, title);
            }

            if (shareIntent.resolveActivity(activity.getPackageManager()) != null) {
                try {
                    if (requestCode != -1) {
                        activity.startActivityForResult(shareIntent, requestCode);
                    } else {
                        activity.startActivity(shareIntent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }
    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addCategory("android.intent.category.DEFAULT");
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // 优先使用ComponentName没有再考虑packageName
        if (!TextUtils.isEmpty(this.componentPackageName) && !TextUtils.isEmpty(componentClassName)) {
            ComponentName comp = new ComponentName(componentPackageName, componentClassName);
            shareIntent.setComponent(comp);
        } else if (!TextUtils.isEmpty(this.targetPackage)) {
            shareIntent.setPackage(this.targetPackage);
        }

        switch (contentType) {
            case ContentType.TEXT:
                shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);
                shareIntent.setType(contentType);
                break;
            case ContentType.FILE:
            case ContentType.IMAGE:
                if (shareFileUriList == null || shareFileUriList.isEmpty()) {
                    // 没数据分享个屁
                    Log.e(TAG, "shareFileUriList is null");
                    return null;
                }
                shareIntent.setType(contentType);
                if (!TextUtils.isEmpty(contentText)) {
                    shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);
                }

                if (shareFileUriList.size() == 1) {
                    shareIntent.putExtra(Intent.EXTRA_STREAM, shareFileUriList.iterator().next());
                } else {
                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(shareFileUriList));
                }

                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        for (Uri uri : shareFileUriList) {
                            activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }
                }
                break;
            default:
                Log.e(TAG, contentType + " is not support share type.");
                shareIntent = null;
                break;
        }

        return shareIntent;
    }


    private boolean checkShareParam() {
        if (this.activity == null) {
            Log.e(TAG, "activity is null.");
            return false;
        }
        if (ContentType.TEXT.equals(contentType)) {
            if (TextUtils.isEmpty(contentText)) {
                Log.e(TAG, "Share text context is empty.");
                return false;
            }
        } else {
            if (this.shareFileUriList == null || shareFileUriList.isEmpty()) {
                Log.e(TAG, "Share file path is null.");
                return false;
            }
        }
        return true;
    }

    public static Builder with(Activity activity) {
        return new Builder(activity);
    }

    public static class Builder {
        private Activity activity;
        @UriType
        private String contentType = ContentType.FILE;
        private String title;
        // 指定app的界面 优先
        private String componentPackageName;
        private String componentClassName;
        // 指定app分享 次之
        private String targetPackage;
        private Set<Uri> shareFileUriList;
        private String textContent;
        private int requestCode = -1;
        private boolean forcedUseSystemChooser = true;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        /**
         * Set Content Type
         *
         * @param contentType {@link ContentType}
         * @return Builder
         */
        public Builder setContentType(@UriType String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Set Title
         *
         * @param title title
         * @return Builder
         */
        public Builder setTitle(@NonNull String title) {
            this.title = title;
            return this;
        }

        /**
         * Set share file path uri， ,分享图片时使用，分享文字时调用无效
         * 添加一个uri
         *
         * @param shareUri shareFileUri
         * @return Builder
         */
        public Builder addShareFileUri(Uri shareUri) {
            if (shareUri == null) {
                return this;
            }
            if (shareFileUriList == null) {
                shareFileUriList = new HashSet<>();
            }
            this.shareFileUriList.add(shareUri);
            return this;
        }

        /**
         * Set share file path uri， ,分享图片时使用，分享文字时调用无效
         * 添加一个uri
         *
         * @param shareUris shareFileUri
         * @return Builder
         */
        public Builder addShareFileUri(Uri... shareUris) {
            if (shareUris == null || shareUris.length == 0) {
                return this;
            }
            if (shareUris.length == 1) {
                addShareFileUri(shareUris[0]);
                return this;
            }
            if (this.shareFileUriList == null) {
                this.shareFileUriList = new HashSet<>();
            }
            this.shareFileUriList.addAll(Arrays.asList(shareUris));
            return this;
        }

        /**
         * 批量添加uri ,分享图片时使用，分享文字时调用无效
         *
         * @param shareUriList Collection<Uri>
         * @return
         */
        public Builder addShareFileUri(Collection<Uri> shareUriList) {
            if (shareUriList == null) {
                return this;
            }
            if (this.shareFileUriList == null) {
                this.shareFileUriList = new HashSet<>();
            }
            this.shareFileUriList.addAll(shareUriList);
            return this;
        }

        public Builder clearShareFileUri() {
            if (this.shareFileUriList != null) {
                this.shareFileUriList.clear();
            }
            return this;
        }

        /**
         * Set text content ,分享文字时使用，分享图片时调用无效
         *
         * @param textContent textContent
         * @return Builder
         */
        public Builder setTextContent(@NonNull String textContent) {
            this.textContent = textContent;
            return this;
        }

        /**
         * Set Share To Component，可指定app 页面、包名分享
         *
         * @param componentPackageName componentPackageName
         * @param componentClassName   componentPackageName
         * @return Builder
         */
        public Builder setShareToComponent(String componentPackageName, String componentClassName) {
            this.componentPackageName = componentPackageName;
            this.componentClassName = componentClassName;
            return this;
        }

        /**
         * 设置分享至app的package name
         *
         * @param targetPackage 目标app的包名
         * @return Builder
         */
        public Builder setTargetPackage(String targetPackage) {
            this.targetPackage = targetPackage;
            return this;
        }

        /**
         * Set onActivityResult requestCode, default value is -1
         *
         * @param requestCode requestCode
         * @return Builder
         */
        public Builder setOnActivityResult(int requestCode) {
            this.requestCode = requestCode;
            return this;
        }

        /**
         * Forced Use System Chooser To Share
         *
         * @param enable default is true
         * @return Builder
         */
        public Builder forcedUseSystemChooser(boolean enable) {
            this.forcedUseSystemChooser = enable;
            return this;
        }

        /**
         * build
         *
         * @return Share2
         */
        public FileShare build() {
            return new FileShare(this);
        }

    }
}
