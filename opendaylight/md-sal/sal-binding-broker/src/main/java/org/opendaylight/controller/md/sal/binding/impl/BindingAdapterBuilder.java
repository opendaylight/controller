/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.spi.AdapterBuilder;

abstract class BindingAdapterBuilder<T extends BindingService>
        extends AdapterBuilder<T, org.opendaylight.mdsal.binding.api.BindingService> {

    interface Factory<T extends BindingService> {

        BindingAdapterBuilder<T> newBuilder();
    }
}
