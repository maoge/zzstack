package com.zzstack.paas.underlying.metasvr.cluster.raft.util;

import com.zzstack.paas.underlying.raft.core.proto.RaftProto;

public class RaftAddressUtils {
    
    public static RaftProto.Server parseServer(String serverString) {
        String[] splitServer = serverString.split(":");
        String host = splitServer[0];
        Integer port = Integer.parseInt(splitServer[1]);
        Integer serverId = Integer.parseInt(splitServer[2]);
        RaftProto.Endpoint endPoint = RaftProto.Endpoint.newBuilder()
                .setHost(host).setPort(port).build();
        RaftProto.Server.Builder serverBuilder = RaftProto.Server.newBuilder();
        RaftProto.Server server = serverBuilder.setServerId(serverId).setEndpoint(endPoint).build();
        return server;
    }
    
    public static String parseClusterAddrs(String serverString) {
        String[] splitServer = serverString.split(",");
        StringBuilder res = new StringBuilder("list://");
        
        for (int i = 0; i < splitServer.length; ++i) {
            String server = splitServer[i];
            String[] split = server.split(":");
            if (i > 0) {
                res.append(",");
            }
                
            res.append(String.format("%s:%s", split[0], split[1]));
        }
        return res.toString();
    }

}
