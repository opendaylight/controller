/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * Implements a Converter that converts RoutedRpcRegistration instances. This is to work around an issue
 * when injecting a RoutedRpcRegistration instance into a bean where Aries is not able to convert the instance
 * returned from the RpcRegistryProvider to the desired generic RoutedRpcRegistration type specified in the
 * bean's setter method. This is because the actual instance class specifies a generic type variable T and,
 * even though it extends RpcService and should match, Aries doesn't handle it correctly.
 *
 * @author Thomas Pantelis
 */
public class RoutedRpcRegistrationConverter implements Converter {
    @Override
    public boolean canConvert(final Object sourceObject, final ReifiedType targetType) {
        return sourceObject instanceof RoutedRpcRegistration
                && RoutedRpcRegistration.class.isAssignableFrom(targetType.getRawClass());
    }

    @Override
    public Object convert(final Object sourceObject, final ReifiedType targetType) {
        return sourceObject;
    }
}
