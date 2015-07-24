/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.parser.builder.impl.ModuleBuilder;

/**
 * Can be passed instead of ModuleBuilderCapability when building capabilities
 * in NetconfDeviceSimulator when testing various schema resolution related exceptions.
 */
public class FakeModuleBuilderCapability extends ModuleBuilderCapability {

    public FakeModuleBuilderCapability(final ModuleBuilder input, final String inputStream) {
        super(input, inputStream);
    }

    /**
     *
     * @return empty schema source to trigger schema resolution exception.
     */
    @Override
    public Optional<String> getCapabilitySchema() {
        return Optional.absent();
    }
}
