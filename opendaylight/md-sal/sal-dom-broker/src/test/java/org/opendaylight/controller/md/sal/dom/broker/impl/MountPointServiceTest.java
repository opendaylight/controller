/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService.DOMMountPointBuilder;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class MountPointServiceTest {

    private DOMMountPointService mountService;
    private static final YangInstanceIdentifier PATH = YangInstanceIdentifier.of(QName.create("namespace", "12-12-2012", "top"));

    @Before
    public void setup() {
        mountService = new DOMMountPointServiceImpl();
    }

    @Test
    public void createSimpleMountPoint() {
        Optional<DOMMountPoint> mountNotPresent = mountService.getMountPoint(PATH);
        assertFalse(mountNotPresent.isPresent());
        DOMMountPointBuilder mountBuilder = mountService.createMountPoint(PATH);
        mountBuilder.register();

        Optional<DOMMountPoint> mountPresent = mountService.getMountPoint(PATH);
        assertTrue(mountPresent.isPresent());
    }
}
