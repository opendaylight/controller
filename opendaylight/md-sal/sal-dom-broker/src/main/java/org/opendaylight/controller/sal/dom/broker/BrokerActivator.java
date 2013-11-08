package org.opendaylight.controller.sal.dom.broker;

import java.util.Hashtable;

import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class BrokerActivator implements BundleActivator {

    BrokerImpl broker;
    private ServiceRegistration<Broker> brokerReg;
    private ServiceRegistration<SchemaService> schemaReg;
    private ServiceRegistration<DataBrokerService> dataReg;
    private ServiceRegistration<DataProviderService> dataProviderReg;
    private SchemaServiceImpl schemaService;
    private DataBrokerImpl dataService;

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, String> emptyProperties = new Hashtable<String, String>();
        broker = new BrokerImpl();
        broker.setBundleContext(context);
        brokerReg = context.registerService(Broker.class, broker, emptyProperties);

        schemaService = new SchemaServiceImpl();
        schemaService.setContext(context);
        schemaService.setParser(new YangParserImpl());
        schemaService.start();
        schemaReg = context.registerService(SchemaService.class, schemaService, new Hashtable<String, String>());
        
        dataService = new DataBrokerImpl();
        dataReg = context.registerService(DataBrokerService.class, dataService, emptyProperties);
        dataProviderReg = context.registerService(DataProviderService.class, dataService, emptyProperties);
        
        
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (brokerReg != null) {
            brokerReg.unregister();
        }
    }
}
