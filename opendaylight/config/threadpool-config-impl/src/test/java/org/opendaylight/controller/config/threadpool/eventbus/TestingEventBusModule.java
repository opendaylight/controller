package org.opendaylight.controller.config.threadpool.eventbus;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.AbstractMockedModule;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.threadpool.util.CloseableEventBus;
import org.opendaylight.controller.config.yang.threadpool.EventBusServiceInterface;
import org.opendaylight.controller.config.yang.threadpool.impl.EventBusModuleMXBean;

public class TestingEventBusModule extends AbstractMockedModule implements Module, EventBusServiceInterface,
        EventBusModuleMXBean {

    public TestingEventBusModule(DynamicMBeanWithInstance old, ModuleIdentifier id) {
        super(old, id);
    }

    @Override
    protected AutoCloseable prepareMockedInstance() throws Exception {
        CloseableEventBus bus = mock(CloseableEventBus.class);
        doNothing().when(bus).close();
        return bus;
    }

}
