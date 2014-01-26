/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl;

import org.opendaylight.yangtools.concepts.Registration;

public abstract class AbstractRegistration<T> implements Registration<T> {


    private final T instance;

    public AbstractRegistration(T instance) {
        super();
        this.instance = instance;
    }

    @Override
    public final T getInstance() {
        return instance;
    }

}
