#include "common_tools.h"
#include "cjson.h"

void write_collect_info(const char *buf)
{
    // 打开一个文本文件，允许读写文件。如果文件已存在，则文件会被截断为零长度，如果文件不存在，则会创建一个新文件
    FILE *file = fopen(FIRMWARE_FILE, "w+");
    if (fputs(buf, file) == EOF)
        printf("lisence write fail ......\n");
    
    fclose(file);
    file = NULL;
}

int main(int argc, char *argv[])
{
    const char * param_date = argv[2];
    char buf[36] = {0};
    cpu_info(buf);

    CJson::cJSON* root = CJson::cJSON_CreateObject();
    CJson::cJSON_AddStringToObject(root, HEADER_CPU_ID, buf);
    char * firmware = CJson::cJSON_Print(root);
    printf("firmware text:\n%s\n", firmware);

    write_collect_info(firmware);

    cJSON_free(firmware);
    CJson::cJSON_Delete(root);
    return 0;
}