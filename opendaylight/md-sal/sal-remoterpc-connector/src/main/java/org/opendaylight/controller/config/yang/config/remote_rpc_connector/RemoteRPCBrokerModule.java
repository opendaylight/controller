package org.opendaylight.controller.config.yang.config.remote_rpc_connector;

import org.opendaylight.controller.remote.rpc.RemoteRpcProviderFactory;
import org.opendaylight.controller.sal.core.api.Broker;
import org.osgi.framework.BundleContext;

public class RemoteRPCBrokerModule extends org.opendaylight.controller.config.yang.config.remote_rpc_connector.AbstractRemoteRPCBrokerModule {
  private BundleContext bundleContext;
  public RemoteRPCBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
    super(identifier, dependencyResolver);
  }

  public RemoteRPCBrokerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.remote_rpc_connector.RemoteRPCBrokerModule oldModule, java.lang.AutoCloseable oldInstance) {
    super(identifier, dependencyResolver, oldModule, oldInstance);
  }

  @Override
  public void customValidation() {
     // add custom validation form module attributes here.
  }

  @Override
  public java.lang.AutoCloseable createInstance() {
    Broker broker = getDomBrokerDependency();
    return RemoteRpcProviderFactory.createInstance(broker, bundleContext);
  }

  public void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }
}
