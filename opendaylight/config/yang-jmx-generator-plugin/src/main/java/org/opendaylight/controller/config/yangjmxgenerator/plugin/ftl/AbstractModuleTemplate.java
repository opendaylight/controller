/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.ModuleField;

public class AbstractModuleTemplate extends GeneralClassTemplate {

    private final List<ModuleField> moduleFields;
    private final boolean runtime;
    private final String registratorType;

    public AbstractModuleTemplate(Header header, String packageName,
            String abstractModuleName, List<String> extendedClasses,
            List<String> implementedIfcs, List<ModuleField> moduleFields, List<MethodDefinition> methods,
            boolean isRuntime, String registratorType) {
        super(header, packageName, abstractModuleName, extendedClasses,
                implementedIfcs, Collections.<Field> emptyList(), methods,
                true, false, Collections.<Constructor> emptyList());
        this.moduleFields = moduleFields;
        this.runtime = isRuntime;
        this.registratorType = registratorType;
    }

    public List<ModuleField> getModuleFields() {
        return moduleFields;
    }

    public String getRegistratorType() {
        return registratorType;
    }

    public boolean isRuntime() {
        return runtime;
    }

}
