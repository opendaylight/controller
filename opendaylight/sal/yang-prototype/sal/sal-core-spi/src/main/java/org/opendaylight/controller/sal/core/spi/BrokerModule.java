/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi;

import java.util.Set;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.Consumer.ConsumerFunctionality;
import org.opendaylight.controller.sal.core.api.Provider.ProviderFunctionality;

public interface BrokerModule {

    Set<Class<? extends BrokerService>> getProvidedServices();

    Set<Class<? extends ConsumerFunctionality>> getSupportedConsumerFunctionality();

    <T extends BrokerService> T getServiceForSession(Class<T> service,
            ConsumerSession session);

    Set<Class<? extends ProviderFunctionality>> getSupportedProviderFunctionality();
}
