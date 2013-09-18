/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService
import org.opendaylight.controller.sal.common.DataStoreIdentifier
import org.opendaylight.yangtools.yang.binding.DataRoot
import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.binding.api.data.DataCommitHandler
import org.opendaylight.controller.sal.binding.api.data.DataRefresher
import org.opendaylight.controller.sal.binding.api.data.DataValidator
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.controller.sal.binding.api.data.RuntimeDataProvider
import java.util.Map

class _DataBrokerImpl implements DataProviderService {

    Map<DataStoreIdentifier, DataProviderContext> dataProviders;
    var DataProviderContext defaultDataProvider;

    override <T extends DataRoot> getData(DataStoreIdentifier store, Class<T> rootType) {
        val dataStore = resolveProvider(store, rootType);
        return dataStore.provider.getData(store, rootType);
    }

    override <T extends DataRoot> getData(DataStoreIdentifier store, T filter) {
    }

    override <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    override <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    override commit(DataStoreIdentifier store) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override editCandidateData(DataStoreIdentifier store, DataRoot changeSet) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override addCommitHandler(DataStoreIdentifier store, DataCommitHandler provider) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override addRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override addValidator(DataStoreIdentifier store, DataValidator validator) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeRefresher(DataStoreIdentifier store, DataRefresher refresher) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    override removeCommitHandler(DataStoreIdentifier store, DataCommitHandler provider) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")

    }

    override removeValidator(DataStoreIdentifier store, DataValidator validator) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub")
    }

    def DataProviderContext resolveProvider(DataStoreIdentifier store, Class<? extends DataRoot> root) {
    }

}
