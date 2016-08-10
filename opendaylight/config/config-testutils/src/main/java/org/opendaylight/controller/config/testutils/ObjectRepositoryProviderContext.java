/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.yangtools.yang.binding.RpcService;

/**
 * ProviderContext based on an ObjectRegistry.
 *
 * @author Michael Vorburger
 */
public abstract class ObjectRepositoryProviderContext
    extends AbstractObjectRepositoryBasedLookup
    implements ProviderContext {

    @Override
    public <T extends BindingAwareService> T getSALService(Class<T> service) {
        return getObjectRepository().getInstanceOrException(service);
    }

    @Override
    public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
        return getObjectRepository().getInstanceOrException(serviceInterface);
    }

}
