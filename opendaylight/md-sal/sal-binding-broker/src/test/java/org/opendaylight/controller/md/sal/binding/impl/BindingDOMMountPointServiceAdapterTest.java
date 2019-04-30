/*
 * Copyright (c) 2019 FRINX and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static org.mockito.Matchers.any;

import com.google.common.base.Optional;
import com.google.common.cache.LoadingCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class BindingDOMMountPointServiceAdapterTest {

    @Mock
    public DOMMountPointService mountService;
    @Mock
    private BindingToNormalizedNodeCodec codec;
    @Mock
    private BindingNormalizedNodeCodecRegistry codecRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(invocationOnMock -> Optional.of(Mockito.mock(DOMMountPoint.class)))
                .when(mountService).getMountPoint(any(YangInstanceIdentifier.class));
        Mockito.doReturn(YangInstanceIdentifier.create(new YangInstanceIdentifier.NodeIdentifier(QName.create("(a)b"))))
                .when(codec).toYangInstanceIdentifierBlocking(any(InstanceIdentifier.class));
        Mockito.doReturn(codecRegistry).when(codec).getCodecRegistry();
        Mockito.doReturn(InstanceIdentifier.create(DataObject.class))
                .when(codecRegistry).fromYangInstanceIdentifier(any(YangInstanceIdentifier.class));
    }

    @Test(timeout = 30 * 1000)
    public void testCaching() throws Exception {
        BindingDOMMountPointServiceAdapter baService = new BindingDOMMountPointServiceAdapter(mountService, codec);
        LoadingCache<DOMMountPoint, BindingMountPointAdapter> cache = baService.bindingMountpoints;

        baService.getMountPoint(InstanceIdentifier.create(DataObject.class));

        while (true) {
            cache.cleanUp();
            System.gc();
            Thread.sleep(100);
            if (cache.asMap().keySet().size() == 0) {
                // Cache has been cleared, the single cache entry was garbage collected
                return;
            }
        }
    }
}