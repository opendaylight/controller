/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.capability;

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.Collections;
import org.opendaylight.controller.netconf.api.Capability;

/**
 * Capability representing a basic, one-line, string based capability
 */
public class BasicCapability implements Capability {

    private final String capability;

    public BasicCapability(final String capability) {
        this.capability = capability;
    }

    @Override
    public String getCapabilityUri() {
        return capability;
    }

    @Override
    public Optional<String> getModuleNamespace() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getModuleName() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getRevision() {
        return Optional.absent();
    }

    @Override
    public Optional<String> getCapabilitySchema() {
        return Optional.absent();
    }

    @Override
    public Collection<String> getLocation() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return capability;
    }
}
