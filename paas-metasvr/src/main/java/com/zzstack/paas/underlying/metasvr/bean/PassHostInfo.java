package com.zzstack.paas.underlying.metasvr.bean;

import com.zzstack.paas.underlying.utils.FixHeader;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * 应用的主机的使用监控信息
 */
public class PassHostInfo extends BeanMapper {
    /**
     * 时间戳
     */
    private Long ts;
    /**
     * 实例ID
     */
    private String instantId;
    /**
     * 内存总量 单位: 字节
     */
    private Long totalMemory;
    /**
     * 已使用内存总量 单位: 字节
     */
    private Long usedMemory;
    /**
     * cpu-user 用户态占用的cpu
     */
    private Double usedUserCpu;
    /**
     * cpu-sys 系统态占用的cpu
     */
    private Double usedSysCpu;
    /**
     * cpu-id 系统态使用的cpu
     */
    private Double cpuIdle;
    /**
     * disk-total  /home目录下磁盘总量
     */
    private Long totalDisk;
    /**
     * disk-used /home目录下已使用磁盘总量
     */
    private Long usedDisk;
    /**
     * disk-unused /home目录下已使用磁盘总量
     */
    private Long unusedDisk;
    /**
     * disk-userused $HOME目录下已使用磁盘总量
     */
    private Long userUsedDisk;
    /**
     * 输入带宽 kb/s
     */
    private Double inputBandWidth;
    /**
     * 输出带宽 kb/s
     */
    private Double outputBandWidth;


    public PassHostInfo() {
        super();
    }

    public PassHostInfo(Long ts, String instantId, Long totalMemory, Long usedMemory, Double usedUserCpu, Double usedSysCpu,
                        Double cpuIdle, Long totalDisk, Long usedDisk, Long unusedDisk, Long userUsedDisk,
                        Double inputBandWidth, Double outputBandWidth) {
        this.ts = ts;
        this.instantId = instantId;
        this.totalMemory = totalMemory;
        this.usedMemory = usedMemory;
        this.usedUserCpu = usedUserCpu;
        this.usedSysCpu = usedSysCpu;
        this.cpuIdle = cpuIdle;
        this.totalDisk = totalDisk;
        this.usedDisk = usedDisk;
        this.unusedDisk = unusedDisk;
        this.userUsedDisk = userUsedDisk;
        this.inputBandWidth = inputBandWidth;
        this.outputBandWidth = outputBandWidth;
    }

    public Long getTs() {
        return ts;
    }

    public void setTs(Long ts) {
        this.ts = ts;
    }

    public String getInstantId() {
        return instantId;
    }

    public void setInstantId(String instantId) {
        this.instantId = instantId;
    }

    public Long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(Long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public Long getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(Long usedMemory) {
        this.usedMemory = usedMemory;
    }

    public Double getUsedUserCpu() {
        return usedUserCpu;
    }

    public void setUsedUserCpu(Double usedUserCpu) {
        this.usedUserCpu = usedUserCpu;
    }

    public Double getUsedSysCpu() {
        return usedSysCpu;
    }

    public void setUsedSysCpu(Double usedSysCpu) {
        this.usedSysCpu = usedSysCpu;
    }

    public Double getCpuIdle() {
        return cpuIdle;
    }

    public void setCpuIdle(Double cpuIdle) {
        this.cpuIdle = cpuIdle;
    }

    public Long getTotalDisk() {
        return totalDisk;
    }

    public void setTotalDisk(Long totalDisk) {
        this.totalDisk = totalDisk;
    }

    public Long getUsedDisk() {
        return usedDisk;
    }

    public void setUsedDisk(Long usedDisk) {
        this.usedDisk = usedDisk;
    }

    public Long getUnusedDisk() {
        return unusedDisk;
    }

    public void setUnusedDisk(Long unusedDisk) {
        this.unusedDisk = unusedDisk;
    }

    public Long getUserUsedDisk() {
        return userUsedDisk;
    }

    public void setUserUsedDisk(Long userUsedDisk) {
        this.userUsedDisk = userUsedDisk;
    }

    public Double getInputBandWidth() {
        return inputBandWidth;
    }

    public void setInputBandWidth(Double inputBandWidth) {
        this.inputBandWidth = inputBandWidth;
    }

    public Double getOutputBandWidth() {
        return outputBandWidth;
    }

    public void setOutputBandWidth(Double outputBandWidth) {
        this.outputBandWidth = outputBandWidth;
    }

    public static PassHostInfo convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty()) {
            return null;
        }
        long ts = getFixDataAsLong(mapper, FixHeader.HEADER_TS);
        String instantId = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
        Long totalMemory = getFixDataAsLong(mapper, FixHeader.HEADER_MEMORY_TOTAL);
        Long usedMemory = getFixDataAsLong(mapper, FixHeader.HEADER_MEMORY_USED);
        Double usedCpuUser = getFixDataAsDouble(mapper, FixHeader.HEADER_USED_CPU_USER);
        Double usedCpuSys = getFixDataAsDouble(mapper, FixHeader.HEADER_USED_CPU_SYS);
        Double cpuIdle = getFixDataAsDouble(mapper, FixHeader.HEADER_CPU_IDLE);
        Long totalDisk = getFixDataAsLong(mapper, FixHeader.HEADER_TOTAL_DISK);
        Long usedDisk = getFixDataAsLong(mapper, FixHeader.HEADER_USED_DISK);
        Long unusedDisk = getFixDataAsLong(mapper, FixHeader.HEADER_UNUSED_DISK);
        Long userUsedDisk = getFixDataAsLong(mapper, FixHeader.HEADER_USER_USED_DISK);
        Double inputBandWidth = getFixDataAsDouble(mapper, FixHeader.HEADER_INPUT_BANDWIDTH);
        Double outputBandWidth = getFixDataAsDouble(mapper, FixHeader.HEADER_OUTPUT_BANDWIDTH);
        return new PassHostInfo(ts, instantId, totalMemory, usedMemory, usedCpuUser, usedCpuSys,
                cpuIdle, totalDisk, usedDisk, unusedDisk, userUsedDisk, inputBandWidth, outputBandWidth);
    }

    public JsonObject toJson() {
        JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_TS, this.ts);
        retval.put(FixHeader.HEADER_INST_ID, this.instantId);
        retval.put(FixHeader.HEADER_MEMORY_TOTAL, this.totalMemory);
        retval.put(FixHeader.HEADER_MEMORY_USED, this.usedMemory);
        retval.put(FixHeader.HEADER_USED_CPU_USER, this.usedUserCpu);
        retval.put(FixHeader.HEADER_USED_CPU_SYS, this.usedSysCpu);
        retval.put(FixHeader.HEADER_CPU_IDLE, this.cpuIdle);
        retval.put(FixHeader.HEADER_TOTAL_DISK, this.totalDisk);
        retval.put(FixHeader.HEADER_USED_DISK, this.usedDisk);
        retval.put(FixHeader.HEADER_UNUSED_DISK, this.unusedDisk);
        retval.put(FixHeader.HEADER_USER_USED_DISK, this.userUsedDisk);
        retval.put(FixHeader.HEADER_INPUT_BANDWIDTH, this.inputBandWidth);
        retval.put(FixHeader.HEADER_OUTPUT_BANDWIDTH, this.outputBandWidth);
        return retval;
    }

}
