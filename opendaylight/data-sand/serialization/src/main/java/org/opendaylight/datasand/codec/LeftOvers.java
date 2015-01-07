/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.codec;

import java.util.LinkedList;
import java.util.List;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class LeftOvers {
    private List<Object> leftOverList = new LinkedList<Object>();

    public LeftOvers() {
    }

    public void addLeftOver(Object o) {
        this.leftOverList.add(o);
    }

    public List<Object> getLeftOvers() {
        return this.leftOverList;
    }
}
