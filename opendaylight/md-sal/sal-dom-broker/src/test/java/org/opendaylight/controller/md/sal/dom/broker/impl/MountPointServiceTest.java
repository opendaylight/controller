package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService.DOMMountPointBuilder;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.Top;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import com.google.common.base.Optional;

public class MountPointServiceTest {

    private DOMMountPointService mountService;
    private static final InstanceIdentifier PATH = InstanceIdentifier.of(Top.QNAME);

    @Before
    public void setup() {
        mountService = new DOMMountPointServiceImpl();
    }

    @Test
    public void createSimpleMountPoint() {
        Optional<DOMMountPoint> mountNotPresent = mountService.getMountPoint(PATH);
        assertFalse(mountNotPresent.isPresent());
        DOMMountPointBuilder mountBuilder = mountService.createMountPoint(PATH);
        mountBuilder.build();

        Optional<DOMMountPoint> mountPresent = mountService.getMountPoint(PATH);
        assertTrue(mountPresent.isPresent());
    }
}
