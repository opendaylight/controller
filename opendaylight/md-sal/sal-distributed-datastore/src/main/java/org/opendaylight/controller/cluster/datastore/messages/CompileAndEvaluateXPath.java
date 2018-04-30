/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.concurrent.ThreadSafe;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Request to compile an XPath string with specified prefix binding and execute it in context of specified
 * data tree node.
 *
 * @author Robert Varga
 */
@Beta
@ThreadSafe
public final class CompileAndEvaluateXPath extends AbstractEvaluateXPath {
    private static final long serialVersionUID = 1L;

    private ImmutableBiMap<String, QNameModule> prefixMapping;
    private String xpath;

    public CompileAndEvaluateXPath() {
        // For Externalizable
    }

    public CompileAndEvaluateXPath(final @NonNull YangInstanceIdentifier path, final @NonNull String xpath,
            final @NonNull BiMap<String, QNameModule> prefixMapping, final short version) {
        super(path, version);
        this.xpath = requireNonNull(xpath);
        this.prefixMapping = ImmutableBiMap.copyOf(prefixMapping);
    }

    /**
     * Return the XPath string which needs to be compiled and evaluated.
     *
     * @return XPath string
     */
    public @NonNull String getXPath() {
        return requireNonNull(xpath);
    }

    /**
     * Return the prefix mapping to use when compiling the XPath string from {@link #getXPath()}.
     *
     * @return Prefix mapping
     */
    public @NonNull BiMap<String, QNameModule> getPrefixMapping() {
        return requireNonNull(prefixMapping);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        xpath = in.readUTF();
        prefixMapping = (ImmutableBiMap<String, QNameModule>) in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeUTF(xpath);
        out.writeObject(prefixMapping);
    }
}
