/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.sal.restconf.impl.test.TestUtils.buildQName;
import static org.opendaylight.controller.sal.restconf.impl.test.TestUtils.loadSchemaContext;

import com.google.common.base.Optional;
import java.io.FileNotFoundException;
import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.controller.sal.restconf.impl.test.TestUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ControllerContextTest {

    /**
     * test of method {@link ControllerContext#getDataNodeContainerFor(YangInstanceIdentifier)
     * getDataNodeContainerFor()}
     */
    @Test
    public void getDataNodeContainerForTest() {
        final ControllerContext controllerContext = ControllerContext.getInstance();
        SchemaContext schemaControllerContextTest = null;
        try {
            schemaControllerContextTest = loadSchemaContext("/controller-context");
        } catch (FileNotFoundException e) {
            fail("Controller context test schema wasn't loaded.");
        }
        controllerContext.setSchemas(schemaControllerContextTest);
        DataNodeContainer dataNodeContainer = controllerContext.getDataNodeContainerFor(prepareInstanceIdentifier(
                "lst", "cont"));
        assertTrue(dataNodeContainer instanceof DataSchemaNode);
        final DataSchemaNode dataSchemaNode = (DataSchemaNode) dataNodeContainer;
        assertEquals("cont", dataSchemaNode.getQName().getLocalName());
        assertEquals("ns:cont:cont", dataSchemaNode.getQName().getNamespace().toString());

        // referring non existing node in direct child
        dataNodeContainer = controllerContext.getDataNodeContainerFor(prepareInstanceIdentifier("lst", "cont1",
                "cont11"));
        assertNull(dataNodeContainer);

        // referring non existing node which is also searched in indirect children
        dataNodeContainer = controllerContext.getDataNodeContainerFor(prepareInstanceIdentifier("lst", "cont11"));
        assertNull(dataNodeContainer);

    }

    private YangInstanceIdentifier prepareInstanceIdentifier(String... nodeNames) {
        final String ns = "ns:cont:cont";
        final String date = "2014-07-24";
        InstanceIdentifierBuilder iiBuilder = YangInstanceIdentifier.builder();
        for (String nodeName : nodeNames) {
            iiBuilder.node(buildQName(nodeName, ns, date));
        }
        return iiBuilder.toInstance();
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if more than one mount point is in URI
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithTwoMountPointTest() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);

        controllerContext.setMountService(mockMountService(mockMountPoint(schemaContext)));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont/yang-ext:mount/test-module:cont/yang-ext:mount");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.OPERATION_NOT_SUPPORTED, ErrorType.APPLICATION,
                    ControllerContext.REST_ONLY_ONE_MOUNT_POINT);
        }
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if mount service isn't present
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithNoMountPointTest() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockMountService(null));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont/yang-ext:mount/test-module:cont/yang-ext:mount");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.UNKNOWN_ELEMENT, ErrorType.PROTOCOL,
                    ControllerContext.REST_MOUNT_POINT_DOES_NOT_EXIST);
        }
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if moduleName behind mount point has incorrect format (NO moduleName:nodeName)
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithIncorrectModuleNameFormatBehindMountPointTest() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockMountService(mockMountPoint(schemaContext)));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont/yang-ext:mount/cont");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.INVALID_VALUE, ErrorType.PROTOCOL,
                    ControllerContext.REST_MODULE_NAME_BEHIND_MOUNT_POINT);
        }
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if moduleName directly behind mount point doesn't exist
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithNoExistingModuleDirectlyNameBehindMountPointTest() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockMountService(mockMountPoint(schemaContext)));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont/yang-ext:mount/wrong-module-name:cont");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.UNKNOWN_ELEMENT, ErrorType.PROTOCOL, null);
        }
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if moduleName in front of mount point doesn't exist
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithNoExistingModuleNameTest() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockMountService(mockMountPoint(schemaContext)));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont/wrong-module-name:cont1");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.UNKNOWN_ELEMENT, ErrorType.PROTOCOL, null);
        }
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if moduleName not directly behind mount point doesn't exist
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithNoExistingModuleNameBehindMountPoint() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockMountService(mockMountPoint(schemaContext)));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont/yang-ext:mount/test-module:cont/wrong-module-name:cont1");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.UNKNOWN_ELEMENT, ErrorType.PROTOCOL, null);
        }
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if moduleName not directly behind mount point doesn't exist
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithNoExistingNodeInUriTest() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockMountService(mockMountPoint(schemaContext)));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont1");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.INVALID_VALUE, ErrorType.PROTOCOL, null);
        }
    }

    /**
     * coverage exceptional statuses of private method collectPathArguments method. It is tested that exception is
     * raised if no container or list node is in URI.
     *
     * In this case leaf is referenced
     *
     * @throws FileNotFoundException
     */
    @Test
    public void toIdentifierMethodWithNoListOrContainerInUriTest() throws FileNotFoundException {
        ControllerContext controllerContext = ControllerContext.getInstance();

        SchemaContext schemaContext = TestUtils.loadSchemaContext("/full-versions/test-module");
        controllerContext.setGlobalSchema(schemaContext);
        controllerContext.setMountService(mockMountService(mockMountPoint(schemaContext)));
        try {
            controllerContext.toInstanceIdentifier("/test-module:cont/cont1/lf11");
        } catch (RestconfDocumentedException e) {
            veryfyException(e, ErrorTag.INVALID_VALUE, ErrorType.PROTOCOL, null);
        }
    }

    private void veryfyException(RestconfDocumentedException e, ErrorTag errorTag, ErrorType errorType, String message) {
        List<RestconfError> errors = e.getErrors();
        assertEquals(1, errors.size());
        assertEquals(errorTag, errors.get(0).getErrorTag());
        assertEquals(errorType, errors.get(0).getErrorType());
        if (message != null) {
            assertEquals(message, errors.get(0).getErrorMessage());
        }
    }

    private DOMMountPoint mockMountPoint(SchemaContext schemaContext) {
        DOMMountPoint mockMountPoint = mock(DOMMountPoint.class);
        when(mockMountPoint.getSchemaContext()).thenReturn(schemaContext);
        return mockMountPoint;
    }

    private DOMMountPointService mockMountService(DOMMountPoint mountPoint) {
        DOMMountPointService mockedMountService = mock(DOMMountPointService.class);
        if (mountPoint != null) {
            when(mockedMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(
                    Optional.of(mountPoint));
        } else {
            when(mockedMountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(
                    Optional.<DOMMountPoint> absent());
        }
        return mockedMountService;

    }

}
