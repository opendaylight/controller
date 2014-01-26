/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl;

import org.opendaylight.controller.md.sal.common.api.routing.RoutedRegistration;
import org.opendaylight.yangtools.concepts.Path;

public abstract class AbstractRoutedRegistration<C, P extends Path<P>, S> extends AbstractRegistration<S> implements
        RoutedRegistration<C, P, S> {

    public AbstractRoutedRegistration(S instance) {
        super(instance);
    }
}
