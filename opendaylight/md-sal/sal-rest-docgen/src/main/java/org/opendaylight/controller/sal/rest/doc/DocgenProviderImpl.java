package org.opendaylight.controller.sal.rest.doc;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.controller.sal.rest.doc.api.RestDocgen;
import org.opendaylight.controller.sal.rest.doc.impl.ApiDocGenerator;
import org.opendaylight.controller.sal.rest.doc.impl.ApiDocServiceImpl;
import org.opendaylight.controller.sal.rest.doc.jaxrs.JaxbContextResolver;
import org.opendaylight.controller.sal.rest.doc.mountpoints.MountPointSwagger;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocgenProviderImpl implements Provider, AutoCloseable, RestDocgen {

    private final Logger _logger = LoggerFactory.getLogger(DocgenProviderImpl.class);
    private static final String APIDOC_BASE_PATH = "/apidoc";

    private final BundleContext bundleContext;
    private HttpService httpService;

    private final List<AutoCloseable> toClose = new LinkedList<>();

    public DocgenProviderImpl(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        SchemaService schemaService = session.getService(SchemaService.class);
        ApiDocGenerator.getInstance().setSchemaService(schemaService);

        DOMMountPointService mountService = session
                .getService(DOMMountPointService.class);
        ListenerRegistration<MountProvisionListener> registration = mountService
                .registerProvisionListener(MountPointSwagger.getInstance());
        MountPointSwagger.getInstance().setGlobalSchema(schemaService);
        synchronized (toClose) {
            toClose.add(registration);
        }
        MountPointSwagger.getInstance().setMountService(mountService);

        _logger.debug("Restconf API Explorer started");

//        Preconditions.checkState((!session.isClosed()), "Session is closed.");
        final ServiceReference<?> ref = bundleContext.getServiceReference(HttpService.class.getName());
        httpService = (HttpService) bundleContext.getService(ref);
        ResourceConfig rc = new ResourceConfig();
        rc.setApplicationName("JAXRSApiDoc");
        rc.register(JaxbContextResolver.class);
        rc.register(ApiDocServiceImpl.class);
        try {
            httpService.registerServlet(APIDOC_BASE_PATH, new ServletContainer(rc), null, null);
        } catch (ServletException | NamespaceException e) {
            _logger.error("APIDOC_CONNECTOR BUNDLE: unexpected error, apidoc servlet was not registred", e);
            return;
        }

        _logger.info("APIDOC_CONNECTOR BUNDLE: apidoc servlet registered");
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return null;
    }

    @Override
    public void close() throws Exception {
        synchronized (toClose) {
            for (AutoCloseable close : toClose) {
                close.close();
            }
        }

        httpService.unregister(APIDOC_BASE_PATH);
    }
}
