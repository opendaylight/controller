/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.io.FileNotFoundException;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class URITest {

    private static final ControllerContext controllerContext = ControllerContext.getInstance();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void init() throws FileNotFoundException {
        Set<Module> allModules = TestUtils.loadModulesFrom("/full-versions/yangs");
        assertNotNull(allModules);
        SchemaContext schemaContext = TestUtils.loadSchemaContext(allModules);
        controllerContext.setSchemas(schemaContext);
    }

    @Test
    public void testToInstanceIdentifierList() throws FileNotFoundException {
        InstanceIdentifierContext instanceIdentifier = controllerContext
                .buildIdentifier("simple-nodes:userWithoutClass/foo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "userWithoutClass");

        instanceIdentifier = controllerContext.buildIdentifier("simple-nodes:userWithoutClass/foo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "userWithoutClass");

        instanceIdentifier = controllerContext.buildIdentifier("simple-nodes:user/foo/boo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "user");

        instanceIdentifier = controllerContext.buildIdentifier("simple-nodes:user//boo");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "user");

    }

    @Test
    public void testToInstanceIdentifierListWithNullKey() {
        exception.expect(RestconfDocumentedException.class);
        controllerContext.buildIdentifier("simple-nodes:user/null/boo");
    }

    @Test
    public void testToInstanceIdentifierListWithMissingKey() {
        exception.expect(RestconfDocumentedException.class);
        controllerContext.buildIdentifier("simple-nodes:user/foo");
    }

    @Test
    public void testToInstanceIdentifierContainer() throws FileNotFoundException {
        InstanceIdentifierContext instanceIdentifier = controllerContext.buildIdentifier("simple-nodes:users");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "users");
        assertTrue(instanceIdentifier.getSchemaNode() instanceof ContainerSchemaNode);
        assertEquals(2, ((ContainerSchemaNode) instanceIdentifier.getSchemaNode()).getChildNodes().size());
    }

    @Test
    public void testToInstanceIdentifierChoice() throws FileNotFoundException {
        InstanceIdentifierContext instanceIdentifier = controllerContext
                .buildIdentifier("simple-nodes:food/nonalcoholic");
        assertEquals(instanceIdentifier.getSchemaNode().getQName().getLocalName(), "nonalcoholic");
    }

    @Test
    public void testToInstanceIdentifierChoiceException() {
        exception.expect(RestconfDocumentedException.class);
        controllerContext.buildIdentifier("simple-nodes:food/snack");
    }

    @Test
    public void testToInstanceIdentifierCaseException() {
        exception.expect(RestconfDocumentedException.class);
        controllerContext.buildIdentifier("simple-nodes:food/sports-arena");
    }

    @Test
    public void testToInstanceIdentifierChoiceCaseException() {
        exception.expect(RestconfDocumentedException.class);
        controllerContext.buildIdentifier("simple-nodes:food/snack/sports-arena");
    }

    @Test
    public void testToInstanceIdentifierWithoutNode() {
        exception.expect(RestconfDocumentedException.class);
        controllerContext.buildIdentifier("simple-nodes");
    }

    @Test
    public void testMountPointWithExternModul() throws FileNotFoundException {
        initMountService(true);
        InstanceIdentifierContext instanceIdentifier = controllerContext
                .buildIdentifier("simple-nodes:users/yang-ext:mount/test-interface2:class/student/name");
        assertEquals(
                "[(urn:ietf:params:xml:ns:yang:test-interface2?revision=2014-08-01)class, (urn:ietf:params:xml:ns:yang:test-interface2?revision=2014-08-01)student[{(urn:ietf:params:xml:ns:yang:test-interface2?revision=2014-08-01)name=name}]]",
                ImmutableList.copyOf(instanceIdentifier.getInstanceIdentifier().getPathArguments()).toString());
    }

    @Test
    public void testMountPointWithoutExternModul() throws FileNotFoundException {
        initMountService(true);
        InstanceIdentifierContext instanceIdentifier = controllerContext
                .buildIdentifier("simple-nodes:users/yang-ext:mount/");
        assertTrue(Iterables.isEmpty(instanceIdentifier.getInstanceIdentifier().getPathArguments()));
    }

    @Test
    public void testMountPointWithoutMountService() throws FileNotFoundException {
        exception.expect(RestconfDocumentedException.class);

        controllerContext.setMountService(null);
        controllerContext.buildIdentifier("simple-nodes:users/yang-ext:mount/test-interface2:class/student/name");
    }

    @Test
    public void testMountPointWithoutMountPointSchema() {
        initMountService(false);
        exception.expect(RestconfDocumentedException.class);

        controllerContext.buildIdentifier("simple-nodes:users/yang-ext:mount/test-interface2:class");
    }

    public void initMountService(final boolean withSchema) {
        DOMMountPointService mountService = mock(DOMMountPointService.class);
        controllerContext.setMountService(mountService);
        BrokerFacade brokerFacade = mock(BrokerFacade.class);
        RestconfImpl restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);

        Set<Module> modules2 = TestUtils.loadModulesFrom("/test-config-data/yang2");
        SchemaContext schemaContext2 = TestUtils.loadSchemaContext(modules2);
        DOMMountPoint mountInstance = mock(DOMMountPoint.class);
        if (withSchema) {
            when(mountInstance.getSchemaContext()).thenReturn(schemaContext2);
        } else {
            when(mountInstance.getSchemaContext()).thenReturn(null);
        }
        when(mountService.getMountPoint(any(YangInstanceIdentifier.class))).thenReturn(Optional.of(mountInstance));
    }
}
