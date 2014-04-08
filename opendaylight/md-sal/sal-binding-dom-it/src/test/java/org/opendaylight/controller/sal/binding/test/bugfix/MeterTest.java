/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.bugfix;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.connect.dom.CrudTestUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.Meters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.BandId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.MeterBandHeadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.MeterBandHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.meter.meter.band.headers.MeterBandHeaderBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MeterTest extends AbstractDataServiceTest{

    private final static Logger log = Logger.getLogger(MeterTest.class
            .getName());

    private static final long METER_ID = 14;
    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier
            .builder(Nodes.class)//
            .child(Node.class, NODE_KEY).toInstance();

    private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA);
    private static final MeterKey METER_KEY = new MeterKey(METER_ID, NODE_REF);

    private static final InstanceIdentifier<Meter> METER_INSTANCE_BA = InstanceIdentifier
            .builder(Meters.class)//
            .child(Meter.class, METER_KEY).toInstance();

    /**
     * crud test for meter
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testForMeter() throws InterruptedException, ExecutionException{
        Meter meter = this.createMeter();
        Meter meterUp = this.createMeterUp();

        DataObject createdMeter = CrudTestUtil.doCreateTest(meter,
                this.baDataService, METER_INSTANCE_BA);
        CrudTestUtil.doReadTest(createdMeter, this.baDataService,
                METER_INSTANCE_BA);
        CrudTestUtil.doUpdateTest(meterUp, createdMeter, this.baDataService,
                METER_INSTANCE_BA);
        CrudTestUtil.doRemoveTest(meterUp, this.baDataService,
                METER_INSTANCE_BA);

        log.info("Test CRUD done for : " + meter);

    }

    /**
     * BA create meter
     * 
     * @return
     */
    private Meter createMeter(){
        MeterBandHeadersBuilder headers = new MeterBandHeadersBuilder();
        MeterBandHeaderBuilder header = new MeterBandHeaderBuilder();

        header.setBandId(new BandId((long) 0));
        header.setBandRate((long) 234);

        List<MeterBandHeader> headertList = Collections
                .<MeterBandHeader> singletonList(header.build());
        headers.setMeterBandHeader(headertList);

        return new MeterBuilder().setKey(METER_KEY).setMeterName("foo")
                .setMeterBandHeaders(headers.build()).build();
    }

    private Meter createMeterUp(){
        MeterBandHeadersBuilder headers = new MeterBandHeadersBuilder();
        MeterBandHeaderBuilder header = new MeterBandHeaderBuilder();

        header.setBandId(new BandId((long) 1));
        header.setBandRate((long) 123);

        List<MeterBandHeader> headertList = Collections
                .<MeterBandHeader> singletonList(header.build());
        headers.setMeterBandHeader(headertList);

        return new MeterBuilder().setKey(METER_KEY).setMeterName("foo_up")
                .build();
    }

}
