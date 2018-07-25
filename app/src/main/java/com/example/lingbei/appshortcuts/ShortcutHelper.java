package com.example.lingbei.appshortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ShortcutHelper {
    private  static  final String  TAG = MainActivity.TAG;
    private static final String EXTRA_LAST_REFRESH ="com.example.lingbei.appshortcuts.EXTRA_LAST_REFRESH";
    private static final long REFRESH_INTERVAL_MS = 60 * 60 * 1000;
    private final Context mContext;
    private final ShortcutManager mShortcutManager;

    public ShortcutHelper(Context context){
        mContext = context;
        mShortcutManager = mContext.getSystemService(ShortcutManager.class);
    }

    public void maybeRestoreAllDynamicShortcuts() {
        if (mShortcutManager.getDynamicShortcuts().size() == 0) {
            // NOTE: If this application is always supposed to have dynamic shortcuts, then publish
            // them here.
            // Note when an application is "restored" on a new device, all dynamic shortcuts
            // will *not* be restored but the pinned shortcuts *will*.
        }
    }

    public void reportShortcutUsed(String id) {
        //每当用户选择包含给定ID的快捷方式或用户在应用程序中完成等同于选择快捷方式的操作时，发布快捷方式的应用程序应调用此方法。
        mShortcutManager.reportShortcutUsed(id);
    }

    private void callShortcutManager(BooleanSupplier r){
        try {
            if(!r.getAsBoolean()){
                Utils.showToast(mContext,"Call to ShortcutManager is rate-limited");
            }
        }catch (Exception e){
            Log.e(TAG,"Caught Exception",e);
            Utils.showToast(mContext,"Call to ShortcutManager is rate-limited"+e.toString());
        }
    }

   public List<ShortcutInfo> getShortcuts(){
        //这里有点不明白为啥要把getDynamicShortcuts和getPinnedShortcuts分开处理可能是要把动的不动的shortcut合并成最终的？
        final List<ShortcutInfo> ret = new ArrayList<>();
        final HashSet<String> seenKeys = new HashSet<>();

        for(ShortcutInfo shortcutInfo:mShortcutManager.getDynamicShortcuts()){
            if(!shortcutInfo.isImmutable()){
                ret.add(shortcutInfo);
                seenKeys.add(shortcutInfo.getId());
            }
        }

        for (ShortcutInfo shortcutInfo:mShortcutManager.getPinnedShortcuts()){
            if (!shortcutInfo.isImmutable() && !seenKeys.contains(shortcutInfo.getId())){
                ret.add(shortcutInfo);
                seenKeys.add(shortcutInfo.getId());
            }
        }

        return ret;
   }

   public void refreshShortcuts(boolean force){
        //force来决定要不要强制刷新一次，如果是false，stableThreshold就是当前时间减去一个小时；
       // 上次刷新如果再一个小时内就不会强制刷新一次
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                final long now = System.currentTimeMillis();
                final long stableThreshold = force? now:now-REFRESH_INTERVAL_MS;

                final List<ShortcutInfo> updateList = new ArrayList<>();

                for (ShortcutInfo shortcutInfo : getShortcuts()){
                    if(shortcutInfo.isImmutable()){
                        continue;
                    }

                    final PersistableBundle extras = shortcutInfo.getExtras();
                    if(extras!=null && extras.getLong(EXTRA_LAST_REFRESH)>=stableThreshold){
                        continue;
                    }

                    Log.i(TAG,"Refreshing shortcut:"+shortcutInfo.getId());

                    final ShortcutInfo.Builder  b = new ShortcutInfo.Builder(mContext,shortcutInfo.getId());

                    setSiteInformation(b,shortcutInfo.getIntent().getData());
                    //更新 EXTRA_LAST_REFRESH
                    setExtras(b);

                    updateList.add(b.build());

                }
                if (updateList.size()>0){
                    callShortcutManager(()->mShortcutManager.updateShortcuts(updateList));
                }
                return null;

            }
        }.execute();
   }


   private ShortcutInfo.Builder setSiteInformation(ShortcutInfo.Builder b, Uri uri){
        b.setShortLabel(uri.getHost());
        b.setLongLabel(uri.toString());

       Bitmap bmp = fetchFavicon(uri);
       if(bmp!=null){
           b.setIcon(Icon.createWithBitmap(bmp));
       }else {
           b.setIcon(Icon.createWithResource(mContext,R.drawable.link));
       }

       return b;
   }


   private  ShortcutInfo.Builder setExtras(ShortcutInfo.Builder b){
        final PersistableBundle extras = new PersistableBundle();
        extras.putLong(EXTRA_LAST_REFRESH,System.currentTimeMillis());
        b.setExtras(extras);
        return b;
   }

   public void addWebSiteShortcut(String urlAsString){
        final String uriFinal = urlAsString;
        //lambda表达式写法，大括号里都是参数，返回值传给callShortcutManager的boolean型参数
        callShortcutManager(()->{
            final ShortcutInfo shortcutInfo = createShortcutForUrl(normalizeUrl(uriFinal));
            return mShortcutManager.addDynamicShortcuts(Arrays.asList(shortcutInfo));

        });
   }

   public void disableShortcut(ShortcutInfo shortcutInfo){
        mShortcutManager.removeDynamicShortcuts(Arrays.asList(shortcutInfo.getId()));
   }

   public void removeShortcut(ShortcutInfo shortcutInfo){
        mShortcutManager.removeDynamicShortcuts(Arrays.asList(shortcutInfo.getId()));
   }

    public void enableShortcut(ShortcutInfo shortcutInfo){
        mShortcutManager.enableShortcuts(Arrays.asList(shortcutInfo.getId()));
    }

   private String normalizeUrl(String urlAsString){
        if(urlAsString.startsWith("http://")||urlAsString.startsWith("https://")){
            return urlAsString;
        }else {
            return "http://"+urlAsString;
        }
   }


   private ShortcutInfo  createShortcutForUrl(String  urlAsString){
       Log.i(TAG, "createShortcutForUrl: " + urlAsString);
       final ShortcutInfo.Builder b = new ShortcutInfo.Builder(mContext,urlAsString);

       final Uri uri = Uri.parse(urlAsString);
       //ACTION_VIEW
       //如果您拥有一些某项 Activity 可向用户显示的信息（例如，要使用图库应用查看的照片；
       // 或者要使用地图应用查看的地址），请使用 Intent 将此操作与 startActivity() 结合使用。
       b.setIntent(new Intent(Intent.ACTION_VIEW,uri));

       setSiteInformation(b,uri);
       //persistentbundle 类型的extra，里面放上次更新时间
       setExtras(b);

       return b.build();

   }
   private Bitmap fetchFavicon(Uri uri){
        final Uri iconUri=uri.buildUpon().path("favicon.icon").build();
        Log.i(TAG, "Fetching favicon from: " + iconUri);

       InputStream is = null;
       BufferedInputStream  bis = null;

       try {
           URLConnection conn = new URL(iconUri.toString()).openConnection();
           conn.connect();
           is = conn.getInputStream();
           bis =  new BufferedInputStream(is,8192);
           return BitmapFactory.decodeStream(bis);
       }  catch (IOException e) {
           Log.w(TAG,"Failed to fetch favicon from " + iconUri,e);
           return null;
       }
   }
}
