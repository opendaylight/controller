/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.binding.impl;

import java.net.URL;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContextFactory;
import org.opendaylight.yangtools.restconf.client.api.UnsupportedProtocolException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.BindingIndependentMappingService;
import org.opendaylight.yangtools.yang.model.api.SchemaContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBrokerServiceImpl implements DataBrokerService {

    private static final Logger logger = LoggerFactory.getLogger(DataBrokerServiceImpl.class.toString());
    private final RestconfClientContext restconfClientContext;
    private final RestconfClientContextFactory restconfClientContextFactory = null;

    public DataBrokerServiceImpl(URL baseUrl, BindingIndependentMappingService mappingService, SchemaContextHolder schemaContextHolder) throws UnsupportedProtocolException {
        this.restconfClientContext = restconfClientContextFactory.getRestconfClientContext(baseUrl, mappingService, schemaContextHolder);
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        //TODO implementation using sal-remote
        return null;
    }

    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        //TODO implementation using restconf-client
        return null;

    }

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        //TODO implementation using restconf-client
        return null;
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener listener) {
        //TODO implementation using restconf-client
        return null;
    }
}
