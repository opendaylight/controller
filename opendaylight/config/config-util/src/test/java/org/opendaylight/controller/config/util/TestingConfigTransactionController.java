/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.ConfigTransactionControllerMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

import com.google.common.collect.Sets;

public class TestingConfigTransactionController implements
        ConfigTransactionControllerMXBean {

    private final ObjectName conf1, conf2, conf3;

    public static final String moduleName1 = "moduleA";
    public static final String moduleName2 = "moduleB";
    public static final String instName1 = "instA";
    public static final String instName2 = "instB";

    public TestingConfigTransactionController() throws Exception {
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
    }

    @Override
    public ObjectName createModule(String moduleName, String instanceName)
            throws InstanceAlreadyExistsException {
        return null;
    }

    @Override
    public void destroyModule(ObjectName objectName)
            throws InstanceNotFoundException {
    }

    @Override
    public void abortConfig() {
    }

    @Override
    public void validateConfig() throws ValidationException {
    }

    @Override
    public String getTransactionName() {
        return null;
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
    public ObjectName lookupConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        if (moduleName.equals(InstanceNotFoundException.class.getSimpleName())) {
            throw new InstanceNotFoundException();
        }
        return conf3;
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
}
