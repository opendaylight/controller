/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
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
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.osgi.framework.ServiceReference;

@Deprecated
public class DataProviderServiceProxy extends AbstractBrokerServiceProxy<DataProviderService> implements DataProviderService {

    public DataProviderServiceProxy(final ServiceReference<DataProviderService> ref, final DataProviderService delegate) {
        super(ref, delegate);
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(final YangInstanceIdentifier path,
            final DataChangeListener listener) {
        return addRegistration(getDelegate().registerDataChangeListener(path, listener));
    }

    @Override
    public CompositeNode readConfigurationData(final YangInstanceIdentifier path) {
        return getDelegate().readConfigurationData(path);
    }

    @Override
    public CompositeNode readOperationalData(final YangInstanceIdentifier path) {
        return getDelegate().readOperationalData(path);
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return getDelegate().beginTransaction();
    }

    @Override
    public void addRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        getDelegate().addRefresher(store, refresher);
    }

    @Override
    public void addValidator(final DataStoreIdentifier store, final DataValidator validator) {
        getDelegate().addValidator(store, validator);
    }

    @Override
    public Registration registerCommitHandler(
            final YangInstanceIdentifier path, final DataCommitHandler<YangInstanceIdentifier, CompositeNode> commitHandler) {
        return addRegistration(getDelegate().registerCommitHandler(path, commitHandler));
    }

    @Override
    public Registration registerConfigurationReader(
            final YangInstanceIdentifier path, final DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        return addRegistration(getDelegate().registerConfigurationReader(path, reader));
    }

    @Override
    public Registration registerOperationalReader(
            final YangInstanceIdentifier path, final DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        return addRegistration(getDelegate().registerOperationalReader(path, reader));
    }

    @Override
    public void removeRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        getDelegate().removeRefresher(store, refresher);
    }

    @Override
    public void removeValidator(final DataStoreIdentifier store, final DataValidator validator) {
        getDelegate().removeValidator(store, validator);
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<YangInstanceIdentifier, CompositeNode>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<YangInstanceIdentifier, CompositeNode>> commitHandlerListener) {
        return addRegistration(getDelegate().registerCommitHandlerListener(commitHandlerListener));
    }
}
