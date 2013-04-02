
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.protocol_plugin.openflow.internal;

import java.util.List;

import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;

import org.opendaylight.controller.sal.reader.NodeDescription;

/**
 * Utility class for converting openflow description statistics
 * into SAL NodeDescription object
 *
 *
 *
 */
public class DescStatisticsConverter {
    NodeDescription hwDesc;
    OFDescriptionStatistics ofDesc;

    public DescStatisticsConverter(List<OFStatistics> statsList) {
        this.hwDesc = null;
        this.ofDesc = (OFDescriptionStatistics) statsList.get(0);
    }

    public NodeDescription getHwDescription() {
        if (hwDesc == null && ofDesc != null) {
            hwDesc = new NodeDescription();
            hwDesc.setManufacturer(ofDesc.getManufacturerDescription());
            hwDesc.setHardware(ofDesc.getHardwareDescription());
            hwDesc.setSoftware(ofDesc.getSoftwareDescription());
            hwDesc.setDescription(ofDesc.getDatapathDescription());
            hwDesc.setSerialNumber(ofDesc.getSerialNumber());
        }
        return hwDesc;
    }

}
