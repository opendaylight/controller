/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Collection;

import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;

/**
 * 
 * Defines the component of controller and supplies additional metadata. A
 * component of the controller or application supplies a concrete implementation
 * of this interface.
 * 
 * A user-implemented component (application) which faciliates the SAL and SAL
 * services to access infrastructure services or providers' functionality.
 * 
 * 
 */
public interface Consumer {

    /**
     * Callback signaling initialization of the consumer session to the SAL.
     * 
     * The consumer MUST use the session for all communication with SAL or
     * retrieving SAL infrastructure services.
     * 
     * This method is invoked by {@link Broker#registerConsumer(Consumer)}
     * 
     * @param session
     *            Unique session between consumer and SAL.
     */
    public void onSessionInitiated(ConsumerSession session);

    /**
     * Get a set of implementations of consumer functionality to be registered
     * into system during the consumer registration to the SAL.
     * 
     * This method is invoked by {@link Broker#registerConsumer(Consumer)}.
     * 
     * @return Set of consumer functionality.
     */
    public Collection<ConsumerFunctionality> getConsumerFunctionality();

    /**
     * The marker interface for the interfaces describing the consumer
     * functionality contracts.
     * 
     * 
     */
    public interface ConsumerFunctionality {

    }
}
