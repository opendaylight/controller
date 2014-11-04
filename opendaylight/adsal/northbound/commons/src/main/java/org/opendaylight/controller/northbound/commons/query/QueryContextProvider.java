/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * A provider for getting hold of the QueryContext.
 */
@Provider
public class QueryContextProvider implements ContextResolver<QueryContext> {

    // Singleton Query Context instance
    private static final QueryContext queryContext = new QueryContextImpl();

    @Override
    public QueryContext getContext(Class<?> type) {
        return queryContext;
    }

}
