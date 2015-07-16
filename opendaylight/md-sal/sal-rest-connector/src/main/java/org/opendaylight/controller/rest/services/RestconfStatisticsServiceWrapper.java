/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.services;

import java.math.BigInteger;
import javax.ws.rs.Path;


/**
 * Interface implements all RestconfService interfaces to provide extra statistics for
 * {@link org.opendaylight.controller.config.yang.rest.connector.RestConnectorRuntimeMXBean}
 *
 */
@Path("/")
public interface RestconfStatisticsServiceWrapper extends RestconfServiceData, RestconfServiceBase,
        RestconfServiceOperations {

    BigInteger getConfigGet();

    BigInteger getSuccessGetConfig();

    BigInteger getFailureGetConfig();

    BigInteger getConfigPost();

    BigInteger getSuccessPost();

    BigInteger getFailurePost();

    BigInteger getConfigPut();

    BigInteger getSuccessPut();

    BigInteger getFailurePut();

    BigInteger getConfigDelete();

    BigInteger getSuccessDelete();

    BigInteger getFailureDelete();

    BigInteger getOperationalGet();

    BigInteger getSuccessGetOperational();

    BigInteger getFailureGetOperational();

    BigInteger getRpc();

}
