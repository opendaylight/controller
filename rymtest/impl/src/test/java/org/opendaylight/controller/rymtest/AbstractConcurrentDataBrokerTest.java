package org.opendaylight.controller.rymtest;

public abstract class AbstractConcurrentDataBrokerTest extends AbstractBaseDataBrokerTest{
    private final boolean useMTDataTreeChangeListenerExecutor;

    protected AbstractConcurrentDataBrokerTest() {
        this(false);
    }

    protected AbstractConcurrentDataBrokerTest(final boolean useMTDataTreeChangeListenerExecutor) {
        this.useMTDataTreeChangeListenerExecutor = useMTDataTreeChangeListenerExecutor;
    }

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new ConcurrentDataBrokerTestCustomizer(useMTDataTreeChangeListenerExecutor);
    }

}
