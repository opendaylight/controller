package org.opendaylight.controller.rest;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Config;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Operational;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.RestConnectorRuntimeMXBean;
import org.opendaylight.controller.config.yang.md.sal.rest.connector.Rpcs;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.rest.api.RestConnector;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RestConnectorProvider implements Provider, AutoCloseable, RestConnector, RestConnectorRuntimeMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(RestConnectorProvider.class);

    private static final String REST_CONNECTOR_BASE_PATH = "/restconf";

    private final BundleContext bundleCx;
    private final PortNumber port;

    // private ServiceRegistration<ExampleResource> registration;

    private HttpService httpService;

    /**
     * RestconfProviderImpl constructor
     *
     * @param bundleCx
     * @param port
     */
    public RestConnectorProvider(@Nonnull final BundleContext bundleCx, @Nonnull final PortNumber port) {
        this.bundleCx = Preconditions.checkNotNull(bundleCx);
        this.port = Preconditions.checkNotNull(port);
    }

    @Override
    public void onSessionInitiated(final ProviderSession session) {
        Preconditions.checkState((!session.isClosed()), "Session is closed.");

        final DOMDataBroker domDataBroker = Preconditions.checkNotNull(session.getService(DOMDataBroker.class));
        final SchemaService schemaService = Preconditions.checkNotNull(session.getService(SchemaService.class));
        final DOMRpcService rpcService = Preconditions.checkNotNull(session.getService(DOMRpcService.class));
        final DOMMountPointService moutpointService = Preconditions.checkNotNull(session
                .getService(DOMMountPointService.class));

        final ServiceReference<?> ref = bundleCx.getServiceReference(HttpService.class.getName());
        httpService = (HttpService) bundleCx.getService(ref);

        final ResourceConfig rc = new ResourceConfig();
        rc.register(ExampleResource.class);

        try {
            httpService.registerServlet(REST_CONNECTOR_BASE_PATH, new ServletContainer(rc), null, null);
        } catch (ServletException | NamespaceException e) {
            LOG.error("REST_CONNECTOR BUNDLE: unexpected error, restconf servlet was not registred", e);
            return;
        }

        LOG.info("REST_CONNECTOR BUNDLE: restconf servlet registered");
    }

    @Override
    public Rpcs getRpcs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Config getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Operational getOperational() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws Exception {
        if (httpService != null) {
            httpService.unregister(REST_CONNECTOR_BASE_PATH);
        }
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        // TODO Auto-generated method stub
        return null;
    }
}
