package dbtest.service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ServiceType {

    DATABASE("database", DefaultDataAccessServiceImpl.class);
    // MQ("mq", MqDataAccessServiceImpl.class),
    // MULTI("multi", MultiDataAccessServiceImpl.class);

    private String type;

    private Class<? extends BaseDataAccessService> serviceCls;

    private static final Map<String, ServiceType> map = new HashMap<>();

    static {
        for (ServiceType s : EnumSet.allOf(ServiceType.class)) {
            map.put(s.type, s);
        }
    }

    ServiceType(String type, Class<? extends BaseDataAccessService> serviceCls) {
        this.type = type;
        this.serviceCls = serviceCls;
    }

    public String getType() {
        return type;
    }

    public Class<? extends BaseDataAccessService> getServiceCls() {
        return serviceCls;
    }

    public static ServiceType getType(String type) {
        return map.get(type);
    }

}
