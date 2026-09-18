

/*
 * Copyright 2009-2011 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <stdio.h>
#include <jni.h>

#include "SerialPort.h"

#include "android/log.h"
static const char *TAG="serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static void throwIllegalArgumentException(JNIEnv *env, const char *message)
{
    jclass exceptionClass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (exceptionClass != NULL) {
        (*env)->ThrowNew(env, exceptionClass, message);
    }
}

static void throwIOException(JNIEnv *env, const char *operation, int errorNumber)
{
    char message[256];
    snprintf(message, sizeof(message), "%s failed: %s (errno=%d)",
             operation, strerror(errorNumber), errorNumber);
    jclass exceptionClass = (*env)->FindClass(env, "java/io/IOException");
    if (exceptionClass != NULL) {
        (*env)->ThrowNew(env, exceptionClass, message);
    }
}

static speed_t getBaudrate(jint baudrate)
{
    switch(baudrate) {
        case 0: return B0;
        case 50: return B50;
        case 75: return B75;
        case 110: return B110;
        case 134: return B134;
        case 150: return B150;
        case 200: return B200;
        case 300: return B300;
        case 600: return B600;
        case 1200: return B1200;
        case 1800: return B1800;
        case 2400: return B2400;
        case 4800: return B4800;
        case 9600: return B9600;
        case 19200: return B19200;
        case 38400: return B38400;
        case 57600: return B57600;
        case 115200: return B115200;
        case 230400: return B230400;
        case 460800: return B460800;
        case 500000: return B500000;
        case 576000: return B576000;
        case 921600: return B921600;
        case 1000000: return B1000000;
        case 1152000: return B1152000;
        case 1500000: return B1500000;
        case 2000000: return B2000000;
        case 2500000: return B2500000;
        case 3000000: return B3000000;
        case 3500000: return B3500000;
        case 4000000: return B4000000;
        default: return -1;
    }
}

/*
 * Class:     android_serialport_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;II)Ljava/io/FileDescriptor;
 */
JNIEXPORT jobject JNICALL Java_com_cl_serialportlibrary_SerialPort_open
        (JNIEnv *env, jclass thiz, jstring path, jint baudrate, jint flags, jint databits, jint stopbits, jint parity)
{
    int fd;
    int openFlags;
    speed_t speed;
    jobject mFileDescriptor;

    /* Check arguments */
    {
        if (path == NULL) {
            throwIllegalArgumentException(env, "path must not be null");
            return NULL;
        }
        speed = getBaudrate(baudrate);
        if (speed == -1) {
            LOGE("Invalid baudrate");
            throwIllegalArgumentException(env, "Unsupported baudrate");
            return NULL;
        }
        if (databits < 5 || databits > 8 || (stopbits != 1 && stopbits != 2) || parity < 0 || parity > 4) {
            LOGE("Invalid serial port parameters");
            throwIllegalArgumentException(env, "Invalid databits, stopbits or parity");
            return NULL;
        }

        int allowedFlags = O_NOCTTY | O_NONBLOCK | O_SYNC;
#ifdef O_DSYNC
        allowedFlags |= O_DSYNC;
#endif
#ifdef O_CLOEXEC
        allowedFlags |= O_CLOEXEC;
#endif
        if ((flags & ~allowedFlags) != 0) {
            LOGE("Unsupported open flags: 0x%x", flags);
            throwIllegalArgumentException(env, "Unsupported serial port open flags");
            return NULL;
        }
        openFlags = O_RDWR | O_NOCTTY | flags;
#ifdef O_CLOEXEC
        openFlags |= O_CLOEXEC;
#endif
    }

    /* Opening device */
    {
        jboolean iscopy;
        const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
        if (path_utf == NULL) {
            return NULL;
        }
        LOGD("Opening serial port %s with flags 0x%x", path_utf, openFlags);
        fd = open(path_utf, openFlags);
        int openError = errno;
        LOGD("open() fd = %d", fd);
        (*env)->ReleaseStringUTFChars(env, path, path_utf);
        if (fd == -1) {
            LOGE("Cannot open port");
            throwIOException(env, "open", openError);
            return NULL;
        }
    }

    /* Configure device */
    {
        struct termios cfg;
        LOGD("Configuring serial port");
        if (tcgetattr(fd, &cfg)) {
            int configError = errno;
            LOGE("tcgetattr() failed");
            close(fd);
            throwIOException(env, "tcgetattr", configError);
            return NULL;
        }

        // Initialize termios struct
        cfmakeraw(&cfg);
        cfg.c_cflag |= CLOCAL | CREAD;

        // Set data bits
        cfg.c_cflag &= ~CSIZE;
        switch (databits) {
            case 5:
                cfg.c_cflag |= CS5;
                break;
            case 6:
                cfg.c_cflag |= CS6;
                break;
            case 7:
                cfg.c_cflag |= CS7;
                break;
            case 8:
                cfg.c_cflag |= CS8;
                break;
            default:
                LOGE("Invalid data bits");
                close(fd);
                return NULL;
        }

        // Set stop bits
        switch (stopbits) {
            case 1:
                cfg.c_cflag &= ~CSTOPB;
                break;
            case 2:
                cfg.c_cflag |= CSTOPB;
                break;
            default:
                LOGE("Invalid stop bits");
                close(fd);
                return NULL;
        }

        switch (parity) {
            case 0:
                cfg.c_cflag &= ~PARENB;    //PARITY OFF
                break;
            case 1:
                cfg.c_cflag |= (PARODD | PARENB);   //PARITY ODD
                cfg.c_iflag &= ~IGNPAR;
                cfg.c_iflag |= PARMRK;
                cfg.c_iflag |= INPCK;
                break;
            case 2:
                cfg.c_iflag &= ~(IGNPAR | PARMRK); //PARITY EVEN
                cfg.c_iflag |= INPCK;
                cfg.c_cflag |= PARENB;
                cfg.c_cflag &= ~PARODD;
                break;
            case 3:
                //  PARITY SPACE
                cfg.c_iflag &= ~IGNPAR;             //  Make sure wrong parity is not ignored
                cfg.c_iflag |= PARMRK;              //  Marks parity error, parity error
                //  is given as three char sequence
                cfg.c_iflag |= INPCK;               //  Enable input parity checking
                cfg.c_cflag |= PARENB | CMSPAR;     //  Enable parity and set space parity
                cfg.c_cflag &= ~PARODD;             //
                break;
            case 4:
                //  PARITY MARK
                cfg.c_iflag &= ~IGNPAR;             //  Make sure wrong parity is not ignored
                cfg.c_iflag |= PARMRK;              //  Marks parity error, parity error
                //  is given as three char sequence
                cfg.c_iflag |= INPCK;               //  Enable input parity checking
                cfg.c_cflag |= PARENB | CMSPAR | PARODD;
                break;
            default:
                cfg.c_cflag &= ~PARENB;
                break;
        }

        // Set baud rate
        cfsetispeed(&cfg, speed);
        cfsetospeed(&cfg, speed);

        if (tcsetattr(fd, TCSANOW, &cfg)) {
            int configError = errno;
            LOGE("tcsetattr() failed");
            close(fd);
            throwIOException(env, "tcsetattr", configError);
            return NULL;
        }
    }

    /* Create a corresponding file descriptor */
    {
        jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
        if (cFileDescriptor == NULL) {
            close(fd);
            return NULL;
        }
        jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
        if (iFileDescriptor == NULL || descriptorID == NULL) {
            close(fd);
            return NULL;
        }
        mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
        if (mFileDescriptor == NULL) {
            close(fd);
            return NULL;
        }
        (*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint)fd);
        if ((*env)->ExceptionCheck(env)) {
            close(fd);
            return NULL;
        }
    }

    return mFileDescriptor;
}

JNIEXPORT void JNICALL Java_com_cl_serialportlibrary_SerialPort_close
        (JNIEnv *env, jobject thiz)
{
    jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    if (SerialPortClass == NULL || FileDescriptorClass == NULL) {
        return;
    }

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    if (mFdID == NULL || descriptorID == NULL) {
        return;
    }

    jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
    if (mFd == NULL) {
        return;
    }
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    if (descriptor >= 0) {
        LOGD("close(fd = %d)", descriptor);
        close(descriptor);
        (*env)->SetIntField(env, mFd, descriptorID, (jint)-1);
    }
}
