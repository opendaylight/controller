/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.mapping;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Date;
import java.util.Map;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.netconf.util.NetconfUtil;

public class SimpleAttributeMappingStrategy extends AbstractAttributeMappingStrategy<String, SimpleType<?>> {

    public SimpleAttributeMappingStrategy(SimpleType<?> openType) {
        super(openType);
    }

    @Override
    public Optional<String> mapAttribute(Object value) {
        if (value == null){
            return Optional.absent();
        }

        String expectedClass = getOpenType().getClassName();
        String realClass = value.getClass().getName();
        Preconditions.checkArgument(realClass.equals(expectedClass), "Type mismatch, expected " + expectedClass
                + " but was " + realClass);

        WriterPlugin prefferedPlugin = writerPlugins.get(value.getClass().getCanonicalName());
        prefferedPlugin = prefferedPlugin == null ? writerPlugins.get(DEFAULT_WRITER_PLUGIN) : prefferedPlugin;
        return Optional.of(prefferedPlugin.writeObject(value));
    }

    private static final String DEFAULT_WRITER_PLUGIN = "default";
    private static final Map<String, WriterPlugin> writerPlugins = Maps.newHashMap();
    static {
        writerPlugins.put(DEFAULT_WRITER_PLUGIN, new DefaultWriterPlugin());
        writerPlugins.put(Date.class.getCanonicalName(), new DatePlugin());
    }

    /**
     * Custom writer plugins must implement this interface.
     */
    static interface WriterPlugin {
        String writeObject(Object value);
    }

    static class DefaultWriterPlugin implements WriterPlugin {

        @Override
        public String writeObject(Object value) {
            return value.toString();
        }
    }

    static class DatePlugin implements WriterPlugin {

        @Override
        public String writeObject(Object value) {
            Preconditions.checkArgument(value instanceof Date, "Attribute must be Date");
            return NetconfUtil.writeDate((Date) value);
        }
    }

}
