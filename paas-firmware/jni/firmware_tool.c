#include "common_tools.h"
#include "cjson.h"
#include "des.h"
#include "base64.h"

const char * PARAM_DATE_FLAG = "-d";
const char * PARAM_FIRMWARE_FILE_FLAG = "-f";

void print_help()
{
    printf("usage:\n");
    printf("firmware_tool -d 2021-07-30 -f ./firmware.txt\n");
}

bool check_params(int argc, char *argv[])
{
    const char * param_date_flag = argv[1];
    const char * param_date = argv[2];

    if (strcmp(param_date_flag, PARAM_DATE_FLAG) != 0)
        return false;

    if (strlen(param_date) == 0)
    {
        printf("-d specified value is null ......\n");
        return false;
    }

    return true;
}

bool cpu_info_firmware_file(char *buf, const char * firmware_file)
{
    FILE *fp = fopen(firmware_file, "r");
    if (fp == NULL) {
        printf("fireware file: %s not exists ......\n", firmware_file);
        return false;
    }
    
    char buff[256] = {0};
    fread(buff, 255, 1, fp);
    
    fclose(fp), fp = NULL;
    
    CJson::cJSON* json = CJson::cJSON_Parse(buff);
    CJson::cJSON * node_cpu = CJson::cJSON_GetObjectItem(json, HEADER_CPU_ID);
    if (node_cpu) {
        const char * cpu_info = node_cpu->valuestring;
        strncpy(buf, cpu_info, strlen(cpu_info));
    }
    
    CJson::cJSON_Delete(json);
    
    return true;
}

void write_lisence(const char *buf)
{
    // 打开一个文本文件，允许读写文件。如果文件已存在，则文件会被截断为零长度，如果文件不存在，则会创建一个新文件
    FILE *file = fopen(LISENCE_FILE, "w+");
    if (fputs(buf, file) == EOF)
        printf("lisence write fail ......\n");
    
    fclose(file);
    file = NULL;
}

int main(int argc, char *argv[])
{
    if (argc < 3 || !check_params(argc, argv))
    {
        print_help();
        return -1;
    }
    
    const char * param_date = argv[2];
    char buf[36] = {0};
    
    if (argc >= 5) {
        const char * param_firmware_file = argv[4];
        if (!cpu_info_firmware_file(buf, param_firmware_file)) { return -1; }
    } else {
        cpu_info(buf);
    }

    CJson::cJSON* root = CJson::cJSON_CreateObject();
    CJson::cJSON_AddStringToObject(root, HEADER_CPU_ID, buf);
    CJson::cJSON_AddStringToObject(root, HEADER_VALID_DATE, param_date);
    char * firmware = CJson::cJSON_Print(root);
    printf("firmware text:\n");
    printf("%s\n", firmware);

    DES des(DES_KEY);
    std::string des_encrypt = des.Encrypt(firmware);

    const char * encrypt_buf = des_encrypt.c_str();
    std::string base64_encrypt = base64_encode(encrypt_buf, strlen(encrypt_buf));
    
    printf("encrypted firmware:\n");
    printf("%s\n", base64_encrypt.c_str());

    write_lisence(base64_encrypt.c_str());

    cJSON_free(firmware);
    CJson::cJSON_Delete(root);
    return 0;
}