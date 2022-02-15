#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string>

#include "firmware.h"
#include "des.h"
#include "base64.h"
#include "common_tools.h"
#include "cjson.h"

static inline std::string jbytearr2str(JNIEnv* env, jbyteArray& bytes)
{
    int len = env->GetArrayLength(bytes);
    std::string s;
    s.assign((char*) env->GetByteArrayElements(bytes, JNI_FALSE), len);
    // cout << s << endl;
    return s;
}

JNIEXPORT jstring JNICALL Java_com_zzstack_paas_underlying_firmware_FirmwareNative_cpuid(JNIEnv *env, jobject obj)
{
    char buf[36] = {0};
    cpu_info(buf);
    return env->NewStringUTF(buf);
}

JNIEXPORT jstring JNICALL Java_com_zzstack_paas_underlying_firmware_FirmwareNative_encrypt(JNIEnv *env, jobject obj, jstring text)
{
    DES des(DES_KEY);

    jboolean isCopy = true;
    const char *converted = (env)->GetStringUTFChars(text, &isCopy);
    std::string s = std::string(converted);

    std::string des_encrypt = des.Encrypt(s);
    std::string base64_encrypt = base64_encode(des_encrypt.c_str(), des_encrypt.length());

    return env->NewStringUTF(base64_encrypt.c_str());
}

JNIEXPORT void JNICALL Java_com_zzstack_paas_underlying_firmware_FirmwareNative_checkFirmware(JNIEnv *env, jobject obj, jbyteArray bytes)
{
    DES des(DES_KEY);

    std::string s = jbytearr2str(env, bytes);
    // cout << s << endl;

    std::string base64_decrypt = base64_decode(s);
    std::string decrypt = des.Decrypt(base64_decrypt);

    CJson::cJSON * root_obj = CJson::cJSON_Parse(decrypt.c_str());
    CJson::cJSON * cpu_id_obj = CJson::cJSON_GetObjectItem(root_obj, HEADER_CPU_ID);
    CJson::cJSON * valid_date_obj = CJson::cJSON_GetObjectItem(root_obj, HEADER_VALID_DATE);

    std::string cpu_id(cpu_id_obj->valuestring);
    std::string valid_date(valid_date_obj->valuestring);

    // printf("cpu_id:%s\n", cpu_id.c_str());
    // printf("valid_date:%s\n", valid_date.c_str());

    char buf[36] = {0};
    cpu_info(buf);

    bool cpu_check_ok = strcmp(buf, cpu_id.c_str()) == 0;
    if (!cpu_check_ok) {
        // cpu serial id check fail
        cout << "checkFirmware cpu_id fail, exit!" << endl;
        exit(-1);
    }

    int lisence_date = date_to_decimal(valid_date);
    int sys_date = systime_to_decimal();

    if (sys_date > lisence_date) {
        // lisence is expired!
        cout << "checkFirmware licence_date fail, exit!" << endl;
        exit(-1);
    }
    
    cout << "checkFirmware ok!" << endl;
}
