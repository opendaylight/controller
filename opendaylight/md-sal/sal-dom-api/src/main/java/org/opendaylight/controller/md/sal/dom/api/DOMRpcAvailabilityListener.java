/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.Collection;
import java.util.EventListener;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * An {@link EventListener} used to track RPC implementations becoming (un)available
 * to a {@link DOMRpcService}.
 */
public interface DOMRpcAvailabilityListener extends EventListener {
    /**
     * Method invoked whenever an RPC type becomes available.
     *
     * @param types RPC types newly available
     */
    void onRpcAvailable(@Nonnull Collection<SchemaPath> types);

    /**
     * Method invoked whenever an RPC type becomes unavailable.
     *
     * @param types RPC types which became unavailable
     */
    void onRpcUnavailable(@Nonnull Collection<SchemaPath> types);
}
