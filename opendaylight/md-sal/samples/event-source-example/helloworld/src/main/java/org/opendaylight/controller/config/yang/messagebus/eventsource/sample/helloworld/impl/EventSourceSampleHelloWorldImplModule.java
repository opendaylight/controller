package org.opendaylight.controller.config.yang.messagebus.eventsource.sample.helloworld.impl;
public class EventSourceSampleHelloWorldImplModule extends org.opendaylight.controller.config.yang.messagebus.eventsource.sample.helloworld.impl.AbstractEventSourceSampleHelloWorldImplModule {
    public EventSourceSampleHelloWorldImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public EventSourceSampleHelloWorldImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.messagebus.eventsource.sample.helloworld.impl.EventSourceSampleHelloWorldImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
