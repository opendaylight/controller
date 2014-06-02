/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

/**
 *
 *
 */
public class QueryContextImpl implements QueryContext {

    @Override
    public <T> Query<T> createQuery(String queryString, Class<T> type) {
        FiqlParser parser = new FiqlParser(
                new java.io.StringReader(queryString));
        try {
            // parse and build FIQL Expression
            Expression expression = parser.START();
            System.out.println("Expression is" + expression);
            // create Query and return;
            Query<T> query = new QueryImpl<T>(type, expression);
            return query;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}