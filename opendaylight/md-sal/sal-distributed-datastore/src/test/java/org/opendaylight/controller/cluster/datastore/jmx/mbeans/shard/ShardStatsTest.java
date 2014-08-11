package org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.AbstractBaseMBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ShardStatsTest {
    private MBeanServer mbeanServer;
    private ShardStats shardStats;
    private ObjectName testMBeanName;

    @Before
    public void setUp() throws Exception {

        shardStats = new ShardStats("shard-1");
        shardStats.registerMBean();
        mbeanServer = shardStats.getMBeanServer();
        String objectName =
            AbstractBaseMBean.BASE_JMX_PREFIX + "type=" + shardStats
                .getMBeanType() + ",Category=" +
                shardStats.getMBeanCategory() + ",name=" +
                shardStats.getMBeanName();
        testMBeanName = new ObjectName(objectName);
    }

    @After
    public void tearDown() throws Exception {
        shardStats.unregisterMBean();
    }

    @Test
    public void testGetShardName() throws Exception {

        Object attribute = mbeanServer.getAttribute(testMBeanName, "ShardName");
        Assert.assertEquals((String) attribute, "shard-1");

    }

    @Test
    public void testGetCommittedTransactionsCount() throws Exception {
        //let us increment some transactions count and then check
        shardStats.incrementCommittedTransactionCount();
        shardStats.incrementCommittedTransactionCount();
        shardStats.incrementCommittedTransactionCount();

        //now let us get from MBeanServer what is the transaction count.
        Object attribute = mbeanServer.getAttribute(testMBeanName,
            "CommittedTransactionsCount");
        Assert.assertEquals((Long) attribute, (Long) 3L);


    }

    @Test
    public void testGetLastCommittedTransactionTime() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Assert.assertEquals(shardStats.getLastCommittedTransactionTime(),
            sdf.format(new Date(0L)));
        long millis = System.currentTimeMillis();
        shardStats.setLastCommittedTransactionTime(new Date(millis));

        //now let us get from MBeanServer what is the transaction count.
        Object attribute = mbeanServer.getAttribute(testMBeanName,
            "LastCommittedTransactionTime");
        Assert.assertEquals((String) attribute, sdf.format(new Date(millis)));
        Assert.assertNotEquals((String) attribute,
            sdf.format(new Date(millis - 1)));

    }

    @Test
    public void testGetFailedTransactionsCount() throws Exception {
        //let us increment some transactions count and then check
        shardStats.incrementFailedTransactionsCount();
        shardStats.incrementFailedTransactionsCount();


        //now let us get from MBeanServer what is the transaction count.
        Object attribute =
            mbeanServer.getAttribute(testMBeanName, "FailedTransactionsCount");
        Assert.assertEquals((Long) attribute, (Long) 2L);



    }
}
