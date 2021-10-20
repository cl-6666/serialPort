package com.cl.myapplication;

import android.app.Application;

import com.hjq.toast.ToastUtils;

/**
 * 项目：serialPort
 * 作者：Arry
 * 创建日期：2021/10/20
 * 描述：
 * 修订历史：
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 Toast 框架
        ToastUtils.init(this);
    }
}
