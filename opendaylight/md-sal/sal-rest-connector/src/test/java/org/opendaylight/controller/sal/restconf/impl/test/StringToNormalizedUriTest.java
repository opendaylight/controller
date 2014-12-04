/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.TestUtils.compareInstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import java.io.FileNotFoundException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class StringToNormalizedUriTest {

    private static QNameModule TEST_QNAME;
    private static QNameModule MOUNT_QNAME;
    static {
        try {
            TEST_QNAME = QNameModule.create(URI.create("ns:simple"),
                    new SimpleDateFormat("yyyy-MM-dd").parse("2014-12-04"));
            MOUNT_QNAME = QNameModule.create(URI.create("ns:mount"),
                    new SimpleDateFormat("yyyy-MM-dd").parse("2014-12-04"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static ControllerContext controllerContext = ControllerContext.getInstance();
    private static final QName CONT_QNAME = QName.create(TEST_QNAME, "cont");
    private static final QName LST1_QNAME = QName.create(TEST_QNAME, "lst1");
    private static final QName KEY11_QNAME = QName.create(TEST_QNAME, "key11");
    private static final String KEY11_VALUE = "key11Value";
    private static final QName KEY12_QNAME = QName.create(TEST_QNAME, "key12");
    private static final String KEY12_VALUE = "key12Value";
    private static final QName CONT12_QNAME = QName.create(TEST_QNAME, "cont12");
    private static final QName CH11_QNAME = QName.create(TEST_QNAME, "ch11");
    private static final QName CH12_QNAME = QName.create(TEST_QNAME, "ch12");
    private static final QName CONT11A1_QNAME = QName.create(TEST_QNAME, "cont11A1");
    private static final QName CONT11D1A1_QNAME = QName.create(TEST_QNAME, "cont11D1A1");
    private static final QName CONT12A1_QNAME = QName.create(TEST_QNAME, "cont12A1");
    private static final QName CONT11C1_QNAME = QName.create(TEST_QNAME, "cont11C1");
    private static final QName CONT11A2_QNAME = QName.create(TEST_QNAME, "cont11A2");
    private static final QName CH11D1_QNAME = QName.create(TEST_QNAME, "ch11D1");

    private static final QName CONT_MOUNT_QNAME = QName.create(MOUNT_QNAME, "cont-mount");
    private static final QName LST1_MOUNT_QNAME = QName.create(MOUNT_QNAME, "lst1-mount");
    private static final QName KEY11_MOUNT_QNAME = QName.create(MOUNT_QNAME, "key11-mount");
    private static final String KEY11_MOUNT_VALUE = "key11-mount-value";
    private static final Object CH11_MOUNT_QNAME = QName.create(MOUNT_QNAME, "ch11-mount");
    private static final Object CH11A1_MOUNT_QNAME = QName.create(MOUNT_QNAME, "ch11A1-mount");;
    private static final QName CONT11A1A1_MOUNT_QNAME = QName.create(MOUNT_QNAME, "cont11A1A1-mount");

    private static SchemaContext schemaContext = null;
    private static SchemaContext schemaContextBehindMount = null;

    @BeforeClass
    public static void initialize() throws FileNotFoundException {
        schemaContextBehindMount = TestUtils.loadSchemaContext("/nested-choice/mountpoint");
        DOMMountPoint mockedMountPoint = mock(DOMMountPoint.class);
        when(mockedMountPoint.getSchemaContext()).thenReturn(schemaContextBehindMount);

        DOMMountPointService mockedMountService = mock(DOMMountPointService.class);
        when(mockedMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(
                Optional.fromNullable(mockedMountPoint));

        schemaContext = TestUtils.loadSchemaContext("/nested-choice/yang");
        assertNotNull(schemaContext);
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockedMountService);
    }

    /**
     * Awaited path arguments (specified in compareInstanceIdentifier) were specified according to output of deprecated
     * method DataNormalizer.toNormalized()
     */
    @Test
    public void testInstanceIdentifier() {
        // without mount point
        compareInstanceIdentifier("/simple:cont/lst1/key11Value/key12Value/cont12", schemaContext, CONT_QNAME,
                LST1_QNAME, new Object[] { LST1_QNAME, KEY11_QNAME, KEY11_VALUE, KEY12_QNAME, KEY12_VALUE },
                CONT12_QNAME);
        compareInstanceIdentifier("/simple:cont/lst1/key11Value/key12Value/cont11A1", schemaContext, CONT_QNAME,
                LST1_QNAME, new Object[] { LST1_QNAME, KEY11_QNAME, KEY11_VALUE, KEY12_QNAME, KEY12_VALUE },
                CH11_QNAME, CONT11A1_QNAME);
        compareInstanceIdentifier("/simple:cont/lst1/key11Value/key12Value/cont11D1A1", schemaContext, CONT_QNAME,
                LST1_QNAME, new Object[] { LST1_QNAME, KEY11_QNAME, KEY11_VALUE, KEY12_QNAME, KEY12_VALUE },
                CH11_QNAME, CH11D1_QNAME, CONT11D1A1_QNAME);
        compareInstanceIdentifier("/simple:cont/lst1/key11Value/key12Value/cont12A1", schemaContext, CONT_QNAME,
                LST1_QNAME, new Object[] { LST1_QNAME, KEY11_QNAME, KEY11_VALUE, KEY12_QNAME, KEY12_VALUE },
                Sets.newHashSet(CH12_QNAME), CH12_QNAME, CONT12A1_QNAME);
        compareInstanceIdentifier("/simple:cont/lst1/key11Value/key12Value/cont11C1", schemaContext, CONT_QNAME,
                LST1_QNAME, new Object[] { LST1_QNAME, KEY11_QNAME, KEY11_VALUE, KEY12_QNAME, KEY12_VALUE },
                CH11_QNAME, CONT11C1_QNAME);
        compareInstanceIdentifier("/simple:cont/lst1/key11Value/key12Value/cont11A2", schemaContext, CONT_QNAME,
                LST1_QNAME, new Object[] { LST1_QNAME, KEY11_QNAME, KEY11_VALUE, KEY12_QNAME, KEY12_VALUE },
                CH11_QNAME, CONT11A2_QNAME);

        // with mount point
        compareInstanceIdentifier("/simple:cont/lst1/key1Value/key2Value/cont12/" + ControllerContext.MOUNT
                + "/mount:cont-mount/lst1-mount/key11-mount-value/cont11A1A1-mount", schemaContextBehindMount,
                CONT_MOUNT_QNAME, LST1_MOUNT_QNAME, new Object[] { LST1_MOUNT_QNAME, KEY11_MOUNT_QNAME,
                        KEY11_MOUNT_VALUE }, CH11_MOUNT_QNAME, CH11A1_MOUNT_QNAME, CONT11A1A1_MOUNT_QNAME);
    }

}
