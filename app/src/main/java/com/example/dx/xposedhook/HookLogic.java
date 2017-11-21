package com.example.dx.xposedhook;

import android.os.Build;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 这个类不一定需要implements IXposedHookLoadPackage,这里我也不知道为什么,感觉implements IXposedHookLoadPackage后更爽一点
 * 开发Xposed模块完成以后，建议修改xposed_init文件，并将起指向这个类,以提升性能
 * 注意：该类不要自己写构造方法，否者可能会hook不成功
 * Created by DX on 2017/10/4.
 */

public class HookLogic implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (loadPackageParam.packageName.equals("com.example.admin.romconfigsetting")){
            Log.wtf("handleHook","log1->sdk_int="+Build.VERSION.SDK_INT);
            XposedHelpers.findAndHookMethod("com.example.admin.romconfigsetting.MainActivity", loadPackageParam.classLoader, "testResult", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult("Build.VERSION.SDK_INT="+Build.VERSION.SDK_INT);
                }
            });
            XposedHelpers.setStaticIntField(Build.VERSION.class,"SDK_INT",25);
            Log.wtf("handleHook","log2->sdk_int="+Build.VERSION.SDK_INT);
        }
    }
}
