
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.util;

import org.openflow.protocol.factory.BasicFactory;
import org.openflow.protocol.factory.OFMessageFactory;

import junit.framework.TestCase;

public class OFTestCase extends TestCase {
    public OFMessageFactory messageFactory;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        messageFactory = new BasicFactory();
    }

    public void test() throws Exception {
    }
}
