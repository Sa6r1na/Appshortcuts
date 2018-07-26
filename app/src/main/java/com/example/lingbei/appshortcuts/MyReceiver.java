package com.example.lingbei.appshortcuts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = MainActivity.TAG;

    //没太懂为啥要加这个receiver，看了下注释是加了层保护，如果当前Intent的场景发生变化，则会被系统调用强制刷新下
    @Override
    public void onReceive(Context context, Intent intent){
        Log.i(TAG, "onReceive: " + intent);
        if(Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())){
            new ShortcutHelper(context).refreshShortcuts(true);
        }
    }
}
