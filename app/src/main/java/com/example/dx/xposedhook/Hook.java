package com.example.dx.xposedhook;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 可以实现免重启，由于存在缓存，
 * 需要杀死宿主程序以后才能生效
 * 这种免重启的方式针对某些特殊情况的hook无效
 * 例如我们需要implements IXposedHookZygoteInit并将自己的一个服务注册为系统服务，这种就必须重启了
 * Created by DX on 2017/10/4.
 */

public class Hook implements IXposedHookLoadPackage{
    //新建hook模块时，需要按照实际情况修改下面三项的值
    private String thisAppPackage="com.example.dx.xposedhook";
    private String handleHookClass=HandleHookClass.class.getName();
    private String handleHookMethod="handleHook";

    //可以通过使用缓存提升性能，不用每次都重新去加载apk
    private boolean isAllowChache=false;
    private Method handleHookMethodChache=null;
    private Object handleHookInstanceChache=null;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        invokeHandleHookMethod(thisAppPackage,handleHookClass,handleHookMethod,loadPackageParam);
    }

    /**
     * 安装app以后，系统会在/data/app/下备份了一份.apk文件，通过动态加载这个apk文件，调用相应的方法
     * 这样就可以实现，只需要第一次重启，以后修改hook代码就不用重启了
     * @param thisAppPackage 当前app的packageName
     * @param handleHookClass 指定由哪一个类处理相关的hook逻辑
     * @param loadPackageParam 传入XC_LoadPackage.LoadPackageParam参数
     * @throws Exception
     */
    private void invokeHandleHookMethod(String thisAppPackage, String handleHookClass,String handleHookMethodName, XC_LoadPackage.LoadPackageParam loadPackageParam)throws Exception{
        if (isAllowChache
                &&handleHookMethodChache!=null
                &&handleHookInstanceChache!=null){
            handleHookMethodChache.invoke(handleHookInstanceChache,loadPackageParam);
        }
        File apkFile=findApkFile(thisAppPackage);
        //加载指定的hook逻辑处理类，并调用它的handleHook方法
        PathClassLoader pathClassLoader=new PathClassLoader(apkFile.getAbsolutePath(),ClassLoader.getSystemClassLoader());
        try {
            Class<?> cls=Class.forName(handleHookClass,true,pathClassLoader);
            Object instance=cls.newInstance();
            Method method=cls.getDeclaredMethod(handleHookMethodName,XC_LoadPackage.LoadPackageParam.class);
            if (isAllowChache){
                handleHookMethodChache=method;
                handleHookInstanceChache=instance;
            }
            method.invoke(instance,loadPackageParam);
        } catch (ClassNotFoundException e) {
            throw new Exception(e);
        } catch (NoSuchMethodException e) {
            throw new Exception(e);
        } catch (InvocationTargetException e) {
            throw new Exception(e);
        } catch (IllegalAccessException e) {
            throw new Exception(e);
        }
    }
    /**
     * 寻找这个Android设备上的当前apk文件
     * @param thisAppPackage
     * @return
     * @throws Exception
     */
    private File findApkFile(String thisAppPackage)throws Exception{
        //先直接在/data/app/目录下查找"packageName-1/2".apk文件
        File apkFile=new File(String.format("/data/app/%s-%s.apk",thisAppPackage,"1"));
        if (!apkFile.exists()){
            apkFile=new File(String.format("/data/app/%s-%s.apk",thisAppPackage,"2"));
        }
        //如果/data/app/目录下没找到apk文件，就在/data/app/"packageName-1/2"下找base.apk文件
        if (!apkFile.exists()) {
            File file = new File(String.format("/data/app/%s-%s", thisAppPackage, "1"));
            if (!file.exists()) {
                file = new File(String.format("/data/app/%s-%s", thisAppPackage, "2"));
            }
            if (!file.exists() || !file.isDirectory()) {
                throw new FileNotFoundException(String.format("/data/app/%s-%s,该路径不存在", thisAppPackage, "1/2"));
            }
            apkFile = new File(file, "base.apk");
        }
        //如果没找到apk文件，就抛出异常
        if (!apkFile.exists()){
            throw new FileNotFoundException(String.format("/data/app/%s-%s/base.apk,该文件不存在",thisAppPackage,"1/2"));
        }
        return apkFile;
    }
}
