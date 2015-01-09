/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.parser.builder.impl.ModuleBuilder;

final class ModuleBuilderCapability implements Capability {
    private static final Date NO_REVISION = new Date(0);
    private final ModuleBuilder input;
    private final Optional<String> content;

    public ModuleBuilderCapability(final ModuleBuilder input, final String inputStream) {
        this.input = input;
        this.content = Optional.of(inputStream);
    }

    @Override
    public String getCapabilityUri() {
        // FIXME capabilities in Netconf-impl need to check for NO REVISION
        final String withoutRevision = getModuleNamespace().get() + "?module=" + getModuleName().get();
        return hasRevision() ? withoutRevision + "&revision=" + Util.writeDate(input.getRevision()) : withoutRevision;
    }

    @Override
    public Optional<String> getModuleNamespace() {
        return Optional.of(input.getNamespace().toString());
    }

    @Override
    public Optional<String> getModuleName() {
        return Optional.of(input.getName());
    }

    @Override
    public Optional<String> getRevision() {
        return Optional.of(hasRevision() ? QName.formattedRevision(input.getRevision()) : "");
    }

    private boolean hasRevision() {
        return !input.getRevision().equals(NO_REVISION);
    }

    @Override
    public Optional<String> getCapabilitySchema() {
        return content;
    }

    @Override
    public List<String> getLocation() {
        return Collections.emptyList();
    }
}
