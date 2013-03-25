/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import java.util.Collection;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerSession;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderSession;
import org.opendaylight.controller.yang.binding.RpcService;


/**
 * 
 * Defines the component of controller and supplies additional metadata. A
 * component of the controller or application supplies a concrete implementation
 * of this interface.
 * 
 * <p>
 * A user-implemented component (application) which facilitates the SAL and SAL
 * services to access infrastructure services and to provide functionality to
 * {@link Consumer}s and other providers.
 * 

 * 
 */
public interface BindingAwareProvider {

    void onSessionInitialized(ConsumerSession session);

    /**
     * Returns a set of provided implementations of YANG modules and their rpcs.
     * 
     * 
     * @return Set of provided implementation of YANG modules and their Rpcs
     */
    Collection<? extends RpcService> getImplementations();

    /**
     * Gets a set of implementations of provider functionality to be registered
     * into system during the provider registration to the SAL.
     * 
     * <p>
     * This method is invoked by {@link Broker#registerProvider(Provider)} to
     * learn the initial provided functionality
     * 
     * @return Set of provider's functionality.
     */
    Collection<? extends ProviderFunctionality> getFunctionality();

    /**
     * Functionality provided by the {@link BindingAwareProvider}
     * 
     * <p>
     * Marker interface used to mark the interfaces describing specific
     * functionality which could be exposed by providers to other components.
     * 

     * 
     */
    public interface ProviderFunctionality {

    }

    void onSessionInitiated(ProviderSession session);

}

