/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import com.google.common.collect.Lists;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;
import org.osgi.framework.BundleContext;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class AbstractFactoryTemplate extends GeneralClassTemplate {

    private static final List<String> implementedIfcs = Lists
            .newArrayList(ModuleFactory.class.getCanonicalName());

    private final String globallyUniqueName, moduleInstanceType;
    private final List<String> providedServices;

    public AbstractFactoryTemplate(Header header, String packageName,
            String abstractFactoryName, String globallyUniqueName,
            String moduleInstanceType, List<Field> fields,
            List<String> providedServices) {
        super(header, packageName, abstractFactoryName, Collections
                .<String> emptyList(), implementedIfcs, fields, Collections
                .<MethodDefinition> emptyList(), true, false, Collections
                .<Constructor> emptyList());
        this.globallyUniqueName = globallyUniqueName;
        this.moduleInstanceType = moduleInstanceType;
        this.providedServices = providedServices;
    }

    public String getGloballyUniqueName() {
        return globallyUniqueName;
    }

    public String getInstanceType() {
        return AutoCloseable.class.getCanonicalName();
    }

    public String getModuleNameType() {
        return ModuleIdentifier.class.getCanonicalName();
    }

    public String getModuleInstanceType() {
        return moduleInstanceType;
    }

    public String getAbstractServiceInterfaceType() {
        return AbstractServiceInterface.class.getCanonicalName();
    }

    public List<String> getProvidedServices() {
        return providedServices;
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

    public String getBundleContextType() {
        return BundleContext.class.getCanonicalName();
    }

    @Override
    public String getFtlTempleteLocation() {
        return "factory_abs_template.ftl";
    }

}
