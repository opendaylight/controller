/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.networkconfig.neutron.northbound;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;

import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class is an instance of javax.ws.rs.core.Application and is used to return the classes
 * that will be instantiated for JAXRS processing. This is necessary
 * because package scanning in jersey doesn't yet work in OSGi environment.
 *
 */
public class NeutronNorthboundRSApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
// northbound URIs
        classes.add(NeutronNetworksNorthbound.class);
        classes.add(NeutronSubnetsNorthbound.class);
        classes.add(NeutronPortsNorthbound.class);
        classes.add(NeutronRoutersNorthbound.class);
        classes.add(NeutronFloatingIPsNorthbound.class);
        classes.add(NeutronSecurityGroupsNorthbound.class);
        classes.add(NeutronSecurityRulesNorthbound.class);
        classes.add(NeutronFirewallNorthbound.class);
        classes.add(NeutronFirewallPolicyNorthbound.class);
        classes.add(NeutronFirewallRulesNorthbound.class);
        classes.add(NeutronLoadBalancerNorthbound.class);
        classes.add(NeutronLoadBalancerListenerNorthbound.class);
        classes.add(NeutronLoadBalancerPoolNorthbound.class);
        classes.add(NeutronLoadBalancerHealthMonitorNorthbound.class);
        classes.add(NeutronLoadBalancerPoolMembersNorthbound.class);
      classes.add(MOXyJsonProvider.class);
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        MOXyJsonProvider moxyJsonProvider = new MOXyJsonProvider();

        moxyJsonProvider.setAttributePrefix("@");
        moxyJsonProvider.setFormattedOutput(true);
        moxyJsonProvider.setIncludeRoot(false);
        moxyJsonProvider.setMarshalEmptyCollections(true);
        moxyJsonProvider.setValueWrapper("$");

        Map<String, String> namespacePrefixMapper = new HashMap<String, String>(3);
        namespacePrefixMapper.put("router", "router");        // FIXME: fill in with XSD
        namespacePrefixMapper.put("provider", "provider");    // FIXME: fill in with XSD
        namespacePrefixMapper.put("binding", "binding");
        moxyJsonProvider.setNamespacePrefixMapper(namespacePrefixMapper);
        moxyJsonProvider.setNamespaceSeparator(':');

        HashSet<Object> set = new HashSet<Object>(1);
        set.add(moxyJsonProvider);
        return set;
    }
}
