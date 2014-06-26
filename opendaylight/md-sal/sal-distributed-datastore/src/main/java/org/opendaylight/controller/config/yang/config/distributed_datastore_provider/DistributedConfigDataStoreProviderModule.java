package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;

public class DistributedConfigDataStoreProviderModule extends org.opendaylight.controller.config.yang.config.distributed_datastore_provider.AbstractDistributedConfigDataStoreProviderModule {
    public DistributedConfigDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedConfigDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.distributed_datastore_provider.DistributedConfigDataStoreProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
     final ActorSystem actorSystem = ActorSystem.create("opendaylight-cluster-system", ConfigFactory
          .load().getConfig("ODLCluster"));


      final DistributedDataStore configDatastore = new DistributedDataStore(actorSystem, "config");
      getSchemaServiceDependency().registerSchemaServiceListener(configDatastore);

      final class AutoCloseableDistributedDataStore implements AutoCloseable {

        @Override
        public void close() throws Exception {
          actorSystem.shutdown();
        }
      }

      return new AutoCloseableDistributedDataStore();
    }

}
