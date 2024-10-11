package com.kongqw.serialportlibrary.enumerate;

/**
 * name：cl
 * date：2024/9/26
 * desc：串口错误码
 */
public enum SerialErrorCode {

    // 定义通用错误码
    UNKNOWN_ERROR(1000, "未知错误"),
    SERIAL_PORT_OPEN_FAILED(1001, "串口打开失败"),
    SERIAL_PORT_DATA_SEND_FAILED(1002, "串口数据发送失败"),
    SERIAL_PORT_TYPE_UNKNOWN(1003, "未知的串口类型，请检查串口路线是否错误"),
    SERIAL_PORT_DATA_RECEIVE_FAILED(1004, "串口数据接收失败"),
    UNINITIALIZED_SERIAL_PORT(1005, "未初始化的串口"),
    FILE_NOT_FOUND(1006, "文件未找到"),
    SERIAL_PORT_NUMBER_ERROR(1007, "串口数量不符合要求,目前最大只支持6路串口"),
    STICK_PACKAGE_CONFIG_ERROR(1008, "黏包数量配置不合法，请检查是否配置了错误的参数，或者没有配置"),
    PERMISSION_DENIED(1009, "权限被拒绝,请检查是否有串口的读写权限，建议运行SelectSerialPortActivity查看");

    private final int code;
    private final String message;

    SerialErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorCode{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }

    public static SerialErrorCode getByCode(int code) {
        for (SerialErrorCode errorCode : values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }
}
