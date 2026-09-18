package com.cl.serialportlibrary.listener

import com.cl.serialportlibrary.enumerate.ReliableSendFailure

/**
 * 可靠发送状态监听器。回调默认发生在串口 IO 协程中，上层包装器会切换到主线程。
 */
interface OnReliableSendListener {
    fun onRetry(data: ByteArray, retryCount: Int, maxRetries: Int) = Unit

    fun onSuccess(data: ByteArray, response: ByteArray) = Unit

    fun onFailure(data: ByteArray, reason: ReliableSendFailure) = Unit
}
