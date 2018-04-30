/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.schema.xpath.XPathResult;

/**
 * Reply to any of the {@link AbstractEvaluateXPath} requests.
 *
 * @author Robert Varga
 */
@Beta
@NonNullByDefault
public final class EvaluateXPathReply extends VersionedExternalizableMessage {
    private static final long serialVersionUID = 1L;

    // FIXME: this is not quite right: we need to translate this to a serializable format
    private @Nullable XPathResult<?> result;

    public EvaluateXPathReply() {
        // For Externalizable
    }

    public EvaluateXPathReply(final @Nullable XPathResult<?> result, final short version) {
        super(version);
        this.result = result;
    }

    public EvaluateXPathReply(final @Nullable XPathResult<?> result) {
        this.result = result;
    }

    public Optional<? extends XPathResult<?>> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public void readExternal(final @Nullable ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        result = (XPathResult<?>) in.readObject();
    }

    @Override
    public void writeExternal(final @Nullable ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(result);
    }
}
