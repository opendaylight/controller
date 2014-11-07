/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewallRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NeutronFirewallRuleInterface implements INeutronFirewallRuleCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronFirewallRuleInterface.class);

    private ConcurrentMap<String, NeutronFirewallRule> firewallRuleDB = new ConcurrentHashMap<String, NeutronFirewallRule>();

    // this method uses reflection to update an object from it's delta.

    private boolean overwrite(Object target, Object delta) {
        Method[] methods = target.getClass().getMethods();

        for(Method toMethod: methods){
            if(toMethod.getDeclaringClass().equals(target.getClass())
                    && toMethod.getName().startsWith("set")){

                String toName = toMethod.getName();
                String fromName = toName.replace("set", "get");

                try {
                    Method fromMethod = delta.getClass().getMethod(fromName);
                    Object value = fromMethod.invoke(delta, (Object[])null);
                    if(value != null){
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
    public boolean neutronFirewallRuleExists(String uuid) {
        return firewallRuleDB.containsKey(uuid);
    }

    @Override
    public NeutronFirewallRule getNeutronFirewallRule(String uuid) {
        if (!neutronFirewallRuleExists(uuid)) {
            logger.debug("No Firewall Rule Have Been Defined");
            return null;
        }
        return firewallRuleDB.get(uuid);
    }

    @Override
    public List<NeutronFirewallRule> getAllNeutronFirewallRules() {
        Set<NeutronFirewallRule> allFirewallRules = new HashSet<NeutronFirewallRule>();
        for (Entry<String, NeutronFirewallRule> entry : firewallRuleDB.entrySet()) {
            NeutronFirewallRule firewallRule = entry.getValue();
            allFirewallRules.add(firewallRule);
        }
        logger.debug("Exiting getFirewallRules, Found {} OpenStackFirewallRule", allFirewallRules.size());
        List<NeutronFirewallRule> ans = new ArrayList<NeutronFirewallRule>();
        ans.addAll(allFirewallRules);
        return ans;
    }

    @Override
    public boolean addNeutronFirewallRule(NeutronFirewallRule input) {
        if (neutronFirewallRuleExists(input.getFirewallRuleUUID())) {
            return false;
        }
        firewallRuleDB.putIfAbsent(input.getFirewallRuleUUID(), input);
        return true;
    }

    @Override
    public boolean removeNeutronFirewallRule(String uuid) {
        if (!neutronFirewallRuleExists(uuid)) {
            return false;
        }
        firewallRuleDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateNeutronFirewallRule(String uuid, NeutronFirewallRule delta) {
        if (!neutronFirewallRuleExists(uuid)) {
            return false;
        }
        NeutronFirewallRule target = firewallRuleDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronFirewallRuleInUse(String firewallRuleUUID) {
        return !neutronFirewallRuleExists(firewallRuleUUID);
    }

}