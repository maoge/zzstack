package com.zzstack.paas.underlying.metasvr.bean;

import com.zzstack.paas.underlying.utils.FixHeader;
import io.vertx.core.json.JsonObject;

import java.util.Map;

/**
 * 应用的redis的使用监控信息
 */
public class PassRocketMqInfo extends BeanMapper {
    /**
     * 时间戳
     */
    private Long ts;

    /**
     * 组件实例ID或者组件集群ID
     */
    private String instantId;

    /**
     * 主题
     */
    private String topicName;

    /**
     * 消费组
     */
    private String consumeGroup;

    /**
     * 消息堆积量
     */
    private Long diffTotal;

    /**
     * 生产总量
     */
    private Long produceTotal;

    /**
     * 消费总量
     */
    private Long consumeTotal;

    /**
     * 消费速率
     */
    private Double consumeTps;

    /**
     * 生产速率
     */
    private Double produceTps;

    public PassRocketMqInfo() {
        super();
    }

    public PassRocketMqInfo(Long ts, String instantId, String topicName, String consumeGroup, Long diffTotal, Long produceTotal, Long consumeTotal, Double consumeTps, Double produceTps) {
        this.ts = ts;
        this.instantId = instantId;
        this.topicName = topicName;
        this.consumeGroup = consumeGroup;
        this.diffTotal = diffTotal;
        this.produceTotal = produceTotal;
        this.consumeTotal = consumeTotal;
        this.consumeTps = consumeTps;
        this.produceTps = produceTps;
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

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getConsumeGroup() {
        return consumeGroup;
    }

    public void setConsumeGroup(String consumeGroup) {
        this.consumeGroup = consumeGroup;
    }

    public Long getDiffTotal() {
        return diffTotal;
    }

    public void setDiffTotal(Long diffTotal) {
        this.diffTotal = diffTotal;
    }

    public Long getProduceTotal() {
        return produceTotal;
    }

    public void setProduceTotal(Long produceTotal) {
        this.produceTotal = produceTotal;
    }

    public Long getConsumeTotal() {
        return consumeTotal;
    }

    public void setConsumeTotal(Long consumeTotal) {
        this.consumeTotal = consumeTotal;
    }

    public Double getConsumeTps() {
        return consumeTps;
    }

    public void setConsumeTps(Double consumeTps) {
        this.consumeTps = consumeTps;
    }

    public Double getProduceTps() {
        return produceTps;
    }

    public void setProduceTps(Double produceTps) {
        this.produceTps = produceTps;
    }

    public static PassRocketMqInfo convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty()) {
            return null;
        }
        long ts = getFixDataAsLong(mapper, FixHeader.HEADER_TS);
        String instantId = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
        String topicName = getFixDataAsString(mapper, FixHeader.HEADER_TOPIC_NAME);
        String consumeGroup = getFixDataAsString(mapper, FixHeader.HEADER_CONSUME_GROUP);
        long diffTotal = getFixDataAsLong(mapper, FixHeader.HEADER_DIFF_TOTAL);
        long produceTotal = getFixDataAsLong(mapper, FixHeader.HEADER_PRODUCE_TOTAL);
        long consumeTotal = getFixDataAsLong(mapper, FixHeader.HEADER_CONSUME_TOTAL);
        double consumeTps = getFixDataAsDouble(mapper, FixHeader.HEADER_PRODUCE_TPS);
        double produceTps = getFixDataAsDouble(mapper, FixHeader.HEADER_CONSUME_TPS);
        return new PassRocketMqInfo(ts, instantId, topicName, consumeGroup, diffTotal, produceTotal, consumeTotal, consumeTps, produceTps);
    }

    public JsonObject toJson() {
        JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_TS, this.ts);
        retval.put(FixHeader.HEADER_INST_ID, this.instantId);
        retval.put(FixHeader.HEADER_TOPIC_NAME, this.topicName);
        retval.put(FixHeader.HEADER_CONSUME_GROUP, this.consumeGroup);
        retval.put(FixHeader.HEADER_DIFF_TOTAL, this.diffTotal);
        retval.put(FixHeader.HEADER_PRODUCE_TOTAL, this.produceTotal);
        retval.put(FixHeader.HEADER_CONSUME_TOTAL, this.consumeTotal);
        retval.put(FixHeader.HEADER_PRODUCE_TPS, this.produceTps);
        retval.put(FixHeader.HEADER_CONSUME_TPS, this.consumeTps);
        return retval;
    }

}
