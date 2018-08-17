package jianqiang.com.activityhook1;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.jianqiang.mypluginlibrary.RefInvoke;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jianqiang.com.activityhook1.ams_hook.AMSHookHelper;

/**
 * @author weishu
 * @date 16/5/10
 */
public final class ServiceManager {

    private static final String TAG = "ServiceManager";

    private static volatile ServiceManager sInstance;

    private Map<String, Service> mServiceMap = new HashMap<String, Service>();

    //store intent-conn
    public Map<Object, Intent> mServiceMap2 = new HashMap<Object, Intent>();

    // 存储插件的Service信息
    private Map<ComponentName, ServiceInfo> mServiceInfoMap = new HashMap<ComponentName, ServiceInfo>();

    public synchronized static ServiceManager getInstance() {
        if (sInstance == null) {
            sInstance = new ServiceManager();
        }
        return sInstance;
    }

    /**
     * 启动某个插件Service; 如果Service还没有启动, 那么会创建新的插件Service
     * @param proxyIntent
     * @param startId
     */
    public int onStartCommand(Intent proxyIntent, int flags, int startId) {

        Intent targetIntent = proxyIntent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
        ServiceInfo serviceInfo = selectPluginService(targetIntent);

        try {
            if (!mServiceMap.containsKey(serviceInfo.name)) {
                // service还不存在, 先创建
                proxyCreateService(serviceInfo);
            }

            Service service = mServiceMap.get(serviceInfo.name);
            return service.onStartCommand(targetIntent, flags, startId);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 停止某个插件Service, 当全部的插件Service都停止之后, ProxyService也会停止
     * @param targetIntent
     * @return
     */
    public int stopService(Intent targetIntent) {
        ServiceInfo serviceInfo = selectPluginService(targetIntent);
        if (serviceInfo == null) {
            Log.w(TAG, "can not found service: " + targetIntent.getComponent());
            return 0;
        }
        Service service = mServiceMap.get(serviceInfo.name);
        if (service == null) {
            Log.w(TAG, "can not runnning, are you stopped it multi-times?");
            return 0;
        }

        service.onDestroy();

        mServiceMap.remove(serviceInfo.name);
        if (mServiceMap.isEmpty()) {
            // 没有Service了, 这个mServiceMap没有必要存在了
            Log.d(TAG, "service all stopped, stop proxy");
            Context appContext = UPFApplication.getContext();
            appContext.stopService(new Intent().setComponent(new ComponentName(appContext.getPackageName(), ProxyService.class.getName())));
        }
        return 1;
    }

    public IBinder onBind(Intent proxyIntent) {

        Intent targetIntent = proxyIntent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
        ServiceInfo serviceInfo = selectPluginService(targetIntent);

        try {
            if (!mServiceMap.containsKey(serviceInfo.name)) {
                // service还不存在, 先创建
                proxyCreateService(serviceInfo);
            }

            Service service = mServiceMap.get(serviceInfo.name);
            return service.onBind(targetIntent);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 停止某个插件Service, 当全部的插件Service都停止之后, ProxyService也会停止
     * @param targetIntent
     * @return
     */
    public boolean onUnbind(Intent targetIntent) {
        ServiceInfo serviceInfo = selectPluginService(targetIntent);
        if (serviceInfo == null) {
            Log.w(TAG, "can not found service: " + targetIntent.getComponent());
            return false;
        }
        Service service = mServiceMap.get(serviceInfo.name);
        if (service == null) {
            Log.w(TAG, "can not runnning, are you stopped it multi-times?");
            return false;
        }

        service.onUnbind(targetIntent);

        mServiceMap.remove(serviceInfo.name);
        if (mServiceMap.isEmpty()) {
            // 没有Service了, 这个mServiceMap没有必要存在了
            Log.d(TAG, "service all stopped, stop proxy");
            Context appContext = UPFApplication.getContext();
            appContext.stopService(
                    new Intent().setComponent(new ComponentName(appContext.getPackageName(), ProxyService.class.getName())));
        }
        return true;
    }

    /**
     * 选择匹配的ServiceInfo
     * @param pluginIntent 插件的Intent
     * @return
     */
    private ServiceInfo selectPluginService(Intent pluginIntent) {
        for (ComponentName componentName : mServiceInfoMap.keySet()) {
            if (componentName.equals(pluginIntent.getComponent())) {
                return mServiceInfoMap.get(componentName);
            }
        }
        return null;
    }

    /**
     * 通过ActivityThread的handleCreateService方法创建出Service对象
     * @param serviceInfo 插件的ServiceInfo
     * @throws Exception
     */
    private void proxyCreateService(ServiceInfo serviceInfo) throws Exception {
        IBinder token = new Binder();

        // 创建CreateServiceData对象, 用来传递给ActivityThread的handleCreateService 当作参数
        Object createServiceData = RefInvoke.createObject("android.app.ActivityThread$CreateServiceData");

        // 写入我们创建的createServiceData的token字段, ActivityThread的handleCreateService用这个作为key存储Service
        RefInvoke.setFieldObject(createServiceData.getClass(), createServiceData, "token", token);

        // 写入info对象
        // 这个修改是为了loadClass的时候, LoadedApk会是主程序的ClassLoader, 我们选择Hook BaseDexClassLoader的方式加载插件
        serviceInfo.applicationInfo.packageName = UPFApplication.getContext().getPackageName();
        RefInvoke.setFieldObject(createServiceData.getClass(), createServiceData, "info", serviceInfo);

        // 获取默认的compatibility配置
        Object defaultCompatibility = RefInvoke.getStaticFieldObject("android.content.res.CompatibilityInfo", "DEFAULT_COMPATIBILITY_INFO");
        // 写入compatInfo字段
        RefInvoke.setFieldObject(createServiceData.getClass(), createServiceData, "compatInfo", defaultCompatibility);


        // private void handleCreateService(CreateServiceData data) {
        Object currentActivityThread = RefInvoke.getStaticFieldObject("android.app.ActivityThread", "sCurrentActivityThread");
        RefInvoke.invokeInstanceMethod(currentActivityThread, "handleCreateService",
                new Class[]{createServiceData.getClass()},
                new Object[]{createServiceData});

        // handleCreateService创建出来的Service对象并没有返回, 而是存储在ActivityThread的mServices字段里面, 这里我们手动把它取出来
        Map mServices = (Map) RefInvoke.getFieldObject(currentActivityThread.getClass(), currentActivityThread, "mServices");
        Service service = (Service) mServices.get(token);

        // 获取到之后, 移除这个service, 我们只是借花献佛
        mServices.remove(token);

        // 将此Service存储起来
        mServiceMap.put(serviceInfo.name, service);
    }

    /**
     * 解析插件Apk文件中的 <service>, 并存储起来
     * 主要是调用PackageParser类的generateServiceInfo方法
     * @param apkFile 插件对应的apk文件
     * @throws Exception 解析出错或者反射调用出错, 均会抛出异常
     */
    public void preLoadServices(File apkFile) throws Exception {
        Object packageParser = RefInvoke.createObject("android.content.pm.PackageParser");

        // 首先调用parsePackage获取到apk对象对应的Package对象
        Object packageObj = RefInvoke.invokeInstanceMethod(packageParser, "parsePackage",
                new Class[] {File.class, int.class},
                new Object[] {apkFile, PackageManager.GET_SERVICES});

        // 读取Package对象里面的services字段
        // 接下来要做的就是根据这个List<Service> 获取到Service对应的ServiceInfo
        List services = (List) RefInvoke.getFieldObject(packageObj.getClass(), packageObj, "services");

        // 调用generateServiceInfo 方法, 把PackageParser.Service转换成ServiceInfo
        Class<?> packageParser$ServiceClass = Class.forName("android.content.pm.PackageParser$Service");
        Class<?> packageUserStateClass = Class.forName("android.content.pm.PackageUserState");

        int userId = (Integer) RefInvoke.invokeStaticMethod("android.os.UserHandle", "getCallingUserId");
        Object defaultUserState = RefInvoke.createObject("android.content.pm.PackageUserState");


        // 解析出intent对应的Service组件
        for (Object service : services) {
            // 需要调用 android.content.pm.PackageParser#generateActivityInfo(android.content.pm.ActivityInfo, int, android.content.pm.PackageUserState, int)
            ServiceInfo info = (ServiceInfo) RefInvoke.invokeInstanceMethod(packageParser, "generateServiceInfo",
                    new Class[] {packageParser$ServiceClass, int.class, packageUserStateClass, int.class},
                    new Object[] {service, 0, defaultUserState, userId});

            mServiceInfoMap.put(new ComponentName(info.packageName, info.name), info);
        }
    }
}
