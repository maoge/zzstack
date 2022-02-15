package dbtest;

import com.google.inject.Binder;

import dbtest.service.ServiceType;
import dbtest.service.exception.InitializeException;

public class AccInjector extends BaseDaoInject {
    
    private static final String SERVICE_IMPL = "database";
    
    private static AccService accService = null;

    public AccInjector() {
        super(ServiceType.getType(SERVICE_IMPL));
    }

    @Override
    protected void bindService(Binder binder) {
        binder.bind(AccService.class).to(AccServiceImpl.class);
    }
    
    public static AccService getAccServiceInstance() {
        if (accService == null) {
            synchronized (AccInjector.class) {
                if (accService == null) {
                    try {
                        accService = new AccInjector().getInstance(AccService.class);
                    } catch (Exception e) {
                        throw new InitializeException(e);
                    }
                }
            }
        }
        return accService;
    }

}
