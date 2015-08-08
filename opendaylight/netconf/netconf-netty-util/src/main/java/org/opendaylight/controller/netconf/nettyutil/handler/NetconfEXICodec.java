/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public final class NetconfEXICodec {
    /**
     * NETCONF is XML environment, so the use of EXI cookie is not really needed. Adding it
     * decreases efficiency of encoding by adding human-readable 4 bytes "EXI$" to the head
     * of the stream. This is really useful, so let's output it now.
     */
    private static final boolean OUTPUT_EXI_COOKIE = true;
    /**
     * OpenEXI does not allow us to directly prevent resolution of external entities. In order
     * to prevent XXE attacks, we reuse a single no-op entity resolver.
     */
    private static final EntityResolver ENTITY_RESOLVER = new EntityResolver() {
        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) {
            return new InputSource();
        }
    };

    /**
     * Since we have a limited number of options we can have, instantiating a weak cache
     * will allow us to reuse instances where possible.
     */
    private static final LoadingCache<Short, GrammarCache> GRAMMAR_CACHES = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<Short, GrammarCache>() {
        @Override
        public GrammarCache load(final Short key) {
            return new GrammarCache(key);
        }
    });

    /**
     * Grammar cache acts as a template and is duplicated by the Transmogrifier and the Reader
     * before use. It is safe to reuse a single instance.
     */
    private final GrammarCache exiGrammarCache;
    private final EXIOptions exiOptions;

    public NetconfEXICodec(final EXIOptions exiOptions) {
        this.exiOptions = Preconditions.checkNotNull(exiOptions);
        this.exiGrammarCache = createGrammarCache(exiOptions);
    }

    private static GrammarCache createGrammarCache(final EXIOptions exiOptions) {
        short go = GrammarOptions.DEFAULT_OPTIONS;
        if (exiOptions.getPreserveComments()) {
            go = GrammarOptions.addCM(go);
        }
        if (exiOptions.getPreserveDTD()) {
            go = GrammarOptions.addDTD(go);
        }
        if (exiOptions.getPreserveNS()) {
            go = GrammarOptions.addNS(go);
        }
        if (exiOptions.getPreservePIs()) {
            go = GrammarOptions.addPI(go);
        }

        return GRAMMAR_CACHES.getUnchecked(go);
    }

    EXIReader getReader() throws EXIOptionsException {
        final EXIReader r = new EXIReader();
        r.setPreserveLexicalValues(exiOptions.getPreserveLexicalValues());
        r.setGrammarCache(exiGrammarCache);
        r.setEntityResolver(ENTITY_RESOLVER);
        return r;
    }

    Transmogrifier getTransmogrifier() throws EXIOptionsException, TransmogrifierException {
        final Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.setAlignmentType(exiOptions.getAlignmentType());
        transmogrifier.setBlockSize(exiOptions.getBlockSize());
        transmogrifier.setGrammarCache(exiGrammarCache);
        transmogrifier.setOutputCookie(OUTPUT_EXI_COOKIE);
        transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
        transmogrifier.setResolveExternalGeneralEntities(false);
        return transmogrifier;
    }
}
