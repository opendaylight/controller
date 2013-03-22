/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Collection;

import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;

/**
 * 
 * Defines the component of controller and supplies additional metadata. A
 * component of the controller or application supplies a concrete implementation
 * of this interface.
 * 
 * <p>
 * A user-implemented component (application) which faciliates the SAL and SAL
 * services to access infrastructure services and to provide functionality to
 * {@link Consumer}s and other providers.
 * 
 * 
 */
public interface Provider {

    /**
     * Callback signaling initialization of the provider session to the SAL.
     * 
     * <p>
     * The provider <b>MUST use the session</b> for all communication with SAL
     * or retrieving SAL infrastructure services.
     * 
     * <p>
     * This method is invoked by {@link Broker#registerConsumer(Consumer)}
     * 
     * @param session
     *            Unique session between provider and SAL.
     */
    public void onSessionInitiated(ProviderSession session);

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
    public Collection<ProviderFunctionality> getProviderFunctionality();

    /**
     * Functionality provided by the {@link Provider}
     * 
     * <p>
     * Marker interface used to mark the interfaces describing specific
     * functionality which could be exposed by providers to other components.
     * 

     * 
     */
    public interface ProviderFunctionality {

    }
}
