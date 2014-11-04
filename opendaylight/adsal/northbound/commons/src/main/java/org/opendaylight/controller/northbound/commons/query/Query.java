/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.util.Collection;
import java.util.List;


/**
 * Represents a parsed query used in filtering of collections.
 */
public interface Query<T> {

    /**
     * Find items in the given collection and return them as a new list. The
     * original collection is not changed.
     *
     * @param collection to search in.
     * @return list of items which match the query.
     * @throws QueryException
     */
    public List<T> find(Collection<T> collection) throws QueryException;

    /**
     * Apply the query on the given collection. Note that this method will modify
     * the given object by removing any items which don't match the query criteria.
     * If the collection is 'singleton' or unmodifiable, invocation will result in
     * an exception.
     *
     * @param collection
     * @return the number matched items
     * @throws QueryException
     */
    public int filter(Collection<T> collection) throws QueryException;

    /**
     * Search the given root for a child collection and them apply the query on.
     * Note that this method will modify the given object by removing any items
     * which don't match the query criteria.
     *
     * @param root - top level object to search in
     * @param childType - the child type which represents the collection.
     * @return the number of matched items
     * @throws QueryException
     */
    public int filter(T root, Class<?> childType) throws QueryException;
}
