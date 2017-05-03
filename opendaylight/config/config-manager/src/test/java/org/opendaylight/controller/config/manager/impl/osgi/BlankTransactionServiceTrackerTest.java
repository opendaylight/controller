/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.manager.impl.osgi;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collections;
import javax.management.ObjectName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.osgi.framework.ServiceReference;

public class BlankTransactionServiceTrackerTest {
    @Mock
    private BlankTransactionServiceTracker.BlankTransaction blankTx;
    private BlankTransactionServiceTracker tracker;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(new CommitStatus(Collections.<ObjectName>emptyList(), Collections.<ObjectName>emptyList(),
                Collections.<ObjectName>emptyList())).when(blankTx).hit();
        tracker = new BlankTransactionServiceTracker(blankTx, 10, MoreExecutors.newDirectExecutorService());
    }

    @Test
    public void testBlankTransaction() throws Exception {
        tracker.addingService(getMockServiceReference());
        tracker.modifiedService(getMockServiceReference(), null);
        tracker.removedService(getMockServiceReference(), null);
        verify(blankTx, times(3)).hit();
    }

    @Test
    public void testValidationException() throws Exception {
        IllegalArgumentException argumentException = new IllegalArgumentException();
        ValidationException validationException = ValidationException.createForSingleException(new ModuleIdentifier("m", "i"), argumentException);
        doThrow(validationException).when(blankTx).hit();

        tracker.addingService(getMockServiceReference());
        verify(blankTx, times(10)).hit();
    }

    @Test
    public void testConflictingException() throws Exception {
        int maxAttempts = 2;
        tracker = new BlankTransactionServiceTracker(blankTx, maxAttempts, MoreExecutors.newDirectExecutorService());

        final ConflictingVersionException ex = new ConflictingVersionException();
        doThrow(ex).when(blankTx).hit();

        tracker.addingService(getMockServiceReference());
        verify(blankTx, times(maxAttempts)).hit();
    }

    private static ServiceReference<ModuleFactory> getMockServiceReference() {
        return mock(ServiceReference.class);
    }
}
