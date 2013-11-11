package org.opendaylight.controller.sal.dom.broker;

import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataBroker;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.controller.sal.dom.broker.impl.DataReaderRouter;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class DataBrokerImpl extends AbstractDataBroker<InstanceIdentifier, CompositeNode, DataChangeListener> implements
        DataProviderService {

    public DataBrokerImpl() {
        setDataReadRouter(new DataReaderRouter());
    }

    @Override
    public DataTransactionImpl beginTransaction() {
        return new DataTransactionImpl(this);
    }

    @Override
    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerConfigurationReader(
            InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        return getDataReadRouter().registerConfigurationReader(path, reader);
    }

    @Override
    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerOperationalReader(
            InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        return getDataReadRouter().registerOperationalReader(path, reader);
    }

    @Deprecated
    @Override
    public void addValidator(DataStoreIdentifier store, DataValidator validator) {
        // TODO Auto-generated method stub

    }

    @Deprecated
    @Override
    public void removeValidator(DataStoreIdentifier store, DataValidator validator) {
        // TODO Auto-generated method stub

    }

    @Deprecated
    @Override
    public void addRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        // TODO Auto-generated method stub

    }

    @Deprecated
    @Override
    public void removeRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        // TODO Auto-generated method stub

    }

}