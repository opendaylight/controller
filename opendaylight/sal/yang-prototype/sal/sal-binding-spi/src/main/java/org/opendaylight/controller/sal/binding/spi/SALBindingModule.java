/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.spi;

import java.util.Set;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerSession;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider.ProviderFunctionality;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;

public interface SALBindingModule {

    void setBroker(BindingAwareBroker broker);
    void onBISessionAvailable(ProviderSession session);
    
    void setMappingProvider(MappingProvider provider);

    Set<Class<? extends BindingAwareService>> getProvidedServices();

    <T extends BindingAwareService> T getServiceForSession(Class<T> service,
            ConsumerSession session);

    Set<Class<? extends ProviderFunctionality>> getSupportedProviderFunctionality();
}
