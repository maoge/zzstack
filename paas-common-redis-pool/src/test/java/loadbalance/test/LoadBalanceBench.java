package loadbalance.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zzstack.paas.underlying.redis.loadbalance.Holder;
import com.zzstack.paas.underlying.redis.loadbalance.Node;
import com.zzstack.paas.underlying.redis.loadbalance.WeightedRRLoadBalancer;
import com.zzstack.paas.underlying.redis.utils.ServerMode;

public class LoadBalanceBench {

    public static void main(String[] args) {
        
        Node node1 = new Node(new HolderImpl("a"), 0, "a");
        Node node2 = new Node(new HolderImpl("b"), 100, "b");
        List<Node> nodes = new ArrayList<Node>();
        nodes.add(node1);
        nodes.add(node2);

        WeightedRRLoadBalancer roundRobin = new WeightedRRLoadBalancer(nodes, ServerMode.CLUSTER);
        Integer times = 10;
        
        long t1 = System.nanoTime();
        for (int i = 1; i <= times; i++) {
            roundRobin.print();
            Holder invoker = roundRobin.select();
            System.out.print(new StringBuffer("    ").append(invoker.id()).append("    "));
            roundRobin.print();
            System.out.println();
        }
        long t2 = System.nanoTime();
        System.out.println("avg cost:" + (t2 - t1) / times);
        
        Map<String, Integer> weightMap = new HashMap<String, Integer>();
        weightMap.put("a", 100);
        weightMap.put("b", 0);
        roundRobin.resetWeight(weightMap);
        for (int i = 1; i <= times; i++) {
            roundRobin.print();
            Holder invoker = roundRobin.select();
            System.out.print(new StringBuffer("    ").append(invoker.id()).append("    "));
            roundRobin.print();
            System.out.println();
        }
    }

}
