package com.cl.serialportlibrary.stick

import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * 常用黏包处理器工厂。
 */
open class StickyPacketHelperFactory {

    companion object {
        @JvmStatic
        fun createNoProcessing(): AbsStickPackageHelper = BaseStickPackageHelper()

        @JvmStatic
        fun createFixedLength(length: Int): AbsStickPackageHelper {
            return StaticLenStickPackageHelper(length)
        }

        @JvmStatic
        fun createDelimiterBased(delimiter: String): AbsStickPackageHelper {
            return SpecifiedStickPackageHelper(ByteArray(0), delimiter.toByteArray(StandardCharsets.UTF_8))
        }

        @JvmStatic
        fun createMarkerBased(startMarker: String, endMarker: String): AbsStickPackageHelper {
            return SpecifiedStickPackageHelper(
                startMarker.toByteArray(StandardCharsets.UTF_8),
                endMarker.toByteArray(StandardCharsets.UTF_8),
            )
        }

        @JvmStatic
        fun createMarkerBased(startMarker: ByteArray, endMarker: ByteArray): AbsStickPackageHelper {
            return SpecifiedStickPackageHelper(startMarker, endMarker)
        }

        @JvmStatic
        fun createVariableLength(
            byteOrder: ByteOrder,
            lenSize: Int,
            lenIndex: Int,
            offset: Int,
        ): AbsStickPackageHelper {
            return VariableLenStickPackageHelper(byteOrder, lenSize, lenIndex, offset)
        }

        @JvmStatic
        fun createVariableLength(): AbsStickPackageHelper {
            return VariableLenStickPackageHelper(ByteOrder.BIG_ENDIAN, 2, 2, 12)
        }

        @JvmStatic
        fun createTimeoutBased(timeout: Int): AbsStickPackageHelper {
            return TimeoutStickPackageHelper(timeout)
        }

        @JvmStatic
        fun createComposite(
            primaryHelper: AbsStickPackageHelper,
            fallbackHelper: AbsStickPackageHelper,
        ): AbsStickPackageHelper {
            return CompositeStickPackageHelper(primaryHelper, fallbackHelper)
        }
    }

    open class Common {
        companion object {
            @JvmStatic
            fun createATCommand(): AbsStickPackageHelper {
                return StickyPacketHelperFactory.createDelimiterBased("\r\n")
            }

            @JvmStatic
            fun createJsonLine(): AbsStickPackageHelper {
                return StickyPacketHelperFactory.createDelimiterBased("\n")
            }

            @JvmStatic
            fun createModbusRTU(): AbsStickPackageHelper {
                return StickyPacketHelperFactory.createFixedLength(8)
            }

            @JvmStatic
            fun createSTXETX(): AbsStickPackageHelper {
                return StickyPacketHelperFactory.createMarkerBased(byteArrayOf(0x02), byteArrayOf(0x03))
            }

            @JvmStatic
            fun createSOHEOT(): AbsStickPackageHelper {
                return StickyPacketHelperFactory.createMarkerBased(byteArrayOf(0x01), byteArrayOf(0x04))
            }
        }
    }
}
