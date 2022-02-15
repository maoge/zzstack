package dbtest;

import org.mybatis.guice.XMLMyBatisModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zzstack.paas.underlying.dbclient.ActiveStandbyDBSrcPool;

import dbtest.service.BaseDataAccessService;
import dbtest.service.ServiceType;
import dbtest.service.exception.InitializeException;

public abstract class BaseDaoInject {
    
    private static Logger logger = LoggerFactory.getLogger(BaseDaoInject.class);
    
    private Injector injector;

    public BaseDaoInject(ServiceType serviceType) {
        if (serviceType == null) {
            logger.error("serviceType is null, please check property[app.service.impl] is in (database,mq,multi)");
            throw new InitializeException("serviceType is null, please check property[app.service.impl] is in (database,mq,multi)");
        }
        injector = Guice.createInjector(
                new XMLMyBatisModule() {
                    @Override
                    protected void initialize() {
                        setClassPathResource(ActiveStandbyDBSrcPool.getMyBatisConf());
                        bind(BaseDataAccessService.class).to(serviceType.getServiceCls());
                        bindService(binder());
                    }
                }
        );
    }

    protected abstract void bindService(Binder binder);

    public <T> T getInstance(Class<T> cls) {
        return injector.getInstance(cls);
    }

}
