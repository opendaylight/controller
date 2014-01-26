/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.sal.binding.api.data.DataProviderService
import org.opendaylight.controller.sal.common.DataStoreIdentifier
import org.opendaylight.yangtools.yang.binding.DataRoot
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener

abstract class DeprecatedDataAPISupport implements DataProviderService {

    @Deprecated
    override commit(DataStoreIdentifier store) {
        throw new UnsupportedOperationException("Deprecated")
    }

    @Deprecated
    override editCandidateData(DataStoreIdentifier store, DataRoot changeSet) {
        throw new UnsupportedOperationException("Deprecated")
    }

    @Deprecated
    override <T extends DataRoot> getCandidateData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated")
    }

    @Deprecated
    override <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated")
    }

    @Deprecated
    override getConfigurationData(InstanceIdentifier<?> data) {
        throw new UnsupportedOperationException("Deprecated")
    }

    @Deprecated
    override <T extends DataRoot> getData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated")
    }

    @Deprecated
    override <T extends DataRoot> T getData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated")
    }

    @Deprecated
    override getData(InstanceIdentifier<? extends DataObject> path) {
        return readOperationalData(path);
    }

    override registerChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
    }

    override unregisterChangeListener(InstanceIdentifier<? extends DataObject> path,
        DataChangeListener changeListener) {
    }

}
