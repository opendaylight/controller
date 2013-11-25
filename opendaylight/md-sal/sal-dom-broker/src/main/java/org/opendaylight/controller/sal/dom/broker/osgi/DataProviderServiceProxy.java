package org.opendaylight.controller.sal.dom.broker.osgi;

import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.osgi.framework.ServiceReference;

public class DataProviderServiceProxy extends AbstractBrokerServiceProxy<DataProviderService> implements
        DataProviderService {

    public DataProviderServiceProxy(ServiceReference<DataProviderService> ref, DataProviderService delegate) {
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

    @Override
    public void addRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        getDelegate().addRefresher(store, refresher);
    }

    @Override
    public void addValidator(DataStoreIdentifier store, DataValidator validator) {
        getDelegate().addValidator(store, validator);
    }

    @Override
    public Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> registerCommitHandler(
            InstanceIdentifier path, DataCommitHandler<InstanceIdentifier, CompositeNode> commitHandler) {
        return addRegistration(getDelegate().registerCommitHandler(path, commitHandler));
    }

    @Override
    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerConfigurationReader(
            InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        return addRegistration(getDelegate().registerConfigurationReader(path, reader));
    }

    @Override
    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerOperationalReader(
            InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        return addRegistration(getDelegate().registerOperationalReader(path, reader));
    }

    @Override
    public void removeRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        getDelegate().removeRefresher(store, refresher);
    }

    @Override
    public void removeValidator(DataStoreIdentifier store, DataValidator validator) {
        getDelegate().removeValidator(store, validator);
    }
    
    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>>> registerCommitHandlerListener(
            RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>> commitHandlerListener) {
        return addRegistration(getDelegate().registerCommitHandlerListener(commitHandlerListener));
    }
}
