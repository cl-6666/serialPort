package com.cl.serialportlibrary.stick

import java.io.InputStream

/**
 * 同步解析串口输入流，并在得到完整数据包后返回。
 */
fun interface AbsStickPackageHelper {
    fun execute(inputStream: InputStream): ByteArray?
}
