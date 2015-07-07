/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.restconf.api;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.common.OperationFailedException;

/**
 * @author Thomas Pantelis
 */
public interface JSONRestconfService {
    /**
     * Issues a restconf PUT request to the configuration data store.
     *
     * @param uriPath the yang instance identifier path, eg "/opendaylight-inventory:nodes/node/device-id".
     * @param payload the payload data in JSON format.
     * @throws OperationFailedException if the request fails.
     */
    void put(String uriPath, String payload) throws OperationFailedException;

    /**
     * Issues a restconf POST request to the configuration data store.
     *
     * @param uriPath the yang instance identifier path, eg "/opendaylight-inventory:nodes/node/device-id".
     * @param payload the payload data in JSON format.
     * @throws OperationFailedException if the request fails.
     */
    void post(String uriPath, String payload) throws OperationFailedException;

    /**
     * Issues a restconf DELETE request to the configuration data store.
     *
     * @param uriPath the yang instance identifier path, eg "/opendaylight-inventory:nodes/node/device-id".
     * @throws OperationFailedException if the request fails.
     */
    void delete(String uriPath) throws OperationFailedException;

    /**
     * Issues a restconf GET request to the given data store.
     *
     * @param uriPath the yang instance identifier path, eg "/opendaylight-inventory:nodes/node/device-id".
     * @param datastoreType the data store type to read from.
     * @return an Optional containing the data in JSON format if present.
     * @throws OperationFailedException if the request fails.
     */
    Optional<String> get(String uriPath, LogicalDatastoreType datastoreType) throws OperationFailedException;

//
//    String invokeRpc(String uriPath, String input);
}
