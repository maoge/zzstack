package com.zzstack.paas.underlying.redis.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node implements Comparable<Node> {
    
    private static Logger logger = LoggerFactory.getLogger(Node.class);

    private final Holder holder;
    private volatile int weight;
    private volatile int effectiveWeight;
    private volatile int currentWeight;
    private String id;

    public Node(Holder holder, int weight, String id) {
        this.holder = holder;
        this.id = id;
        this.resetWeight(weight);
    }
    
    public void resetWeight(int weight) {
        this.weight = weight;
        this.effectiveWeight = weight;
        this.currentWeight = 0;
        
        logger.info("id:{}, weight:{}, effectiveWeight:{}", this.id, this.weight, this.effectiveWeight);
    }

    @Override
    public int compareTo(Node o) {
        return currentWeight > o.currentWeight ? 1 : (currentWeight == o.currentWeight ? 0 : -1);
    }

    public void onInvokeSuccess() {
        if (effectiveWeight < this.weight)
            effectiveWeight++;
    }

    public void onInvokeFail() {
        effectiveWeight--;
    }

    public Holder getHolder() {
        return holder;
    }

    public Integer getEffectiveWeight() {
        return effectiveWeight;
    }

    public void setEffectiveWeight(Integer effectiveWeight) {
        this.effectiveWeight = effectiveWeight;
    }

    public Integer getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(Integer currentWeight) {
        this.currentWeight = currentWeight;
    }

    public Integer getWeight() {
        return weight;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void destroy() {
        if (holder != null) {
            holder.destroy();
        }
    }

}
