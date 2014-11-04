/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.northbound.commons.types;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a list of string values
 */
@XmlRootElement(name = "list")
@XmlAccessorType(XmlAccessType.NONE)
public class StringList {
    @XmlElement(name = "item")
    private List<String> list;

    public StringList() {
    }

    public StringList(List<String> list) {
        this.list = list;
    }

    public List<String> getList() {
        return list;
    }

}
