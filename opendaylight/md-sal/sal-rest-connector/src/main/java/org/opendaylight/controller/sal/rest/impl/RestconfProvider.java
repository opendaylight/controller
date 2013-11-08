package org.opendaylight.controller.sal.rest.impl;

import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.model.SchemaServiceListener;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.osgi.framework.BundleContext;

public class RestconfProvider extends AbstractProvider {

    private ListenerRegistration<SchemaServiceListener> listenerRegistration;

    @Override
    public void onSessionInitiated(ProviderSession session) {
        DataBrokerService dataService = session.getService(DataBrokerService.class);

        BrokerFacade.getInstance().setContext(session);
        BrokerFacade.getInstance().setDataService(dataService);

        SchemaService schemaService = session.getService(SchemaService.class);
        listenerRegistration = schemaService.registerSchemaServiceListener(ControllerContext.getInstance());
        ControllerContext.getInstance().setSchemas(schemaService.getGlobalContext());
    }

    @Override
    protected void stopImpl(BundleContext context) {
        super.stopImpl(context);
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
