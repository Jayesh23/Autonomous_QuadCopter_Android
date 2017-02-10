#include <jni.h>
#include <string>

extern "C"
jstring
Java_com_example_dell_sensor_1data_1send_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
