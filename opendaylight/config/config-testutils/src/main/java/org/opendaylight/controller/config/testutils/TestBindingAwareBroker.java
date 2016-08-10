/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils;

import static org.opendaylight.yangtools.testutils.mockito.MoreAnswers.exception;

import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.osgi.framework.BundleContext;

public abstract class TestBindingAwareBroker implements BindingAwareBroker {

    @Override
    public ProviderContext registerProvider(BindingAwareProvider provider, BundleContext ctx) {
        return registerProvider(provider);
    }

    @Override
    public ProviderContext registerProvider(BindingAwareProvider provider) {
        return Mockito.mock(ProviderContext.class, exception());
    }

}
