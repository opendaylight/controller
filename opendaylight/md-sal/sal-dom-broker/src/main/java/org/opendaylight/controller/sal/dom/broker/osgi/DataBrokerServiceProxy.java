package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class DataBrokerServiceProxy extends AbstractBrokerServiceProxy<DataBrokerService> implements DataBrokerService {

    public DataBrokerServiceProxy(ServiceReference<DataBrokerService> ref, DataBrokerService delegate) {
        super(ref, delegate);
    }

    public ListenerRegistration<DataChangeListener> registerDataChangeListener(InstanceIdentifier path,
            DataChangeListener listener) {
        return addRegistration(getDelegate().registerDataChangeListener(path, listener));
    }

    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        return getDelegate().readConfigurationData(path);
    }

    public CompositeNode readOperationalData(InstanceIdentifier path) {
        return getDelegate().readOperationalData(path);
    }

    public DataModificationTransaction beginTransaction() {
        return getDelegate().beginTransaction();
    }
    
    
}
