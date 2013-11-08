package org.opendaylight.controller.config.threadpool.naming;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.Closeable;
import java.io.IOException;

import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.threadpool.util.NamingThreadPoolFactory;
import org.opendaylight.controller.config.yang.threadpool.ThreadFactoryServiceInterface;
import org.opendaylight.controller.config.yang.threadpool.impl.NamingThreadFactoryModuleMXBean;

public class TestingNamingThreadPoolFactoryModule implements Module, ThreadFactoryServiceInterface,
        NamingThreadFactoryModuleMXBean {

    private final NamingThreadPoolFactory fact;

    public TestingNamingThreadPoolFactoryModule() throws IOException {
        fact = mock(NamingThreadPoolFactory.class);
        Thread thread = mock(Thread.class);
        doNothing().when(thread).start();
        doReturn(thread).when(fact).newThread(any(Runnable.class));
        doNothing().when(fact).close();
    }

    public TestingNamingThreadPoolFactoryModule(DynamicMBeanWithInstance old) {
        fact = (NamingThreadPoolFactory) old.getInstance();
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return new ModuleIdentifier(TestingNamingThreadPoolFactoryModule.class.getCanonicalName(), "mock");
    }

    @Override
    public String getNamePrefix() {
        return null;
    }

    @Override
    public void setNamePrefix(String arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validate() {
    }

    @Override
    public Closeable getInstance() {
        return fact;
    }

}
