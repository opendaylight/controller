/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import java.io.Serializable;
import java.util.Map;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Snapshot of a "state" of a DataBroker.
 *
 * Useful in tests to assert a DataBroker has an exact expected content. The
 * expected DataBrokerSnapshot could be statically created in the test (or,
 * hypothetically, read from a serialized file of some sort); the actual
 * DataBrokerSnapshot would be obtained from a live DataBroker used in
 * the test via a {@link DataBrokerSnapshoter}.  You would compare the
 * two via {@link #equals(Object)}, or (much more conveniently) the
 * ch.vorburger.xtendbeans.AssertBeans.assertEqualBeans helper.
 *
 * @see DataBrokerSnapshoter
 *
 * @author Michael Vorburger
 */
public interface DataBrokerSnapshot extends Serializable {

    Map<InstanceIdentifier<?>, DataObject> getOperationalDatastoreMap();

    Map<InstanceIdentifier<?>, DataObject> getConfigurationDatastoreMap();

    @Override
    boolean equals(Object anotherDataBrokerSnapshot);

}
