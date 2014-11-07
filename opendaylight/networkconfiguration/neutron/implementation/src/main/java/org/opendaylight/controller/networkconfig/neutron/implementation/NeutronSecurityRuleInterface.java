/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronSecurityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NeutronSecurityRuleInterface implements INeutronSecurityRuleCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronSecurityRuleInterface.class);
    private ConcurrentMap<String, NeutronSecurityRule> securityRuleDB  = new ConcurrentHashMap<String, NeutronSecurityRule>();

    // this method uses reflection to update an object from it's delta.
    private boolean overwrite(Object target, Object delta) {
        Method[] methods = target.getClass().getMethods();

        for (Method toMethod : methods) {
            if (toMethod.getDeclaringClass().equals(target.getClass())
                && toMethod.getName().startsWith("set")) {

                String toName = toMethod.getName();
                String fromName = toName.replace("set", "get");

                try {
                    Method fromMethod = delta.getClass().getMethod(fromName);
                    Object value = fromMethod.invoke(delta, (Object[]) null);
                    if (value != null) {
                        toMethod.invoke(target, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean neutronSecurityRuleExists(String uuid) {
        return securityRuleDB.containsKey(uuid);
    }

    @Override
    public NeutronSecurityRule getNeutronSecurityRule(String uuid) {
        if (!neutronSecurityRuleExists(uuid)) {
            logger.debug("No Security Rules Have Been Defined");
            return null;
        }
        return securityRuleDB.get(uuid);
    }

    @Override
    public List<NeutronSecurityRule> getAllNeutronSecurityRules() {
        Set<NeutronSecurityRule> allSecurityRules = new HashSet<NeutronSecurityRule>();
        for (Entry<String, NeutronSecurityRule> entry : securityRuleDB.entrySet()) {
            NeutronSecurityRule securityRule = entry.getValue();
            allSecurityRules.add(securityRule);
        }
        logger.debug("Exiting getSecurityRule, Found {} OpenStackSecurityRule", allSecurityRules.size());
        List<NeutronSecurityRule> ans = new ArrayList<NeutronSecurityRule>();
        ans.addAll(allSecurityRules);
        return ans;
    }

    @Override
    public boolean addNeutronSecurityRule(NeutronSecurityRule input) {
        if (neutronSecurityRuleExists(input.getSecurityRuleUUID())) {
            return false;
        }
        securityRuleDB.putIfAbsent(input.getSecurityRuleUUID(), input);
        return true;
    }

    @Override
    public boolean removeNeutronSecurityRule(String uuid) {
        if (!neutronSecurityRuleExists(uuid)) {
            return false;
        }
        securityRuleDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateNeutronSecurityRule(String uuid, NeutronSecurityRule delta) {
        if (!neutronSecurityRuleExists(uuid)) {
            return false;
        }
        NeutronSecurityRule target = securityRuleDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronSecurityRuleInUse(String securityRuleUUID) {
        return !neutronSecurityRuleExists(securityRuleUUID);
    }

}