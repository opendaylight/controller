/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.datastore.node.utils.stream.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Base class for XPath evaluation requests.
 *
 * @author Robert Varga
 */
abstract class AbstractEvaluateXPath extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    private YangInstanceIdentifier path;

    AbstractEvaluateXPath() {
        // For Externalizable
    }

    AbstractEvaluateXPath(final @NonNull YangInstanceIdentifier path, short version) {
        super(version);
        this.path = requireNonNull(path);
    }

    /**
     * Return the path of node on which this request should be evaluated.
     *
     * @return Node path
     */
    public final @NonNull YangInstanceIdentifier getPath() {
        return requireNonNull(path);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        path = SerializationUtils.deserializePath(in);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        SerializationUtils.serializePath(path, out);
    }
}
