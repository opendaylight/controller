/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.mapping.api;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.w3c.dom.Document;

/**
 * Filters wrap each netconf operation, if there is one found. Filters are
 * sorted and applied from the greatest to smallest sorting order.
 */
public interface NetconfOperationFilter extends Comparable<NetconfOperationFilter> {

    Document doFilter(Document message, NetconfOperationRouter operationRouter, NetconfOperationFilterChain filterChain)
            throws NetconfDocumentedException;

    int getSortingOrder();

}
