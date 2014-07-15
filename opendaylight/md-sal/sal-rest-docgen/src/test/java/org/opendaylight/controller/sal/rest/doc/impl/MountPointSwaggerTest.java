/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.ws.rs.core.UriInfo;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionService;
import org.opendaylight.controller.sal.rest.doc.mountpoints.MountPointSwagger;
import org.opendaylight.controller.sal.rest.doc.swagger.Api;
import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;
import org.opendaylight.controller.sal.rest.doc.swagger.Operation;
import org.opendaylight.controller.sal.rest.doc.swagger.Resource;
import org.opendaylight.controller.sal.rest.doc.swagger.ResourceList;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

public class MountPointSwaggerTest {

    private static final String HTTP_URL = "http://localhost/path";
    private static final YangInstanceIdentifier instanceId = YangInstanceIdentifier.builder()
            .node(QName.create("nodes"))
            .nodeWithKey(QName.create("node"), QName.create("id"), "123").build();
    private static final String INSTANCE_URL = "nodes/node/123/";
    private MountPointSwagger swagger;
    private DocGenTestHelper helper;
    private SchemaContext schemaContext;

    @Before
    public void setUp() throws Exception {
        swagger = new MountPointSwagger();
        helper = new DocGenTestHelper();
        helper.setUp();
        schemaContext = new YangParserImpl().resolveSchemaContext(new HashSet<Module>(helper.getModules().values()));
    }

    @Test()
    public void testGetResourceListBadIid() throws Exception {
        UriInfo mockInfo = helper.createMockUriInfo(HTTP_URL);

        assertEquals(null, swagger.getResourceList(mockInfo, 1L));
    }

    @Test()
    public void getInstanceIdentifiers() throws Exception {
        UriInfo mockInfo = setUpSwaggerForDocGeneration();

        assertEquals(0, swagger.getInstanceIdentifiers().size());
        swagger.onMountPointCreated(instanceId); // add this ID into the list of
                                                 // mount points
        assertEquals(1, swagger.getInstanceIdentifiers().size());
        assertEquals((Long) 1L, swagger.getInstanceIdentifiers().entrySet().iterator().next()
                .getValue());
        assertEquals(INSTANCE_URL, swagger.getInstanceIdentifiers().entrySet().iterator().next()
                .getKey());
        swagger.onMountPointRemoved(instanceId); // remove ID from list of mount
                                                 // points
        assertEquals(0, swagger.getInstanceIdentifiers().size());
    }

    @Test
    public void testGetResourceListGoodId() throws Exception {
        UriInfo mockInfo = setUpSwaggerForDocGeneration();
        swagger.onMountPointCreated(instanceId); // add this ID into the list of
                                                 // mount points
        ResourceList resourceList = swagger.getResourceList(mockInfo, 1L);

        Resource dataStoreResource = null;
        for (Resource r : resourceList.getApis()) {
            if (r.getPath().endsWith("/Datastores(-)")) {
                dataStoreResource = r;
            }
        }
        assertNotNull("Failed to find data store resource", dataStoreResource);
    }

    @Test
    public void testGetDataStoreApi() throws Exception {
        UriInfo mockInfo = setUpSwaggerForDocGeneration();
        swagger.onMountPointCreated(instanceId); // add this ID into the list of
                                                 // mount points
        ApiDeclaration mountPointApi = swagger.getMountPointApi(mockInfo, 1L, "Datastores", "-");
        assertNotNull("failed to find Datastore API", mountPointApi);
        List<Api> apis = mountPointApi.getApis();
        assertEquals("Unexpected api list size", 3, apis.size());

        Set<String> actualApis = new TreeSet<>();
        for (Api api : apis) {
            actualApis.add(api.getPath());
            List<Operation> operations = api.getOperations();
            assertEquals("unexpected operation size on " + api.getPath(), 1, operations.size());
            assertEquals("unexpected operation method " + api.getPath(), "GET", operations.get(0)
                    .getMethod());
            assertNotNull("expected non-null desc on " + api.getPath(), operations.get(0)
                    .getNotes());
        }
        Set<String> expectedApis = new TreeSet<>(Arrays.asList(new String[] {
                "/config/" + INSTANCE_URL + "yang-ext:mount/",
                "/operational/" + INSTANCE_URL + "yang-ext:mount/",
                "/operations/" + INSTANCE_URL + "yang-ext:mount/", }));
        assertEquals(expectedApis, actualApis);
    }

    protected UriInfo setUpSwaggerForDocGeneration() throws URISyntaxException {
        UriInfo mockInfo = helper.createMockUriInfo(HTTP_URL);
        // We are sharing the global schema service and the mount schema service
        // in our test.
        // OK for testing - real thing would have seperate instances.
        SchemaContext context = helper.createMockSchemaContext();
        SchemaService schemaService = helper.createMockSchemaService(context);

        MountProvisionInstance mountPoint = mock(MountProvisionInstance.class);
        when(mountPoint.getSchemaContext()).thenReturn(context);

        MountProvisionService service = mock(MountProvisionService.class);
        when(service.getMountPoint(instanceId)).thenReturn(mountPoint);
        swagger.setMountService(service);
        swagger.setGlobalSchema(schemaService);

        return mockInfo;
    }

}
