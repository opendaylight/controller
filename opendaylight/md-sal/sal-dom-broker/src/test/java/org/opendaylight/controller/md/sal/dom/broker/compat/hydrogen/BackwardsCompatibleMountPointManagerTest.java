/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.dom.api.DOMService;
import org.opendaylight.controller.md.sal.dom.broker.impl.mount.DOMMountPointServiceImpl;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionListener;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class BackwardsCompatibleMountPointManagerTest {
    private static final Logger log = LoggerFactory.getLogger(BackwardsCompatibleMountPointManagerTest.class);

    @Mock
    private DOMMountPointServiceImpl domMountPointService;
    @Mock
    private DOMMountPointService.DOMMountPointBuilder mountBuilder;

    private BackwardsCompatibleMountPointManager compatibleMountPointManager;
    static final QName qName = QName.create("namespace", "12-12-1212", "mount");
    static final YangInstanceIdentifier id = YangInstanceIdentifier.of(qName);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        stubMountService();
        compatibleMountPointManager = new BackwardsCompatibleMountPointManager(domMountPointService);
    }

    public void testCreateMountpointAlreadyCreated() throws Exception {
        compatibleMountPointManager.createMountPoint(id);
        verify(domMountPointService).createMountPoint(id);
        verify(mountBuilder, times(3)).addService(any(Class.class), any(DOMService.class));
        verify(mountBuilder).addInitialSchemaContext(any(SchemaContext.class));

        try {
            compatibleMountPointManager.createMountPoint(id);
        } catch (final IllegalStateException e) {
            log.debug("", e);
            return;
        }
        fail("Should fail to create duplicate mount");
    }

    @Test
    public void testCreateMountpointGetOrCreate() throws Exception {
        compatibleMountPointManager = new BackwardsCompatibleMountPointManager(new DOMMountPointServiceImpl());

        final MountProvisionListener listener = new MountProvisionListener() {
            public int createdMounts = 0;

            @Override
            public void onMountPointCreated(final YangInstanceIdentifier path) {
                if(createdMounts++ > 1 ) {
                    fail("Only one mount point should have been created");
                }
            }

            @Override
            public void onMountPointRemoved(final YangInstanceIdentifier path) {}
        };

        compatibleMountPointManager.registerProvisionListener(listener);

        final MountProvisionInstance m1 = compatibleMountPointManager.createOrGetMountPoint(id);
        m1.setSchemaContext(mockSchemaContext());
        compatibleMountPointManager.createOrGetMountPoint(id);
        compatibleMountPointManager.createOrGetMountPoint(id);
    }

    private void stubMountService() {
        doReturn(mockMountPointBuilder()).when(domMountPointService).createMountPoint(any(YangInstanceIdentifier.class));
        doReturn(Optional.of(mockMountPoint())).when(domMountPointService).getMountPoint(any(YangInstanceIdentifier.class));
    }

    private DOMMountPoint mockMountPoint() {
        final DOMMountPoint mock = mock(DOMMountPoint.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return Optional.of(mock(((Class<?>) invocation.getArguments()[0])));
            }
        }).when(mock).getService(any(Class.class));
        doReturn(mockSchemaContext()).when(mock).getSchemaContext();
        return mock;
    }

    static SchemaContext mockSchemaContext() {
        final SchemaContext mock = mock(SchemaContext.class);
        doReturn(qName).when(mock).getQName();
        doReturn("schema").when(mock).toString();
        doReturn(mock(DataSchemaNode.class)).when(mock).getDataChildByName(any(QName.class));
        return mock;
    }

    private DOMMountPointService.DOMMountPointBuilder mockMountPointBuilder() {
        doReturn(mountBuilder).when(mountBuilder).addService(any(Class.class), any(DOMService.class));
        doReturn(mockObjectRegistration()).when(mountBuilder).register();
        doReturn(mountBuilder).when(mountBuilder).addInitialSchemaContext(any(SchemaContext.class));
        return mountBuilder;
    }

    private ObjectRegistration<?> mockObjectRegistration() {
        return mock(ObjectRegistration.class);
    }

}
