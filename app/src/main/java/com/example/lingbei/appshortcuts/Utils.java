package com.example.lingbei.appshortcuts;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

/***
 * 封装了一个异常提示的工具类
 */
public class Utils {
    private Utils(){

    }
    public static void showToast(Context context, String message) {
        new android.os.Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }
}
