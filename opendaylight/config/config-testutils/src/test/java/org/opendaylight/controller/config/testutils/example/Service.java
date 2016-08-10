/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils.example;

import javax.inject.Inject;

public class Service {

    private final AnotherService anotherDaggerService;

    @Inject
    public Service(AnotherService another) {
        super();
        this.anotherDaggerService = another;
    }

    public String hi() {
        return anotherDaggerService.foo("world");
    }
}
