package com.cl.myapplication.message;

/**
 * 项目：serialPort
 * 作者：Arry
 * 创建日期：2021/10/20
 * 描述：
 * 修订历史：
 */
public class ConversionNoticeEvent {

    private String message;

    public ConversionNoticeEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
