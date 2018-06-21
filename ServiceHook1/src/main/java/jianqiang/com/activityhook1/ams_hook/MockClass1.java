package jianqiang.com.activityhook1.ams_hook;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jianqiang.com.activityhook1.ProxyService;
import jianqiang.com.activityhook1.ServiceManager;
import jianqiang.com.activityhook1.UPFApplication;

class MockClass1 implements InvocationHandler {

    private static final String TAG = "MockClass1";

    Object mBase;

    public MockClass1(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Log.e("bao", method.getName());

        if ("startService".equals(method.getName())) {
            // 只拦截这个方法
            // 替换参数, 任你所为;甚至替换原始ProxyService启动别的Service偷梁换柱

            // 找到参数里面的第一个Intent 对象
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }

            //get ProxyService form UPFApplication.pluginServices
            Intent rawIntent = (Intent) args[index];

            // 代理Service的包名, 也就是我们自己的包名
            String stubPackage = UPFApplication.getContext().getPackageName();


            // replace Plugin Service of ProxyService
            ComponentName componentName = new ComponentName(stubPackage, ProxyService.class.getName());
            Intent newIntent = new Intent();
            newIntent.setComponent(componentName);

            // 把我们原始要启动的TargetService先存起来
            newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, rawIntent);

            // Replace Intent, cheat AMS
            args[index] = newIntent;

            Log.d(TAG, "hook success");
            return method.invoke(mBase, args);
        } else if ("stopService".equals(method.getName())) {
            // 只拦截这个方法
            // 替换参数, 任你所为;甚至替换原始ProxyService启动别的Service偷梁换柱

            // 找到参数里面的第一个Intent 对象
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }

            Intent rawIntent = (Intent) args[index];
            Log.d(TAG, "hook success");
            return ServiceManager.getInstance().stopService(rawIntent);
        } else if("bindService".equals(method.getName())) {
            // 只拦截这个方法
            // 替换参数, 任你所为;甚至替换原始ProxyService启动别的Service偷梁换柱

            // 找到参数里面的第一个Intent 对象
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }

            //get ProxyService form UPFApplication.pluginServices
            Intent rawIntent = (Intent) args[index];

            //stroe intent-conn
            ServiceManager.getInstance().mServiceMap2.put(args[4], rawIntent);

            // 代理Service的包名, 也就是我们自己的包名
            String stubPackage = UPFApplication.getContext().getPackageName();

            // replace Plugin Service of ProxyService
            ComponentName componentName = new ComponentName(stubPackage, ProxyService.class.getName());
            Intent newIntent = new Intent();
            newIntent.setComponent(componentName);

            // 把我们原始要启动的TargetService先存起来
            newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, rawIntent);

            // Replace Intent, cheat AMS
            args[index] = newIntent;

            Log.d(TAG, "hook success");
            return method.invoke(mBase, args);
        } else if("unbindService".equals(method.getName())) {
            Intent rawIntent = ServiceManager.getInstance().mServiceMap2.get(args[0]);
            ServiceManager.getInstance().onUnbind(rawIntent);
            return method.invoke(mBase, args);
        }

        return method.invoke(mBase, args);
    }
}