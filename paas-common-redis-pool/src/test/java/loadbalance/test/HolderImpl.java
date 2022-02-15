package loadbalance.test;

import com.zzstack.paas.underlying.redis.loadbalance.Holder;

public class HolderImpl implements Holder {
    
    private String id;
    
    public HolderImpl(String id) {
        super();
        this.id = id;
    }

    @Override
    public boolean isAvalable() {
        return true;
    }
    
    @Override
    public void destroy() {
        
    }

    @Override
    public String id() {
        return this.id;
    }

}
