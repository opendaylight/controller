/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mdsal.connector;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.sal.core.api.model.SchemaService;

public class MdsalNetconfOperationServiceFactory implements NetconfOperationServiceFactory, AutoCloseable {

    private final DOMDataBroker dataBroker;
    private final CurrentSchemaContext currentSchemaContext;

    public MdsalNetconfOperationServiceFactory(final SchemaService schemaService, final DOMDataBroker domDataBroker) {
        this.currentSchemaContext = new CurrentSchemaContext(Preconditions.checkNotNull(schemaService));
        this.dataBroker = Preconditions.checkNotNull(domDataBroker);
    }

    @Override
    public MdsalNetconfOperationService createService(final String netconfSessionIdForReporting) {
        return new MdsalNetconfOperationService(currentSchemaContext, netconfSessionIdForReporting, dataBroker);
    }

    @Override
    public void close() throws Exception {
        currentSchemaContext.close();
    }
}
