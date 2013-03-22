
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * @file   PropertyTest.java
 *
 * @brief  Test for properties
 *
 */

package org.opendaylight.controller.sal.core;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Latency;
import org.opendaylight.controller.sal.core.Property;

public class PropertyTest {
    @Test
    public void testBandWidthStr() {
        Property b;

        b = new Bandwidth(Bandwidth.BWUNK);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[UnKnown]"));

        b = new Bandwidth(100L);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[100bps]"));

        b = new Bandwidth(Bandwidth.BW10Mbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[10Mbps]"));

        b = new Bandwidth(Bandwidth.BW100Mbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[100Mbps]"));

        b = new Bandwidth(Bandwidth.BW100Mbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[100Mbps]"));

        b = new Bandwidth(Bandwidth.BW1Gbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[1Gbps]"));

        b = new Bandwidth(Bandwidth.BW10Gbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[10Gbps]"));

        b = new Bandwidth(Bandwidth.BW40Gbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[40Gbps]"));

        b = new Bandwidth(Bandwidth.BW100Gbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[100Gbps]"));

        b = new Bandwidth(Bandwidth.BW100Gbps + 15L);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[100Gbps]"));

        b = new Bandwidth(Bandwidth.BW1Tbps);
        System.out.println("b = " + b);
        Assert.assertTrue(b.toString().equals("BandWidth[1Tbps]"));
    }

    @Test
    public void testLatencyStr() {
        Property l;

        l = new Latency(Latency.LATENCYUNK);
        System.out.println("l = " + l);
        Assert.assertTrue(l.toString().equals("Latency[UnKnown]"));

        l = new Latency(Latency.LATENCY1ns);
        System.out.println("l = " + l);
        Assert.assertTrue(l.toString().equals("Latency[1nsec]"));

        l = new Latency(Latency.LATENCY1us);
        System.out.println("l = " + l);
        Assert.assertTrue(l.toString().equals("Latency[1usec]"));

        l = new Latency(Latency.LATENCY1ms);
        System.out.println("l = " + l);
        Assert.assertTrue(l.toString().equals("Latency[1msec]"));
    }
}
