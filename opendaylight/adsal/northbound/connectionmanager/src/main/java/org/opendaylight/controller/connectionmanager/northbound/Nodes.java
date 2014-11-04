package org.opendaylight.controller.connectionmanager.northbound;
/*
 * Copyright (c) 2013 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.sal.core.Node;

@XmlRootElement(name="list")
@XmlAccessorType(XmlAccessType.NONE)

public class Nodes {
    @XmlElement
    Set<Node> node;
    //To satisfy JAXB
    private Nodes() {
    }

    public Nodes(Set<Node> nodes) {
        this.node = nodes;
    }

    public Set<Node> getNode() {
        return node;
    }

    public void setNode(Set<Node> nodes) {
        this.node = nodes;
    }
}
