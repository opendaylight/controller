/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.util.Collections;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;

import com.google.common.collect.Lists;

/**
 *
 */
public class StubModuleTemplate extends GeneralClassTemplate {

    private final String extendedClass;

    public StubModuleTemplate(Header header, String packageName,
            String stubModuleName, String extendedClass) {
        super(header, packageName, stubModuleName, Lists
                .newArrayList(extendedClass), Collections.<String> emptyList(),
                Collections.<Field> emptyList(), Collections
                        .<MethodDefinition> emptyList(), false, true,
                Collections.<Constructor> emptyList());
        this.extendedClass = extendedClass;
    }

    public String getExtendedClass() {
        return extendedClass;
    }

    public String getInstanceType() {
        return AutoCloseable.class.getCanonicalName();
    }

    public String getModuleNameType() {
        return ModuleIdentifier.class.getCanonicalName();
    }

    public String getAbstractServiceInterfaceType() {
        return AbstractServiceInterface.class.getCanonicalName();
    }

    public String getModuleType() {
        return Module.class.getCanonicalName();
    }

    public String getDependencyResolverType() {
        return DependencyResolver.class.getCanonicalName();
    }

    public String getDynamicMBeanWithInstanceType() {
        return DynamicMBeanWithInstance.class.getCanonicalName();
    }

    @Override
    public String getFtlTempleteLocation() {
        return "module_stub_template.ftl";
    }
}
