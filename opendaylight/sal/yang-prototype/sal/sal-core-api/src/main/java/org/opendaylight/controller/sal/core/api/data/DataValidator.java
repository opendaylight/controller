/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.data;

import java.util.Set;

import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;


/**
 * {@link Provider}-supplied Validator of the data.
 * 
 * <p>
 * The registration could be done by :
 * <ul>
 * <li>returning an instance of implementation in the return value of
 * {@link Provider#getProviderFunctionality()}
 * <li>passing an instance of implementation and {@link DataStoreIdentifier} rpc
 * as arguments to the
 * {@link DataProviderService#addValidator(DataStoreIdentifier, DataValidator)}
 * </ul>
 * 
 **/
public interface DataValidator extends Provider.ProviderFunctionality {

    /**
     * A set of Data Stores supported by implementation.
     * 
     * The set of {@link DataStoreIdentifier}s which identifies target data
     * stores which are supported by this implementation. This set is used, when
     * {@link Provider} is registered to the SAL, to register and expose the
     * validation functionality to affected data stores.
     * 
     * @return Set of Data Store identifiers
     */
    Set<DataStoreIdentifier> getSupportedDataStores();

    /**
     * Performs validation on supplied data.
     * 
     * @param toValidate
     *            Data to validate
     * @return Validation result. The
     *         <code>{@link RpcResult#isSuccessful()} == true</code> if the data
     *         passed validation, otherwise contains list of errors.
     */
    RpcResult<Void> validate(CompositeNode toValidate);

}
