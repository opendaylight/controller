/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.store.spi;

import java.net.URL;
import java.util.Collection;

/**
 * Implementation of this interface gets notified when bundle containing yang files in META-INF/yang has been
 * added or removed. One notification is sent per one bundle.
 */
public interface YangStoreListener {

    void onAddedYangURL(Collection<URL> url);

    void onRemovedYangURL(Collection<URL> url);

}
