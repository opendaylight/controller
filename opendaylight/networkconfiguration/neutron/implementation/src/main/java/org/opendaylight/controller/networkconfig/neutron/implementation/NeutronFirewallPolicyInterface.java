/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.implementation;

import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronFirewallPolicy;
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

public class NeutronFirewallPolicyInterface implements INeutronFirewallPolicyCRUD {
    private static final Logger logger = LoggerFactory.getLogger(NeutronFirewallPolicyInterface.class);

    private ConcurrentMap<String, NeutronFirewallPolicy> firewallPolicyDB  = new ConcurrentHashMap<String, NeutronFirewallPolicy>();

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
    public boolean neutronFirewallPolicyExists(String uuid) {
        return firewallPolicyDB.containsKey(uuid);
    }

    @Override
    public NeutronFirewallPolicy getNeutronFirewallPolicy(String uuid) {
        if (!neutronFirewallPolicyExists(uuid)) {
            logger.debug("No Firewall Rule Have Been Defined");
            return null;
        }
        return firewallPolicyDB.get(uuid);
    }

    @Override
    public List<NeutronFirewallPolicy> getAllNeutronFirewallPolicies() {
        Set<NeutronFirewallPolicy> allFirewallPolicies = new HashSet<NeutronFirewallPolicy>();
        for (Entry<String, NeutronFirewallPolicy> entry : firewallPolicyDB.entrySet()) {
            NeutronFirewallPolicy firewallPolicy = entry.getValue();
            allFirewallPolicies.add(firewallPolicy);
        }
        logger.debug("Exiting getFirewallPolicies, Found {} OpenStackFirewallPolicy", allFirewallPolicies.size());
        List<NeutronFirewallPolicy> ans = new ArrayList<NeutronFirewallPolicy>();
        ans.addAll(allFirewallPolicies);
        return ans;
    }

    @Override
    public boolean addNeutronFirewallPolicy(NeutronFirewallPolicy input) {
        if (neutronFirewallPolicyExists(input.getFirewallPolicyUUID())) {
            return false;
        }
        firewallPolicyDB.putIfAbsent(input.getFirewallPolicyUUID(), input);
        return true;
    }

    @Override
    public boolean removeNeutronFirewallPolicy(String uuid) {
        if (!neutronFirewallPolicyExists(uuid)) {
            return false;
        }
        firewallPolicyDB.remove(uuid);
        return true;
    }

    @Override
    public boolean updateNeutronFirewallPolicy(String uuid, NeutronFirewallPolicy delta) {
        if (!neutronFirewallPolicyExists(uuid)) {
            return false;
        }
        NeutronFirewallPolicy target = firewallPolicyDB.get(uuid);
        return overwrite(target, delta);
    }

    @Override
    public boolean neutronFirewallPolicyInUse(String firewallPolicyUUID) {
        return !neutronFirewallPolicyExists(firewallPolicyUUID);
    }

}