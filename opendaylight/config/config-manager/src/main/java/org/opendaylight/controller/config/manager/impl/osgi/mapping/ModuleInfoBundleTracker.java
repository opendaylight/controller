/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import org.apache.commons.io.IOUtils;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.sal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

/**
 * Tracks bundles and attempts to retrieve YangModuleInfo.
 */
public final class ModuleInfoBundleTracker implements BundleTrackerCustomizer<Collection<Registration<YangModuleInfo>>> {

    private static final Logger logger = LoggerFactory.getLogger(ModuleInfoBundleTracker.class);
    public static final String GET_MODULE_INFO_METHOD = "getModuleInfo";

    public static final String MODULE_INFO_PROVIDER_PATH_PREFIX = "META-INF/services/";

    private ModuleInfoBackedContext moduleInfoLoadingStrategy = ModuleInfoBackedContext.create();

    public GeneratedClassLoadingStrategy getModuleInfoLoadingStrategy() {
        return moduleInfoLoadingStrategy;
    }

    @Override
    public Collection<Registration<YangModuleInfo>> addingBundle(Bundle bundle, BundleEvent event) {
        URL resource = bundle.getEntry(MODULE_INFO_PROVIDER_PATH_PREFIX + YangModelBindingProvider.class.getName());

        if(resource==null) {
            return null;
        }

        List<Registration<YangModuleInfo>> registrations = new LinkedList<>();

        try (InputStream inputStream = resource.openStream()) {
            List<String> lines = IOUtils.readLines(inputStream);
            for (String moduleInfoName : lines) {
                YangModuleInfo moduleInfo = retrieveModuleInfo(moduleInfoName, bundle);
                registrations.add(moduleInfoLoadingStrategy.registerModuleInfo(moduleInfo));
            }


        } catch (Exception e) {
            logger.error("Error while reading {}", resource, e);
            throw new RuntimeException(e);
        }

        return registrations;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Collection<Registration<YangModuleInfo>> object) {
        // NOOP
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Collection<Registration<YangModuleInfo>> regs) {
        if(regs == null) {
            return;
        }

        for (Registration<YangModuleInfo> reg : regs) {
            try {
                reg.close();
            } catch (Exception e) {
                throw new RuntimeException("Unable to unregister YangModuleInfo " + reg.getInstance(), e);
            }
        }
    }

    private static YangModuleInfo retrieveModuleInfo(String moduleInfoClass, Bundle bundle) {
        String errorMessage;
        Class<?> clazz = loadClass(moduleInfoClass, bundle);

        if (YangModelBindingProvider.class.isAssignableFrom(clazz) == false) {
            errorMessage = logMessage("Class {} does not implement {} in bundle {}", clazz, YangModelBindingProvider.class, bundle);
            throw new IllegalStateException(errorMessage);
        }

        try {
            Object instance = clazz.newInstance();
            Object result = clazz.getMethod(GET_MODULE_INFO_METHOD).invoke(instance);

            if (YangModuleInfo.class.isAssignableFrom(result.getClass()) == false) {
                errorMessage = logMessage("Error invoking method not found {} in bundle {}, reason {}",
                        GET_MODULE_INFO_METHOD, bundle, "Not assignable from " + YangModuleInfo.class);
            } else {
                return (YangModuleInfo) result;
            }

        } catch (InstantiationException e) {
            errorMessage = logMessage("Could not instantiate {} in bundle {}, reason {}", moduleInfoClass, bundle, e);
        } catch (IllegalAccessException e) {
            errorMessage = logMessage("Illegal access during instatiation of class {} in bundle {}, reason {}",
                    moduleInfoClass, bundle, e);
        } catch (NoSuchMethodException e) {
            errorMessage = logMessage("Method not found {} in bundle {}, reason {}", GET_MODULE_INFO_METHOD, bundle, e);
        } catch (InvocationTargetException e) {
            errorMessage = logMessage("Error invoking method {} in bundle {}, reason {}", GET_MODULE_INFO_METHOD,
                    bundle, e);
        }

        throw new IllegalStateException(errorMessage);
    }

    private static Class<?> loadClass(String moduleInfoClass, Bundle bundle) {
        try {
            return bundle.loadClass(moduleInfoClass);
        } catch (ClassNotFoundException e) {
            String errorMessage = logMessage("Could not find class {} in bunde {}, reason {}", moduleInfoClass, bundle, e);
            throw new IllegalStateException(errorMessage);
        }
    }

    public static String logMessage(String slfMessage, Object... params) {
        logger.info(slfMessage, params);
        String formatMessage = slfMessage.replaceAll("\\{\\}", "%s");
        return format(formatMessage, params);
    }
}
