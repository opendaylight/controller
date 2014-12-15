package org.opendaylight.controller.netconf.nettyutil.handler;

import com.google.common.base.Preconditions;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;

public final class NetconfEXICodec {
    /**
     * NETCONF is XML environment, so the use of EXI cookie is not really needed. Adding it
     * decreases efficiency of encoding by adding human-readable 4 bytes "EXI$" to the head
     * of the stream. This is really useful, so let's output it now.
     */
    private static final boolean OUTPUT_EXI_COOKIE = true;
    private final EXIOptions exiOptions;

    public NetconfEXICodec(final EXIOptions exiOptions) {
        this.exiOptions = Preconditions.checkNotNull(exiOptions);
    }

    private GrammarCache getGrammarCache() {
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

        return new GrammarCache(null, go);
    }

    EXIReader getReader() throws EXIOptionsException {
        final EXIReader r = new EXIReader();
        r.setPreserveLexicalValues(exiOptions.getPreserveLexicalValues());
        r.setGrammarCache(getGrammarCache());
        return r;
    }

    Transmogrifier getTransmogrifier() throws EXIOptionsException, TransmogrifierException {
        final Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.setAlignmentType(exiOptions.getAlignmentType());
        transmogrifier.setBlockSize(exiOptions.getBlockSize());
        transmogrifier.setGrammarCache(getGrammarCache());
        transmogrifier.setOutputCookie(OUTPUT_EXI_COOKIE);
        transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
        transmogrifier.setResolveExternalGeneralEntities(false);
        return transmogrifier;
    }
}
