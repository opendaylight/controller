/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

/**
 * Utility class for cached thread-local transformers. This class exists mostly for use by handlers.
 */
final class ThreadLocalTransformers {
    private static final TransformerFactory FACTORY = TransformerFactory.newInstance();

    private static final ThreadLocal<Transformer> DEFAULT_TRANSFORMER = new ThreadLocal<Transformer>() {
        @Override
        protected Transformer initialValue() {
            try {
                return FACTORY.newTransformer();
            } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
                throw new IllegalStateException("Unexpected error while instantiating a Transformer", e);
            }
        };

        @Override
        public void set(final Transformer value) {
            throw new UnsupportedOperationException();
        };
    };

    private static final ThreadLocal<Transformer> PRETTY_TRANSFORMER = new ThreadLocal<Transformer>() {
        @Override
        protected Transformer initialValue() {
            final Transformer ret;

            try {
                ret = FACTORY.newTransformer();
            } catch (TransformerConfigurationException | TransformerFactoryConfigurationError e) {
                throw new IllegalStateException("Unexpected error while instantiating a Transformer", e);
            }

            ret.setOutputProperty(OutputKeys.INDENT, "yes");
            ret.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            return ret;
        };

        @Override
        public void set(final Transformer value) {
            throw new UnsupportedOperationException();
        };
    };

    private ThreadLocalTransformers() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get the transformer with default configuration.
     *
     * @return A transformer with default configuration based on the default implementation.
     */
    public static Transformer getDefaultTransformer() {
        return DEFAULT_TRANSFORMER.get();
    }

    /**
     * Get the transformer with default configuration, but with automatic indentation
     * and the XML declaration removed.
     *
     * @return A transformer with human-friendly configuration.
     */
    public static Transformer getPrettyTransformer() {
        return PRETTY_TRANSFORMER.get();
    }
}
