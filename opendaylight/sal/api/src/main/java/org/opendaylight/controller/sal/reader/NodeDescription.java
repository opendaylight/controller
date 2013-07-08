
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.reader;

import java.io.Serializable;


/**
 * Represents the network node description information
 */
public class NodeDescription implements Serializable, Cloneable{
    private static final long serialVersionUID = 1L;

    private String manufacturer;
    private String hardware;
    private String software;
    private String serialNumber;
    private String description;

    public NodeDescription() {

    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getHardware() {
        return hardware;
    }

    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "HwDescription[manufacturer=" + manufacturer + ", hardware="
                        + hardware + ", software=" + software + ", serialNumber="
                        + serialNumber + ", description=" + description + "]";
    }
    @Override
    public NodeDescription clone() {
        NodeDescription nd = new NodeDescription();
        nd.setDescription(description);
        nd.setHardware(hardware);
        nd.setManufacturer(manufacturer);
        nd.setSerialNumber(serialNumber);
        nd.setSoftware(software);

        return nd;
    }
}
