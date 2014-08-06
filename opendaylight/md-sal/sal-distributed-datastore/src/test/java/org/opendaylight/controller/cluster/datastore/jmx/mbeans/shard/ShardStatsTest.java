package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.jmx.AbstractMXBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class ShardStatsTest {
  private MBeanServer mbeanServer;
 private  ShardStats  shardStats;
  private ObjectName testMBeanName;

  @Before
  public void setUp() throws Exception {

    shardStats = new ShardStats("shard-1", "DataStore");
    shardStats.registerMBean();
    mbeanServer= shardStats.getMBeanServer();
    String objectName = AbstractMXBean.BASE_JMX_PREFIX + "type="+shardStats.getMBeanType()+",Category="+
        shardStats.getMBeanCategory() + ",name="+
        shardStats.getMBeanName();
    testMBeanName = new ObjectName(objectName);
  }

  @After
  public void tearDown() throws Exception {
    shardStats.unregisterMBean();
  }

  @Test
  public void testGetShardName() throws Exception {

    Object attribute = mbeanServer.getAttribute(testMBeanName,"ShardName");
    Assert.assertEquals(attribute, "shard-1");

  }

  @Test
  public void testGetCommittedTransactionsCount() throws Exception {
    //let us increment some transactions count and then check
    shardStats.incrementCommittedTransactionCount();
    shardStats.incrementCommittedTransactionCount();
    shardStats.incrementCommittedTransactionCount();

    //now let us get from MBeanServer what is the transaction count.
    Object attribute = mbeanServer.getAttribute(testMBeanName,"CommittedTransactionsCount");
    Assert.assertEquals(attribute, 3L);


  }
}