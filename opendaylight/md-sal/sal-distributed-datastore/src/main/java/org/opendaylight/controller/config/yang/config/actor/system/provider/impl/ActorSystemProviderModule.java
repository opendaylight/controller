package org.opendaylight.controller.config.yang.config.actor.system.provider.impl;

import akka.actor.ActorSystem;
import akka.osgi.BundleDelegatingClassLoader;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.FileAkkaConfigurationReader;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

public class ActorSystemProviderModule extends AbstractActorSystemProviderModule {
    private static final String ACTOR_SYSTEM_NAME = "opendaylight-cluster-data";
    private static final String CONFIGURATION_NAME = "odl-cluster-data";
    private static final Logger LOG = LoggerFactory.getLogger(ActorSystemProviderModule.class);

    private BundleContext bundleContext;

    public ActorSystemProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ActorSystemProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, ActorSystemProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Creating new ActorSystem");

        BundleDelegatingClassLoader classLoader = new BundleDelegatingClassLoader(bundleContext.getBundle(),
                Thread.currentThread().getContextClassLoader());

        AkkaConfigurationReader configurationReader = new FileAkkaConfigurationReader();
        ActorSystem actorSystem = ActorSystem.create(ACTOR_SYSTEM_NAME,
                ConfigFactory.load(configurationReader.read()).getConfig(CONFIGURATION_NAME), classLoader);

        return new ActorSystemProviderImpl(actorSystem);
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class ActorSystemProviderImpl implements ActorSystemProvider, AutoCloseable {
        private final ActorSystem actorSystem;

        ActorSystemProviderImpl(ActorSystem actorSystem) {
            this.actorSystem = actorSystem;
        }

        @Override
        public ActorSystem getActorSystem() {
            return actorSystem;
        }

        @Override
        public void close() {
            actorSystem.shutdown();
            try {
                actorSystem.awaitTermination(Duration.create(10, TimeUnit.SECONDS));
            } catch (Exception e) {
                LOG.warn("Error awaiting actor termination", e);
            }
        }
    }
}