package com.zzstack.paas.underlying.redis.loadbalance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zzstack.paas.underlying.redis.utils.ServerMode;

public class WeightedRRLoadBalancer {
    
    private static Logger logger = LoggerFactory.getLogger(WeightedRRLoadBalancer.class);

    // 约定的invoker和权重的键值对
    private final List<Node> nodes;
    private final ServerMode serverMode;
    
    private ReentrantLock lock = null;

    public WeightedRRLoadBalancer(List<Node> items, ServerMode serverMode) {
        if (items != null && !items.isEmpty()) {
            nodes = items;
        } else {
            nodes = null;
        }
        
        this.serverMode = serverMode;
        this.lock = new ReentrantLock();
    }

    /**
     * 算法逻辑： 1. 对于每个请求，遍历集群中的所有可用后端，对于每个后端peer执行： peer->current_weight +=
     * peer->effecitve_weight. 同时累加所有peer的effective_weight，保存为total。
     * 
     * 2. 从集群中选出current_weight最大的peer，作为本次选定的后端;
     * 
     * 3. 对于本次选定的后端，执行：peer->current_weight -= total。
     */
    public Holder select() {
        if (!checkNodes())
            return null;
        else if (nodes.size() == 1) {
            if (nodes.get(0).getHolder().isAvalable())
                return nodes.get(0).getHolder();
            else
                return null;
        }
        Integer total = 0;
        Node nodeOfMaxWeight = null;
        lock.lock();
        try {
            for (Node node : nodes) {
                total += node.getEffectiveWeight();
                node.setCurrentWeight(node.getCurrentWeight() + node.getEffectiveWeight());
    
                if (nodeOfMaxWeight == null) {
                    nodeOfMaxWeight = node;
                } else {
                    nodeOfMaxWeight = nodeOfMaxWeight.compareTo(node) > 0 ? nodeOfMaxWeight : node;
                }
            }
    
            nodeOfMaxWeight.setCurrentWeight(nodeOfMaxWeight.getCurrentWeight() - total);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
        return nodeOfMaxWeight.getHolder();
    }
    
    public List<Holder> getAllHolder() {
        List<Holder> list = new ArrayList<Holder>();
        for (Node node : nodes) {
            Holder holder = node.getHolder();
            if (holder != null) {
                list.add(holder);
            }
        }
        
        return list;
    }
    
    public boolean resetWeight(Map<String, Integer> weightMap) {
        boolean result = true;
        
        if (!checkResetWeight(weightMap))
            return false;
        
        lock.lock();
        try {
            for (Node node : nodes) {
                String id = node.getId();
                Integer weight = weightMap.get(id);
                if (weight != null) {
                    node.resetWeight(weight);
                    
                    String info = String.format("id:%s, weight:%d", id, weight);
                    logger.info(info);
                } else {
                    String info = String.format("id:%s, weight == null ......", id);
                    logger.info(info);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = false;
        } finally {
            lock.unlock();
        }
        return result;
    }
    
    private boolean checkResetWeight(Map<String, Integer> weightMap) {
        int zeroWeightCnt = 0, minusWeightCnt = 0;
        
        Set<Entry<String, Integer>> entrySet = weightMap.entrySet();
        for (Entry<String, Integer> entry : entrySet) {
            int weight = entry.getValue();
            if (weight == 0)
                ++zeroWeightCnt;
            
            if (weight < 0)
                ++minusWeightCnt;
        }
        
        boolean res = (zeroWeightCnt == weightMap.size() || minusWeightCnt > 0) ? false : true;
        if (!res) {
            logger.error("输入权值不合法:权值全为0或存在负数");
        }
        
        return res;
    }

    public void onInvokeSuccess(Holder invoker) {
        if (checkNodes()) {
            nodes.stream().filter((Node node) -> invoker.id().equals(node.getHolder().id())).findFirst().get()
                    .onInvokeSuccess();
        }
    }

    public void onInvokeFail(Holder invoker) {
        if (checkNodes()) {
            nodes.stream().filter((Node node) -> invoker.id().equals(node.getHolder().id())).findFirst().get()
                    .onInvokeFail();
        }
    }

    private boolean checkNodes() {
        return (nodes != null && nodes.size() > 0);
    }

    public void print() {
        if (checkNodes()) {
            final StringBuffer out = new StringBuffer("[");
            
            Iterator<Node> it = nodes.iterator();
            while (it.hasNext()) {
                Node node = it.next();
                if (out.length() > 1) out.append(",");
                
                out.append("{");
                out.append("id:").append(node.getHolder().id()).append(",");
                out.append("currWht:").append(node.getCurrentWeight()).append(",");
                out.append("effWht:").append(node.getEffectiveWeight()).append(",");
                out.append("wht:").append(node.getWeight());
                out.append("}");
            }
            
            out.append("]");
            System.out.print(out);
        }
    }

    public ServerMode getServerMode() {
        return serverMode;
    }
    
    public void destroy() {
        if (nodes == null) return;
        
        for (Node node : nodes) {
            node.destroy();
        }
    }

}
