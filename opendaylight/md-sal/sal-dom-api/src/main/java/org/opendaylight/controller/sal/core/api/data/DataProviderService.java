/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.data;

import org.opendaylight.controller.md.sal.common.api.data.DataProvisionService;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 *
 * @deprecated Replaced by {@link org.opendaylight.controller.md.sal.dom.api.DOMDataBroker}
 *
 */
@Deprecated
public interface DataProviderService extends
    DataBrokerService, //
    DataProvisionService<YangInstanceIdentifier, CompositeNode>
    {

    /**
     * Adds {@link DataValidator} for specified Data Store
     *
     * @param store
     *            Data Store
     * @param validator
     *            Validator
     */
    public void addValidator(DataStoreIdentifier store, DataValidator validator);

    /**
     * Removes {@link DataValidator} from specified Data Store
     *
     * @param store
     * @param validator
     *            Validator
     */
    public void removeValidator(DataStoreIdentifier store,
            DataValidator validator);

    /**
     * Adds {@link DataRefresher} for specified data store
     *
     * @param store
     * @param refresher
     */
    void addRefresher(DataStoreIdentifier store, DataRefresher refresher);

    /**
     * Removes {@link DataRefresher} from specified data store
     *
     * @param store
     * @param refresher
     */
    void removeRefresher(DataStoreIdentifier store, DataRefresher refresher);


    Registration registerConfigurationReader(YangInstanceIdentifier path, DataReader<YangInstanceIdentifier, CompositeNode> reader);

    Registration registerOperationalReader(YangInstanceIdentifier path, DataReader<YangInstanceIdentifier, CompositeNode> reader);

    public interface DataRefresher extends Provider.ProviderFunctionality {

        /**
         * Fired when some component explicitly requested the data refresh.
         *
         * The provider which exposed the {@link DataRefresher} should republish
         * its provided data by editing the data in all affected data stores.
         */
        void refreshData();
    }
}
