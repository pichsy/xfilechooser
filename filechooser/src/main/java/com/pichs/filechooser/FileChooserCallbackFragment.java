package com.pichs.filechooser;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 无界面的隐形 Fragment。
 * <p>
 * 通过 {@link ActivityResultLauncher} 启动系统 Intent，并在内部拦截返回结果，
 * 从而免去调用方在 onActivityResult 中手动转发的样板代码。
 * <p>
 * 与 FileChooserBuilder 配合使用，回调结果最终会直接送达
 * {@link FileChooser.OnFileChooseCallBack}。
 */
public class FileChooserCallbackFragment extends Fragment {

    private ActivityResultLauncher<Intent> mLauncher;
    private OnResultListener mOnResultListener;

    /**
     * 结果回调监听。
     */
    public interface OnResultListener {
        void onResult(int resultCode, @Nullable Intent data);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 必须在 onCreate 中注册，且需在 launch 之前完成
        mLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    OnResultListener listener = mOnResultListener;
                    if (listener != null) {
                        listener.onResult(result.getResultCode(), result.getData());
                    }
                }
        );
    }

    /**
     * 设置结果回调。每次 launch 前设置一次即可。
     */
    public void setOnResultListener(@Nullable OnResultListener listener) {
        this.mOnResultListener = listener;
    }

    /**
     * 启动目标 Intent。
     */
    public void launch(@NonNull Intent intent) {
        if (mLauncher != null) {
            mLauncher.launch(intent);
        }
    }
}
