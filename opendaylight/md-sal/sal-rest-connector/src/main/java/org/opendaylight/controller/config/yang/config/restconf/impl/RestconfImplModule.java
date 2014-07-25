package org.opendaylight.controller.config.yang.config.restconf.impl;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.rest.impl.RestconfProvider;
//import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaServiceListener;
import org.osgi.framework.BundleContext;


public class RestconfImplModule extends org.opendaylight.controller.config.yang.config.restconf.impl.AbstractRestconfImplModule {
  private BundleContext bundleContext;
  private Thread webSocketServerThread;
  private ListenerRegistration<SchemaServiceListener> listenerRegistration;

  public RestconfImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
    super(identifier, dependencyResolver);
  }

  public RestconfImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.restconf.impl.RestconfImplModule oldModule, java.lang.AutoCloseable oldInstance) {
    super(identifier, dependencyResolver, oldModule, oldInstance);
  }

  @Override
  public void customValidation() {
    // add custom validation form module attributes here.
  }

  @Override
  public java.lang.AutoCloseable createInstance() {

    RestconfProvider instance = new RestconfProvider();
    try {
      instance.start(getBundleContext());
      String websocketPortStr = getBundleContext().getProperty(WebSocketServer.WEBSOCKET_SERVER_CONFIG_PROPERTY);
      int websocketPort = (websocketPortStr != null && !"".equals(websocketPortStr)) ? Integer
          .parseInt(websocketPortStr) : WebSocketServer.DEFAULT_PORT;
      Thread webSocketServerThread = new Thread(WebSocketServer.createInstance(websocketPort));
      webSocketServerThread.setName("Web socket server");
      webSocketServerThread.start();
      instance.setWebSocketServerThread(webSocketServerThread);
      Broker broker = getDomRegistryDependency();
      if(broker != null) {
        broker.registerProvider(instance, getBundleContext());
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return instance;
  }

  public void setBundleContext(final BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  public BundleContext getBundleContext() {
    return bundleContext;
  }

}



