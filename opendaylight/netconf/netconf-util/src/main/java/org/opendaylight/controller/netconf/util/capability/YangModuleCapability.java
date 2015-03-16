/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.capability;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.common.SimpleDateFormatUtil;
import org.opendaylight.yangtools.yang.model.api.Module;

/**
 * Yang model representing capability.
 */
public final class YangModuleCapability extends BasicCapability {

    private final String content;
    private final String revision;
    private final String moduleName;
    private final String moduleNamespace;

    public YangModuleCapability(final Module module, final String moduleContent) {
        super(toCapabilityURI(module));
        this.content = moduleContent;
        this.moduleName = module.getName();
        this.moduleNamespace = module.getNamespace().toString();
        this.revision = SimpleDateFormatUtil.getRevisionFormat().format(module.getRevision());
    }

    @Override
    public Optional<String> getCapabilitySchema() {
        return Optional.of(content);
    }

    private static String toCapabilityURI(final Module module) {
        return String.valueOf(module.getNamespace()) + "?module="
                + module.getName() + "&revision=" + SimpleDateFormatUtil.getRevisionFormat().format(module.getRevision());
    }

    @Override
    public Optional<String> getModuleName() {
        return Optional.of(moduleName);
    }

    @Override
    public Optional<String> getModuleNamespace() {
        return Optional.of(moduleNamespace);
    }

    @Override
    public Optional<String> getRevision() {
        return Optional.of(revision);
    }
}
