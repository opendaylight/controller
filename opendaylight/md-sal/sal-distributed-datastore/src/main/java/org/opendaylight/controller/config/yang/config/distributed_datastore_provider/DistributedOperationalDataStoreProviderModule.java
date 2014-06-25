package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;

public class DistributedOperationalDataStoreProviderModule extends org.opendaylight.controller.config.yang.config.distributed_datastore_provider.AbstractDistributedOperationalDataStoreProviderModule {
    public DistributedOperationalDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedOperationalDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.distributed_datastore_provider.DistributedOperationalDataStoreProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

  @Override
  public java.lang.AutoCloseable createInstance() {
    final ActorSystem actorSystem = ActorSystem.create("opendaylight-cluster", ConfigFactory
        .load().getConfig("opendaylight-cluster-system"));
    final DistributedDataStore operationalStore = new DistributedDataStore();
    //TODO: change to below datastore once it is merged
    //new DistributedDataStore(actorSystem, "operational");

    final class AutoCloseableDistributedDataStore implements AutoCloseable {

      @Override
      public void close() throws Exception {
        actorSystem.shutdown();
      }
    }

    return new AutoCloseableDistributedDataStore();
  }

}
