package org.opendaylight.controller.config.yang.config.remote_rpc_connector;

import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.DefaultAkkaConfigurationReader;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderConfig;
import org.opendaylight.controller.remote.rpc.RemoteRpcProviderFactory;
import org.opendaylight.controller.sal.core.api.Broker;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.framework.ServiceReference;
import java.util.Dictionary;
import java.io.IOException;

public class RemoteRPCBrokerModule extends org.opendaylight.controller.config.yang.config.remote_rpc_connector.AbstractRemoteRPCBrokerModule {
  private BundleContext bundleContext;
  private static final String CONFIG_FILE = "org.opendaylight.controller.cluster.datastore";
  private static final String CLUSTER_CONFIG_PATH = "config.cluster-config-path";
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
  public boolean canReuseInstance(AbstractRemoteRPCBrokerModule oldModule) {
      return true;
  }

  @Override
  public java.lang.AutoCloseable createInstance() {
    Broker broker = getDomBrokerDependency();

    ServiceReference<ConfigurationAdmin> srvRef =
                    bundleContext.getServiceReference(ConfigurationAdmin.class);
    AkkaConfigurationReader reader = null;
    if(srvRef != null) {
        try {
            ConfigurationAdmin configAdmin = bundleContext.getService(srvRef);
            Configuration conf = configAdmin.getConfiguration(CONFIG_FILE);
            if(conf != null) {
                Dictionary<String, Object> configs = conf.getProperties();

                if ((configs != null) && (!configs.isEmpty())) {
                    String path = configs.get(CLUSTER_CONFIG_PATH).toString();
                    if (path != null) {
                        reader = new DefaultAkkaConfigurationReader(path);
                    }
                }
            }
        } catch (IOException e) {
            // Ignoring exceptions
        } catch(IllegalStateException e) {
            // Ignoring exceptions
        } finally {
            bundleContext.ungetService(srvRef);
        }
    }
    if (reader == null) {
        reader = new DefaultAkkaConfigurationReader();
    }

    RemoteRpcProviderConfig config = new RemoteRpcProviderConfig.Builder(getActorSystemName())
                              .metricCaptureEnabled(getEnableMetricCapture())
                              .mailboxCapacity(getBoundedMailboxCapacity())
                              .withConfigReader(reader)
                              .build();

    return RemoteRpcProviderFactory.createInstance(broker, bundleContext, config);
  }

  public void setBundleContext(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }
}
