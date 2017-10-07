package com.example.dx.xposedhook;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 注意：该类不要自己写构造方法，否者可能会hook不成功
 * Created by DX on 2017/10/4.
 */

public class HandleHookClass {
    public void handleHook(XC_LoadPackage.LoadPackageParam loadPackageParam){
        if (loadPackageParam.packageName.equals("com.example.dx.xposedhookcontrol")){
            Log.wtf("handleHook","log");
            XposedHelpers.findAndHookMethod("com.example.dx.xposedhookcontrol.MainActivity", loadPackageParam.classLoader, "hookTest", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult("motherfucker");
                }
            });
        }
    }
}
