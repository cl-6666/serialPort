package com.cl.myapplication.message;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * log管理类
 */

public class LogManager {

    public final List<IMessage> messages;
    private boolean mAutoEnd = true;

    public LogManager() {
        messages = new ArrayList<>();
    }

    private static class InstanceHolder {

        public static LogManager sManager = new LogManager();
    }

    public static LogManager instance() {
        return InstanceHolder.sManager;
    }

    public void add(IMessage message) {
        messages.add(message);
    }

    public void post(IMessage message) {
        EventBus.getDefault().post(message);
    }

    public void clear() {
        messages.clear();
    }

    public boolean isAutoEnd() {
        return mAutoEnd;
    }

    public void setAutoEnd(boolean autoEnd) {
        mAutoEnd = autoEnd;
    }

    public void changAutoEnd() {
        mAutoEnd = !mAutoEnd;
    }
}

