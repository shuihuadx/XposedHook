package com.example.xposedhook;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author DX
 * 这种方案建议只在开发调试的时候使用，因为这将损耗一些性能(需要额外加载apk文件)，调试没问题后，直接修改xposed_init文件为正确的类即可
 * 可以实现免重启，由于存在缓存，需要杀死宿主程序以后才能生效
 * Created by DX on 2017/10/4.
 * Modified by chengxuncc on 2019/4/16.
 */

public class HookLoader implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    //按照实际使用情况修改下面几项的值
    /**
     * 当前Xposed模块的包名,方便寻找apk文件
     */
    private final static String modulePackageName = HookLoader.class.getPackage().getName();

    /**
     * 实际hook逻辑处理类
     */
    private final String handleHookClass = HookLogic.class.getName();
    /**
     * 实际hook逻辑处理类的入口方法
     */
    private final String handleHookMethod = "handleLoadPackage";

    private final String initMethod = "initZygote";

    private IXposedHookZygoteInit.StartupParam startupparam;

    /**
     * 重定向handleLoadPackage函数前会执行initZygote
     *
     * @param loadPackageParam
     * @throws Throwable
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        // 排除系统应用
        if (loadPackageParam.appInfo == null ||
                (loadPackageParam.appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) == 1) {
            return;
        }
        //将loadPackageParam的classloader替换为宿主程序Application的classloader,解决宿主程序存在多个.dex文件时,有时候ClassNotFound的问题
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) param.args[0];
                loadPackageParam.classLoader = context.getClassLoader();
                Class<?> cls = getApkClass(context, modulePackageName, handleHookClass);
                Object instance = cls.newInstance();
                try {
                    cls.getDeclaredMethod(initMethod, startupparam.getClass()).invoke(instance, startupparam);
                }catch (NoSuchMethodException e){
                    // 找不到initZygote方法
                }
                cls.getDeclaredMethod(handleHookMethod, loadPackageParam.getClass()).invoke(instance, loadPackageParam);
            }
        });
    }

    /**
     * 实现initZygote，保存启动参数。
     *
     * @param startupParam
     */
    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        this.startupparam = startupParam;
    }

    private Class<?> getApkClass(Context context, String modulePackageName, String handleHookClass) throws Throwable {
        File apkFile = findApkFile(context, modulePackageName);
        if (apkFile == null) {
            throw new RuntimeException("寻找模块apk失败");
        }
        //加载指定的hook逻辑处理类，并调用它的handleHook方法
        PathClassLoader pathClassLoader = new PathClassLoader(apkFile.getAbsolutePath(), ClassLoader.getSystemClassLoader());
        Class<?> cls = Class.forName(handleHookClass, true, pathClassLoader);
        return cls;
    }

    /**
     * 根据包名构建目标Context,并调用getPackageCodePath()来定位apk
     *
     * @param context           context参数
     * @param modulePackageName 当前模块包名
     * @return apk file
     */
    private File findApkFile(Context context, String modulePackageName) {
        if (context == null) {
            return null;
        }
        try {
            Context moudleContext = context.createPackageContext(modulePackageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            String apkPath = moudleContext.getPackageCodePath();
            return new File(apkPath);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}