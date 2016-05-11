/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.annotations.Beta;

/**
 * @author Robert Varga
 */
@Beta
public abstract class ClientLocalHistory implements AutoCloseable {
    @Override
    public abstract void close();

    // FIXME: add client requests related to a particular local history
}
