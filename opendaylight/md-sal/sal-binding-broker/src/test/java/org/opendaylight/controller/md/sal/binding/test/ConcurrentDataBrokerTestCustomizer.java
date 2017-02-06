/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;

/**
 * ConcurrentDataBrokerTestCustomizer.
 *
 * <p>See {@link AbstractConcurrentDataBrokerTest} and <a href="https://bugs.opendaylight.org/show_bug.cgi?id=7538">bug 7538</a> for more details & background.
 *
 * @author Michael Vorburger
 */
public class ConcurrentDataBrokerTestCustomizer extends AbstractDataBrokerTestCustomizer {

    @Override
    public ListeningExecutorService getCommitCoordinatorExecutor() {
        return MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }
}
