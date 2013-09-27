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

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.ModuleField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class AbstractModuleTemplate extends GeneralClassTemplate {

    private final List<ModuleField> moduleFields;
    private final boolean runtime;
    private final String registratorType;

    public AbstractModuleTemplate(Header header, String packageName,
            String abstractModuleName, List<String> implementedIfcs,
            List<ModuleField> moduleFields, List<MethodDefinition> methods,
            boolean isRuntime, String registratorType) {
        super(header, packageName, abstractModuleName, Collections
                .<String> emptyList(), implementedIfcs, Collections
                .<Field> emptyList(), methods, true, false, Collections
                .<Constructor> emptyList());
        this.moduleFields = moduleFields;
        this.runtime = isRuntime;
        this.registratorType = registratorType;
    }

    public List<ModuleField> getModuleFields() {
        return moduleFields;
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

    public String getRegistratorType() {
        return registratorType;
    }

    public boolean isRuntime() {
        return runtime;
    }

    public String getDependencyResolverType() {
        return DependencyResolver.class.getCanonicalName();
    }

    public String getDynamicMBeanWithInstanceType() {
        return DynamicMBeanWithInstance.class.getCanonicalName();
    }

    public String getRootRuntimeRegistratorType() {
        return RootRuntimeBeanRegistrator.class.getCanonicalName();
    }

    @Override
    public String getFtlTempleteLocation() {
        return "module_abs_template_new.ftl";
    }

    public String getLoggerType() {
        return Logger.class.getCanonicalName();
    }

    public String getLoggerFactoryType() {
        return LoggerFactory.class.getCanonicalName();
    }

}
