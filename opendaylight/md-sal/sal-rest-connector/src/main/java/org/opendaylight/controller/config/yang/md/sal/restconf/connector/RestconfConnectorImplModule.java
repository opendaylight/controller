package org.opendaylight.controller.config.yang.md.sal.restconf.connector;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.rest.impl.RestconfProvider;
import org.opendaylight.controller.sal.streams.websockets.WebSocketServer;
import org.osgi.framework.BundleContext;

public class RestconfConnectorImplModule extends org.opendaylight.controller.config.yang.md.sal.restconf.connector.AbstractRestconfConnectorImplModule {
    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public RestconfConnectorImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RestconfConnectorImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.md.sal.restconf.connector.RestconfConnectorImplModule oldModule, java.lang.AutoCloseable oldInstance) {
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
            Broker broker = getDomBrokerDependency();
            if(broker != null) {
                broker.registerProvider(instance, getBundleContext());
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return instance;
    }

}
