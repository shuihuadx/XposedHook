## 说明	
* 这种方案建议只在开发阶段**调试**的时候使用，因为这将损耗一些性能(*需要额外加载apk文件*)，调试没问题后，直接修改xposed_init配置文件指向目标类即可。

## 原理如下:
- Android设备安装一个app后，会在/data/app/目录下保存一份原始的apk。
- 自己写一个方法,将所有Hook逻辑直接或者间接的包含进来,作为"程序的入口"。  
- 这里通过读取包含"hook逻辑"的apk文件，然后new一个PathClassLoader，该PathClassLoader用于加载包含"hook处理逻辑"的类，最后使用反射的方式进入到"程序的入口"。
- 由于这里是动态加载的"hook逻辑"，所以不需要每次都重启设备，仅仅在第一次需要重启。
- 虽然不用每次都重启设备了，不过由于Xposed实现机制的原因（*handleLoadPackage方法的被调用时机的问题*），需要杀死宿主程序后，并重新启动宿主程序才能生效。  

## 注意  
1. 该项目使用的是XposedBridgeApi-54.jar开发的.   
2. 须根据实际情况修改HookLoader类中以下各项值
```java
    //按照实际使用情况修改下面几项的值
    /**
     * 当前Xposed模块的包名,方便寻找apk文件
     */
    private final String thisModulePackage = "com.example.xposedhook";
    /**
     * 宿主程序的包名(允许多个),过滤无意义的包名,防止无意义的apk文件加载
     */
    private static List<String> hostAppPackages = new ArrayList<>();

    static {
        // TODO: Add the package name of application your want to hook!
        hostAppPackages.add("xxx.xxx.xxx");
    }

    /**
     * 实际hook逻辑处理类
     */
    private final String handleHookClass = HookLogic.class.getName();
    /**
     * 实际hook逻辑处理类的入口方法
     */
    private final String handleHookMethod = "handleLoadPackage";
```
3. 如果XposedInstaller的log中提示未找到apk文件之类的错误,请首先检查thisModulePackage是否设置正确需要与build.gradle中的applicationId值对应,如果build.gradle中没有配置applicationId,就与AndroidManifest.xml的package值对应.
