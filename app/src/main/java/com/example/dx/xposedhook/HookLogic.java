package com.example.dx.xposedhook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookLogic implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    final String TAG = "Bootless Xposed: ";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedBridge.log(TAG + "handleLoadPackage from " + loadPackageParam.processName);
        if (loadPackageParam.processName.equals(HookMe.class.getPackage().getName())) {
            XposedHelpers.findAndHookMethod(
                    HookMe.class.getName(),
                    loadPackageParam.classLoader,
                    "isHooked",
                    XC_MethodReplacement.returnConstant(true));
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XposedBridge.log(TAG + "initZygote");
    }
}