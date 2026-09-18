# Consumer proguard rules for serial_lib

# JNI 方法名由 Native 符号绑定，不能被重命名。
-keep class com.cl.serialportlibrary.SerialPort {
    native <methods>;
}

# Native close() 按字段名读取该文件描述符。
-keepclassmembers class com.cl.serialportlibrary.SerialPortManager {
    java.io.FileDescriptor mFd;
}

# 保持公开可序列化设备模型的字段名兼容。
-keepclassmembers class com.cl.serialportlibrary.Device implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
