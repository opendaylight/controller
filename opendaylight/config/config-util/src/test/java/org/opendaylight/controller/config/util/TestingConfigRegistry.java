/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import java.util.List;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

import com.google.common.collect.Sets;

public class TestingConfigRegistry implements ConfigRegistryMXBean {

    static final ObjectName conf1, conf2, conf3, run1, run2, run3;

    public static final String moduleName1 = "moduleA";
    public static final String moduleName2 = "moduleB";
    public static final String instName1 = "instA";
    public static final String instName2 = "instB";

    static {
        conf1 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=Module," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName1);
        conf2 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=Module," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName1 + "," + ObjectNameUtil.INSTANCE_NAME_KEY
                + "=" + instName1);
        conf3 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=Module," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName2 + "," + ObjectNameUtil.INSTANCE_NAME_KEY
                + "=" + instName2);
        run1 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=RuntimeBean," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName1);
        run2 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=RuntimeBean," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName1 + "," + ObjectNameUtil.INSTANCE_NAME_KEY
                + "=" + instName1);
        run3 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=RuntimeBean," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName2 + "," + ObjectNameUtil.INSTANCE_NAME_KEY
                + "=" + instName2);
    }

    @Override
    public ObjectName beginConfig() {
        return null;
    }

    @Override
    public CommitStatus commitConfig(ObjectName transactonControllerON)
            throws ConflictingVersionException, ValidationException {
        if (transactonControllerON == null) {
            Exception e = new RuntimeException("message");
            throw ValidationException.createForSingleException(
                    new ModuleIdentifier("moduleName", "instanceName"), e);
        }
        return null;
    }

    @Override
    public List<ObjectName> getOpenConfigs() {
        return null;
    }

    @Override
    public boolean isHealthy() {
        return false;
    }

    @Override
    public Set<String> getAvailableModuleNames() {
        return null;
    }

    @Override
    public Set<ObjectName> lookupConfigBeans() {
        return Sets.newHashSet(conf1, conf2, conf3);
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName) {
        if (moduleName.equals(moduleName1)) {
            return Sets.newHashSet(conf1, conf2);
        } else if (moduleName.equals(moduleName2)) {
            return Sets.newHashSet(conf3);
        } else {
            return null;
        }
    }

    @Override
    public Set<ObjectName> lookupConfigBeans(String moduleName,
            String instanceName) {
        if (moduleName.equals(moduleName1) && instanceName.equals(instName1)) {
            return Sets.newHashSet(conf2);
        } else if (moduleName.equals(moduleName2)
                && instanceName.equals(instName2)) {
            return Sets.newHashSet(conf3);
        } else {
            return null;
        }
    }

    @Override
    public ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        if (moduleName.equals(InstanceNotFoundException.class.getSimpleName())) {
            throw new InstanceNotFoundException();
        }
        return conf3;
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans() {
        return Sets.<ObjectName> newHashSet(run1, run2, run3);
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans(String moduleName,
            String instanceName) {
        if (moduleName.equals(moduleName1) && instanceName.equals(instName1)) {
            return Sets.<ObjectName> newHashSet(run2);
        } else if (moduleName.equals(moduleName2)
                && instanceName.equals(instName2)) {
            return Sets.<ObjectName> newHashSet(run3);
        } else {
            return Sets.<ObjectName> newHashSet();
        }
    }

}
