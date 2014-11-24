/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;

/**
 * @deprecated Compatibility wrapper around {@link SerializedDOMDataBroker}.
 */
@Deprecated
public final class DOMDataBrokerImpl extends SerializedDOMDataBroker {
    public DOMDataBrokerImpl(final Map<LogicalDatastoreType, DOMStore> datastores, final ListeningExecutorService executor) {
        super(datastores, executor);
    }
}
