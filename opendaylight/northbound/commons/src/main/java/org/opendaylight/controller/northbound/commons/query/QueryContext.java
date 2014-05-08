/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

/**
 * Query context
 */
public interface QueryContext {

    /**
     * Create a Query
     * @param queryString - query string to parse
     * @param clazz - The class which represents the top level jaxb object
     * @return a query object
     * @throws QueryException if the query cannot be parsed.
     */
    <T> Query<T> createQuery(String queryString, Class<T> clazz)
            throws QueryException;

}
