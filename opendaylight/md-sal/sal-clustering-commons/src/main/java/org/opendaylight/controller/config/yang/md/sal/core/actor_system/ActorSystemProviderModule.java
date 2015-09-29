package org.opendaylight.controller.config.yang.md.sal.core.actor_system;

import akka.actor.ActorSystem;
import org.opendaylight.controller.cluster.ActorSystemFactory;
import org.osgi.framework.BundleContext;

public class ActorSystemProviderModule extends org.opendaylight.controller.config.yang.md.sal.core.actor_system.AbstractActorSystemProviderModule {
    private BundleContext bundleContext;

    public ActorSystemProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ActorSystemProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.md.sal.core.actor_system.ActorSystemProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final ActorSystem actorSystem = ActorSystemFactory.createActorSystem(bundleContext);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                actorSystem.shutdown();
            }
        };
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
