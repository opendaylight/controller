/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.statistics.northbound;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author adityavaja
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class AllTableStatistics {
    @XmlElement
    List<TableStatistics> tableStatistics;
    //To satisfy JAXB
    private AllTableStatistics() {
    }

    public AllTableStatistics(List<TableStatistics> tableStatistics) {
        this.tableStatistics = tableStatistics;
    }

    public List<TableStatistics> getTableStatistics() {
        return tableStatistics;
    }

    public void setTableStatistics(List<TableStatistics> tableStatistics) {
        this.tableStatistics = tableStatistics;
    }

}
