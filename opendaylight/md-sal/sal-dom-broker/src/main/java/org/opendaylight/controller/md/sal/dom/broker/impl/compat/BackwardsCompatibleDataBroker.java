package org.opendaylight.controller.md.sal.dom.broker.impl.compat;

import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataProviderService;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public class BackwardsCompatibleDataBroker implements DataProviderService, SchemaContextListener {

    DOMDataBroker backingBroker;
    DataNormalizer normalizer;
    private final ListenerRegistry<DataChangeListener> fakeRegistry = ListenerRegistry.create();


    public BackwardsCompatibleDataBroker(final DOMDataBroker newBiDataImpl) {
        backingBroker = newBiDataImpl;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext ctx) {
        normalizer = new DataNormalizer(ctx);
    }

    @Override
    public CompositeNode readConfigurationData(final InstanceIdentifier legacyPath) {
        BackwardsCompatibleTransaction<?> tx = BackwardsCompatibleTransaction.readOnlyTransaction(backingBroker.newReadOnlyTransaction(),normalizer);
        try {
            return tx.readConfigurationData(legacyPath);
        } finally {
            tx.commit();
        }
    }

    @Override
    public CompositeNode readOperationalData(final InstanceIdentifier legacyPath) {
        BackwardsCompatibleTransaction<?> tx = BackwardsCompatibleTransaction.readOnlyTransaction(backingBroker.newReadOnlyTransaction(),normalizer);
        try {
            return tx.readOperationalData(legacyPath);
        } finally {
            tx.commit();
        }
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return BackwardsCompatibleTransaction.readWriteTransaction(backingBroker.newReadWriteTransaction(), normalizer);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(final InstanceIdentifier path,
            final DataChangeListener listener) {
        return fakeRegistry .register(listener);
    }

    @Override
    public Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> registerCommitHandler(
            final InstanceIdentifier path, final DataCommitHandler<InstanceIdentifier, CompositeNode> commitHandler) {
        // FIXME Do real forwarding
        return new AbstractObjectRegistration<DataCommitHandler<InstanceIdentifier,CompositeNode>>(commitHandler) {
            @Override
            protected void removeRegistration() {
                // NOOP
            }
        };
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>> commitHandlerListener) {
        return null;
    }

    // Obsolote functionality

    @Override
    public void addValidator(final DataStoreIdentifier store, final DataValidator validator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeValidator(final DataStoreIdentifier store, final DataValidator validator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerConfigurationReader(
            final InstanceIdentifier path, final DataReader<InstanceIdentifier, CompositeNode> reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerOperationalReader(
            final InstanceIdentifier path, final DataReader<InstanceIdentifier, CompositeNode> reader) {
        throw new UnsupportedOperationException();
    }

}
