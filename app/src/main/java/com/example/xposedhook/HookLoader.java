package com.example.xposedhook;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author DX
 *         这种方案建议只在开发调试的时候使用，因为这将损耗一些性能(需要额外加载apk文件)，调试没问题后，直接修改xposed_init文件为正确的类即可
 *         可以实现免重启，由于存在缓存，需要杀死宿主程序以后才能生效
 *         这种免重启的方式针对某些特殊情况的hook无效
 *         例如我们需要implements IXposedHookZygoteInit,并将自己的一个服务注册为系统服务，这种就必须重启了
 *         Created by DX on 2017/10/4.
 */

public class HookLoader implements IXposedHookLoadPackage {
    //新建hook模块时，需要按照实际情况修改下面几项的值
    /**
     * 该模块的包名,方便寻找apk文件
     */
    private final String thisModulePackage = "com.example.xposedhook";
    /**
     * 宿主程序的包名(允许多个),过滤无意义的包名,防止无意义的apk文件加载
     */
    private static List<String> hostAppPackages = new ArrayList<>();

    static {
        // TODO: Add your want to hook package name
        hostAppPackages.add("xxx.xxx.xxx");
    }

    /**
     * 实际hook逻辑处理类
     */
    private String handleHookClass = HookLogic.class.getName();
    /**
     * 实际hook逻辑处理类的入口方法
     */
    private String handleHookMethod = "handleLoadPackage";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (hostAppPackages.contains(loadPackageParam.packageName)) {
            //将loadPackageParam的classloader替换为宿主程序Application的classloader,解决宿主程序存在多个.dex文件时,有时候ClassNotFound的问题
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    loadPackageParam.classLoader = ((Context) param.args[0]).getClassLoader();
                    invokeHandleHookMethod(thisModulePackage, handleHookClass, handleHookMethod, loadPackageParam);
                }
            });
        }
    }

    /**
     * 安装app以后，系统会在/data/app/下备份了一份.apk文件，通过动态加载这个apk文件，调用相应的方法
     * 这样就可以实现，只需要第一次重启，以后修改hook代码就不用重启了
     *
     * @param thisAppPackage   当前app的packageName
     * @param handleHookClass  指定由哪一个类处理相关的hook逻辑
     * @param loadPackageParam 传入XC_LoadPackage.LoadPackageParam参数
     * @throws Throwable 抛出各种异常,包括具体hook逻辑的异常,寻找apk文件异常,反射加载Class异常等
     */
    private void invokeHandleHookMethod(String thisAppPackage, String handleHookClass, String handleHookMethodName, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
//        File apkFile = findApkFileBySDK(thisAppPackage);//会受其它Xposed模块hook 当前宿主程序的SDK_INT的影响
        File apkFile = findApkFile(thisAppPackage);
        //加载指定的hook逻辑处理类，并调用它的handleHook方法
        PathClassLoader pathClassLoader = new PathClassLoader(apkFile.getAbsolutePath(), ClassLoader.getSystemClassLoader());
        Class<?> cls = Class.forName(handleHookClass, true, pathClassLoader);
        Object instance = cls.newInstance();
        Method method = cls.getDeclaredMethod(handleHookMethodName, XC_LoadPackage.LoadPackageParam.class);
        method.invoke(instance, loadPackageParam);
    }

    /**
     * 寻找这个Android设备上的当前apk文件,不受其它Xposed模块hook SDK_INT的影响
     *
     * @param thisAppPackage 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException 在/data/app/下的未找到本模块apk文件,请检查本模块包名配置是否正确.
     *                               具体检查build.gradle中的applicationId和AndroidManifest.xml中的package
     */
    private File findApkFile(String thisAppPackage) throws FileNotFoundException {
        File apkFile = null;
        try {
            apkFile = findApkFileAfterSDK21(thisAppPackage);
        } catch (Exception e) {
            try {
                apkFile = findApkFileBeforeSDK21(thisAppPackage);
            } catch (Exception e2) {
                //忽略这个异常
            }
        }
        if (apkFile == null) {
            throw new FileNotFoundException("没在/data/app/下找到文件对应的apk文件");
        }
        return apkFile;
    }

    /**
     * 根据当前的SDK_INT寻找这个Android设备上的当前apk文件
     *
     * @param thisAppPackage 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException 在/data/app/下的未找到本模块apk文件,请检查本模块包名配置是否正确.
     *                               具体检查build.gradle中的applicationId和AndroidManifest.xml中的package
     */
    private File findApkFileBySDK(String thisAppPackage) throws FileNotFoundException {
        File apkFile;
        //当前Xposed模块hook了Build.VERSION.SDK_INT不用担心，因为这是发生在hook之前，不会有影响
        //但是其它的Xposed模块hook了当前宿主的这个值以后，就会有影响了,所以这里没有使用这个方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            apkFile = findApkFileAfterSDK21(thisAppPackage);
        } else {
            apkFile = findApkFileBeforeSDK21(thisAppPackage);
        }
        return apkFile;
    }

    /**
     * 寻找apk文件(api_21之后)
     * 在Android sdk21以及之后，apk文件的路径发生了变化
     *
     * @param packageName 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException apk文件未找到
     */
    private File findApkFileAfterSDK21(String packageName) throws FileNotFoundException {
        File apkFile;
        File path = new File(String.format("/data/app/%s-%s", packageName, "1"));
        if (!path.exists()) {
            path = new File(String.format("/data/app/%s-%s", packageName, "2"));
        }
        if (!path.exists() || !path.isDirectory()) {
            throw new FileNotFoundException(String.format("没找到目录/data/app/%s-%s", packageName, "1/2"));
        }
        apkFile = new File(path, "base.apk");
        if (!apkFile.exists() || apkFile.isDirectory()) {
            throw new FileNotFoundException(String.format("没找到文件/data/app/%s-%s/base.apk", packageName, "1/2"));
        }
        return apkFile;
    }

    /**
     * 寻找apk文件(api_21之前)
     *
     * @param packageName 当前模块包名
     * @return File 返回apk文件
     * @throws FileNotFoundException apk文件未找到
     */
    private File findApkFileBeforeSDK21(String packageName) throws FileNotFoundException {
        File apkFile = new File(String.format("/data/app/%s-%s.apk", packageName, "1"));
        if (!apkFile.exists()) {
            apkFile = new File(String.format("/data/app/%s-%s.apk", packageName, "2"));
        }
        if (!apkFile.exists() || apkFile.isDirectory()) {
            throw new FileNotFoundException(String.format("没找到文件/data/app/%s-%s.apk", packageName, "1/2"));
        }
        return apkFile;
    }
}