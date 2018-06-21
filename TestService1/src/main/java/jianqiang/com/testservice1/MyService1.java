package jianqiang.com.testservice1;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyService1 extends Service {
    public MyService1() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("baobao", "Service is created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("baobao", "Service is started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("baobao", "Service is Destroy");
    }
}
