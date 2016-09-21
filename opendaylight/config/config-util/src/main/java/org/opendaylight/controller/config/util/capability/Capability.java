/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.util.capability;

import com.google.common.base.Optional;
import java.util.Collection;

/**
 * Contains capability URI announced by server hello message and optionally its
 * corresponding yang schema that can be retrieved by get-schema rpc.
 */
// TODO this should be placed into API bundle for netconf
public interface Capability {

    String getCapabilityUri();

    Optional<String> getModuleNamespace();

    Optional<String> getModuleName();

    Optional<String> getRevision();

    Optional<String> getCapabilitySchema();

    Collection<String> getLocation();
}
