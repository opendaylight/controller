package org.opendaylight.controller.sal.rest.doc.jaxrs;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.rest.doc.impl.ApiDocServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiDocProvider implements Provider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ApiDocProvider.class);

    private static final String APIDOC_BASE_PATH = "/apidoc";

    private final BundleContext bundleCtx;
    private HttpService httpService;

    public ApiDocProvider(@Nonnull final BundleContext bundleCtx) {
        this.bundleCtx = bundleCtx;
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        Preconditions.checkState((!session.isClosed()), "Session is closed.");
        final ServiceReference<?> ref = bundleCtx.getServiceReference(HttpService.class.getName());
        httpService = (HttpService) bundleCtx.getService(ref);
        Application app = new ApiDocApplication();
        ResourceConfig rc = new ResourceConfig();
        rc.register(JaxbContextResolver.class);
        rc.register(ApiDocServiceImpl.class);
        try {
            httpService.registerServlet(APIDOC_BASE_PATH, new ServletContainer(rc), null, null);
        } catch (ServletException | NamespaceException e) {
            LOG.error("REST_CONNECTOR BUNDLE: unexpected error, restconf servlet was not registred", e);
            return;
        }

        LOG.info("REST_CONNECTOR BUNDLE: restconf servlet registered");
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return null;
    }

    @Override
    public void close() throws Exception {
        httpService.unregister(APIDOC_BASE_PATH);
    }
}
