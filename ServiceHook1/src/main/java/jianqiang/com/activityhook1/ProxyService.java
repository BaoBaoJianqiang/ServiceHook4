package jianqiang.com.activityhook1;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ProxyService extends Service {

    private static final String TAG = "ProxyService";

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStart() called with " + "intent = [" + intent + "], startId = [" + startId + "]");

        // 分发Service
        ServiceManager.getInstance().onStartCommand(intent, flags, startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("jianqiang", "Service is binded");

        // 分发Service
        return ServiceManager.getInstance().onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("jianqiang", "Service is unbinded");

        return super.onUnbind(intent);
    }
}
