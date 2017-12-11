/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.mapping;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.management.openmbean.SimpleType;
import org.opendaylight.yangtools.yang.common.Revision;

public class SimpleAttributeMappingStrategy extends AbstractAttributeMappingStrategy<String, SimpleType<?>> {

    public SimpleAttributeMappingStrategy(final SimpleType<?> openType) {
        super(openType);
    }

    @Override
    public Optional<String> mapAttribute(final Object value) {
        if (value == null) {
            return Optional.absent();
        }

        String expectedClass = getOpenType().getClassName();
        String realClass = value.getClass().getName();
        Preconditions.checkArgument(realClass.equals(expectedClass),
                "Type mismatch, expected " + expectedClass + " but was " + realClass);

        WriterPlugin prefferedPlugin = WRITER_PLUGINS.get(value.getClass().getCanonicalName());
        prefferedPlugin = prefferedPlugin == null ? WRITER_PLUGINS.get(DEFAULT_WRITER_PLUGIN) : prefferedPlugin;
        return Optional.of(prefferedPlugin.writeObject(value));
    }

    private static final String DEFAULT_WRITER_PLUGIN = "default";
    private static final Map<String, WriterPlugin> WRITER_PLUGINS = Maps.newHashMap();

    static {
        WRITER_PLUGINS.put(DEFAULT_WRITER_PLUGIN, new DefaultWriterPlugin());
        WRITER_PLUGINS.put(Revision.class.getCanonicalName(), new RevisionPlugin());
    }

    /**
     * Custom writer plugins must implement this interface.
     */
    interface WriterPlugin {
        String writeObject(Object value);
    }

    static class DefaultWriterPlugin implements WriterPlugin {

        @Override
        public String writeObject(final Object value) {
            return value.toString();
        }
    }

    static class RevisionPlugin implements WriterPlugin {

        @Override
        public String writeObject(final Object value) {
            Preconditions.checkArgument(value instanceof Revision, "Attribute must be Date");
            return ((Revision) value).toString();
        }
    }

}
