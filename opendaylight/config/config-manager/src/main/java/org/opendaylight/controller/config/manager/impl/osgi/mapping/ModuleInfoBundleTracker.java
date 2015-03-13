/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.osgi.mapping;

import static java.lang.String.format;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.sal.binding.generator.api.ModuleInfoRegistry;
import org.opendaylight.yangtools.yang.binding.YangModelBindingProvider;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks bundles and attempts to retrieve YangModuleInfo, which is then fed into ModuleInfoRegistry
 */
public final class ModuleInfoBundleTracker implements BundleTrackerCustomizer<Collection<ObjectRegistration<YangModuleInfo>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleInfoBundleTracker.class);

    public static final String MODULE_INFO_PROVIDER_PATH_PREFIX = "META-INF/services/";


    private final ModuleInfoRegistry moduleInfoRegistry;

    public ModuleInfoBundleTracker(ModuleInfoRegistry moduleInfoRegistry) {
        this.moduleInfoRegistry = moduleInfoRegistry;
    }

    @Override
    public Collection<ObjectRegistration<YangModuleInfo>> addingBundle(Bundle bundle, BundleEvent event) {
        URL resource = bundle.getEntry(MODULE_INFO_PROVIDER_PATH_PREFIX + YangModelBindingProvider.class.getName());
        LOG.debug("Got addingBundle({}) with YangModelBindingProvider resource {}", bundle, resource);
        if(resource==null) {
            return null;
        }
        List<ObjectRegistration<YangModuleInfo>> registrations = new LinkedList<>();

        try {
            for (String moduleInfoName : Resources.readLines(resource, Charsets.UTF_8)) {
                LOG.trace("Retrieve ModuleInfo({}, {})", moduleInfoName, bundle);
                YangModuleInfo moduleInfo = retrieveModuleInfo(moduleInfoName, bundle);
                registrations.add(moduleInfoRegistry.registerModuleInfo(moduleInfo));
            }
        } catch (IOException e) {
            LOG.error("Error while reading {}", resource, e);
            throw new RuntimeException(e);
        }

        LOG.trace("Got following registrations {}", registrations);
        return registrations;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Collection<ObjectRegistration<YangModuleInfo>> object) {
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Collection<ObjectRegistration<YangModuleInfo>> regs) {
        if(regs == null) {
            return;
        }

        for (ObjectRegistration<YangModuleInfo> reg : regs) {
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
        YangModelBindingProvider instance;
        try {
            Object instanceObj = clazz.newInstance();
            instance = YangModelBindingProvider.class.cast(instanceObj);
        } catch (InstantiationException e) {
            errorMessage = logMessage("Could not instantiate {} in bundle {}, reason {}", moduleInfoClass, bundle, e);
            throw new IllegalStateException(errorMessage, e);
        } catch (IllegalAccessException e) {
            errorMessage = logMessage("Illegal access during instantiation of class {} in bundle {}, reason {}",
                    moduleInfoClass, bundle, e);
            throw new IllegalStateException(errorMessage, e);
        }
        try{
            return instance.getModuleInfo();
        } catch (NoClassDefFoundError e) {


            LOG.error("Error while executing getModuleInfo on {}", instance, e);
            throw e;
        }
    }

    private static Class<?> loadClass(String moduleInfoClass, Bundle bundle) {
        try {
            return bundle.loadClass(moduleInfoClass);
        } catch (ClassNotFoundException e) {
            String errorMessage = logMessage("Could not find class {} in bundle {}, reason {}", moduleInfoClass, bundle, e);
            throw new IllegalStateException(errorMessage);
        }
    }

    public static String logMessage(String slfMessage, Object... params) {
        LOG.info(slfMessage, params);
        String formatMessage = slfMessage.replaceAll("\\{\\}", "%s");
        return format(formatMessage, params);
    }
}
