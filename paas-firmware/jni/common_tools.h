#ifndef _COMMON_TOOLS_H_
#define _COMMON_TOOLS_H_

#include <stdlib.h>
#include <stdio.h>
#include <cpuid.h>
#include <string>

#ifdef __cplusplus
extern "C" {
#endif

using namespace std;

static const std::string DES_KEY = "abcd.1234";
static const char * HEADER_CPU_ID = "cpu_id";
static const char * HEADER_VALID_DATE = "valid_date";
static const char * LISENCE_FILE = "system.license";

static inline void cpu_info(char *buf) {
    unsigned int level = 1;
    unsigned eax = 3, ebx = 0, ecx = 0, edx = 0;  // processor serial number
    __get_cpuid(level, &eax, &ebx, &ecx, &edx);

    // byte swap
    int first = ((eax >> 24) & 0xff) | ((eax << 8) & 0xff0000) | ((eax >> 8) & 0xff00) | ((eax << 24) & 0xff000000);
    int last = ((edx >> 24) & 0xff) | ((edx << 8) & 0xff0000) | ((edx >> 8) & 0xff00) | ((edx << 24) & 0xff000000);

    snprintf(buf, 36, "%08X-%08X-%08X-%08X", first, last, ecx, edx);
}

static inline int date_to_decimal(const std::string& date)
{
    if (date.length() < 10) return 0;

    // date format YYYY-MM-DD
    std::string year = date.substr(0, 4);
    std::string month = date.substr(5, 2);
    std::string day = date.substr(8, 2);

    return atoi(year.c_str()) * 10000 + atoi(month.c_str()) * 100 + atoi(day.c_str());
}

static inline int systime_to_decimal()
{
    time_t nowsec = time(NULL);
    struct tm* nowTime = localtime(&nowsec);

    int year = nowTime->tm_year + 1900;
    int month = nowTime->tm_mon + 1;
    int day = nowTime->tm_mday;

    return year * 10000 + month * 100 + day;
}

#ifdef __cplusplus
}
#endif

#endif 
