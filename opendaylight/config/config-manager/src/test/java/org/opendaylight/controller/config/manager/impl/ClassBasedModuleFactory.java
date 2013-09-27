/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

/**
 * Creates new Config beans by calling {@link Class#newInstance()} on provided config bean class.
 *
 */
public class ClassBasedModuleFactory implements ModuleFactory {
	private final String implementationName;
	private final Class<? extends Module> configBeanClass;

	/**
	 * @param implementationName
	 * @param configBeanClass class that will be instantiated when createModule is called. This class
	 * must implement Module interface and all exported interfaces.
	 */
	public ClassBasedModuleFactory(String implementationName,
	                               Class<? extends Module> configBeanClass) {
		this.implementationName = implementationName;
		this.configBeanClass = configBeanClass;
	}

	@Override
	public String getImplementationName() {
		return implementationName;
	}

	@Override
	public Module createModule(String instanceName, DependencyResolver dependencyResolver,
			DynamicMBeanWithInstance old) throws Exception {
		Preconditions.checkNotNull(dependencyResolver);
		Preconditions.checkNotNull(old);
		Constructor<? extends Module> declaredConstructor;
		try{
			declaredConstructor = configBeanClass.getDeclaredConstructor(DynamicMBeanWithInstance.class);
		}catch(NoSuchMethodException e){
			throw new IllegalStateException("Did not find constructor with parameters (DynamicMBeanWithInstance) in " + configBeanClass, e);
		}
		Preconditions.checkState(declaredConstructor != null);
		return declaredConstructor.newInstance(old);
	}

	@Override
	public Module createModule(String instanceName, DependencyResolver dependencyResolver) {
		try {
			return configBeanClass.newInstance();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

    @Override
    public boolean isModuleImplementingServiceInterface(Class<? extends AbstractServiceInterface> serviceInterface) {
        Class<?>[] classes = configBeanClass.getInterfaces();
        List<Class<?>> ifc = Arrays.asList(classes);
        if(ifc.contains(serviceInterface)){
            return true;
        }
        for(Class<?> c: classes){
            ifc = Arrays.asList(c.getInterfaces());
            if(ifc.contains(serviceInterface)){
                return true;
            }
        }
        return false;
    }
}
