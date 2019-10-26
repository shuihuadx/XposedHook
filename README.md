## 说明	
* 这种方案建议只在开发阶段**调试**的时候使用，因为这将损耗一些性能(*需要额外加载apk文件*)，调试没问题后，直接修改xposed_init配置文件指向目标类即可。

## 原理如下:
- Android设备安装一个app后，会在/data/app/目录下保存一份原始的apk。
- 自己写一个方法,将所有Hook逻辑直接或者间接的包含进来,作为"程序的入口"。  
- 这里通过读取包含"hook逻辑"的apk文件，然后new一个PathClassLoader，该PathClassLoader用于加载包含"hook处理逻辑"的类，最后使用反射的方式进入到"程序的入口"。
- 由于这里是动态加载的"hook逻辑"，所以不需要每次都重启设备，仅仅在第一次需要重启。
- 虽然不用每次都重启设备了，不过由于Xposed实现机制的原因（*handleLoadPackage方法的被调用时机的问题*），需要杀死宿主程序后，并重新启动宿主程序才能生效。  

## 用法  
1. 复制`HookLoader.java`到自己的模块代码中
2. 修正`HookLoader.java`包名`com.example.dx.xposedhook`为自己的包名
3. 修改`HookLogic`为自己模块的Hook逻辑处理类
```
    // 实际hook逻辑处理类
    private final String hookLogicClassName = HookLogic.class.getName();
``` 
4. 修改`xposed_init`指向`HookLoader`, 调试完后可以改回原来的Hook逻辑处理类

