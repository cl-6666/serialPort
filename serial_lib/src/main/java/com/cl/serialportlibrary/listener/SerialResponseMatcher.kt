package com.cl.serialportlibrary.listener

/**
 * 判断收到的数据是否为指定发送数据的响应。
 * 返回 true 时，当前可靠发送请求完成；返回 false 时，数据仍会正常分发给接收监听器。
 */
fun interface SerialResponseMatcher {
    fun matches(request: ByteArray, response: ByteArray): Boolean
}
