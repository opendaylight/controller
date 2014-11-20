/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

public class TestingConfigRegistry implements ConfigRegistryMXBean {

    static final ObjectName conf1, conf2, conf3, run1, run2, run3;
    public static String check;
    public static boolean checkBool;
    private Map<String, ObjectName> map = new HashMap<>();

    public static final String moduleName1 = "moduleA";
    public static final String moduleName2 = "moduleB";
    public static final String instName1 = "instA";
    public static final String instName2 = "instB";
    public static final String refName1 = "refA";
    public static final String refName2 = "refB";
    public static final String serviceQName1 = "qnameA";
    public static final String serviceQName2 = "qnameB";

    static {
        conf1 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=Module," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName1 + "," + ObjectNameUtil.SERVICE_QNAME_KEY
                + "=" + serviceQName1 + "," + ObjectNameUtil.REF_NAME_KEY
                + "=" + refName1);
        conf2 = ObjectNameUtil.createON(ObjectNameUtil.ON_DOMAIN
                + ":type=Module," + ObjectNameUtil.MODULE_FACTORY_NAME_KEY
                + "=" + moduleName1 + "," + ObjectNameUtil.INSTANCE_NAME_KEY
                + "=" + instName1 + "," + ObjectNameUtil.SERVICE_QNAME_KEY
                + "=" + serviceQName2 + "," + ObjectNameUtil.REF_NAME_KEY
                + "=" + refName1);
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

        check = null;
        checkBool = false;

    }

    @Override
    public ObjectName beginConfig() {
        return conf2;
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

    @Override
    public void checkConfigBeanExists(ObjectName objectName) throws InstanceNotFoundException {
        Set<ObjectName> configBeans = Sets.<ObjectName> newHashSet(run1, run2, run3);
        if(configBeans.size()>0){
            checkBool = true;
        }
    }

    @Override
    public ObjectName lookupConfigBeanByServiceInterfaceName(String serviceInterfaceQName, String refName) {
        if (serviceInterfaceQName.equals(serviceQName1) && refName.equals(refName1)) {
            return conf1;
        }
        else{
            return null;
        }
    }

    @Override
    public Map<String, Map<String, ObjectName>> getServiceMapping() {
        return null;
    }

    @Override
    public Map<String, ObjectName> lookupServiceReferencesByServiceInterfaceName(String serviceInterfaceQName) {

        if(serviceInterfaceQName.equals(serviceQName1)){
            map.put("conf1", conf1);
        }
        else if(serviceInterfaceQName.equals(serviceQName2)){
            map.put("conf2", conf2);
        }
        else{
            map.put("conf3", conf3);
        }
        return map;
    }

    @Override
    public Set<String> lookupServiceInterfaceNames(ObjectName objectName) throws InstanceNotFoundException {
        return Sets.<String> newHashSet(serviceQName1, serviceQName2);
    }

    @Override
    public String getServiceInterfaceName(String namespace, String localName) {
        return null;
    }

    @Override
    public Set<String> getAvailableModuleFactoryQNames() {
        return Sets.<String> newHashSet(moduleName1, moduleName2);
    }

    @Override
    public ObjectName getServiceReference(String serviceInterfaceQName, String refName) throws InstanceNotFoundException {
        return conf1;
    }

    @Override
    public void checkServiceReferenceExists(ObjectName objectName) throws InstanceNotFoundException {
        throw new UnsupportedOperationException();
    }
}
