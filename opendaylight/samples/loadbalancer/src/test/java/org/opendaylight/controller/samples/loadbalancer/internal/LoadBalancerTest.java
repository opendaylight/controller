/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.samples.loadbalancer.internal;


import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.samples.loadbalancer.ConfigManager;
import org.opendaylight.controller.samples.loadbalancer.entities.Client;
import org.opendaylight.controller.samples.loadbalancer.entities.Pool;
import org.opendaylight.controller.samples.loadbalancer.entities.PoolMember;
import org.opendaylight.controller.samples.loadbalancer.entities.VIP;
import org.opendaylight.controller.samples.loadbalancer.policies.RoundRobinLBPolicy;

import junit.framework.TestCase;

/**
 * 
 * Class to unit test the load balancing policies.
 *
 */
public class LoadBalancerTest extends TestCase {
    @Test
    public void testRoundRobinPolicy() {
        ConfigManager cm = null;
        cm = new ConfigManager();
        Assert.assertFalse(cm== null);
        
        Pool pool = cm.createPool("TestPool","roundrobin");
        VIP vip = cm.createVIP("TestVIP","10.0.0.9","TCP",(short)5550,"TestPool");
        PoolMember host1 = new PoolMember("host1","10.0.0.1","TestPool");
        PoolMember host2 = new PoolMember("host2","10.0.0.2","TestPool");
        PoolMember host3 = new PoolMember("host3","10.0.0.3","TestPool");
        PoolMember host4 = new PoolMember("host4","10.0.0.4","TestPool");
        PoolMember host5 = new PoolMember("host5","10.0.0.5","TestPool");
        PoolMember host6 = new PoolMember("host6","10.0.0.6","TestPool");
        PoolMember host7 = new PoolMember("host7","10.0.0.7","TestPool");
        
        pool.addMember(host1);
        pool.addMember(host2);
        pool.addMember(host3);
        pool.addMember(host4);
        pool.addMember(host5);
        pool.addMember(host6);
        pool.addMember(host7);
        pool.addVIP(vip);
        
        Assert.assertTrue(cm.getAllPoolMembers("TestPool").size() == pool.getAllMembers().size());
        
        RoundRobinLBPolicy rrp = new RoundRobinLBPolicy(cm);
        
        Client c1 = new Client("10.0.0.1","TCP",(short)5000);
        Assert.assertTrue(rrp.getPoolMemberForClient(c1, vip).equals(host1.getIp()));
        
        c1 = new Client("10.0.0.1","TCP",(short)5001);
        Assert.assertTrue(rrp.getPoolMemberForClient(c1, vip).equals(host2.getIp()));
        
        c1 = new Client("10.0.0.1","TCP",(short)5002);
        Assert.assertTrue(rrp.getPoolMemberForClient(c1, vip).equals(host3.getIp()));
        
        c1 = new Client("10.0.0.1","TCP",(short)5003);
        Assert.assertTrue(rrp.getPoolMemberForClient(c1, vip).equals(host4.getIp()));
    }
}