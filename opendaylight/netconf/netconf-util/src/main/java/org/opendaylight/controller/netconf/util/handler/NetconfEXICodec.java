package org.opendaylight.controller.netconf.util.handler;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.EXIReader;
import org.openexi.sax.Transmogrifier;

import com.google.common.base.Preconditions;

final class NetconfEXICodec {
    /**
     * NETCONF is XML environment, so the use of EXI cookie is not really needed. Adding it
     * decreases efficiency of encoding by adding human-readable 4 bytes "EXI$" to the head
     * of the stream. This is really useful, so let's output it now.
     */
    private static final boolean OUTPUT_EXI_COOKIE = true;
    private final AlignmentType alignmentType;
    private final EXIOptions exiOptions;

    public NetconfEXICodec(final AlignmentType alignmentType, final EXIOptions exiOptions) {
        this.alignmentType = Preconditions.checkNotNull(alignmentType);
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

    Transmogrifier getTransmogrifier() throws EXIOptionsException {
        final Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.setAlignmentType(alignmentType);
        transmogrifier.setBlockSize(exiOptions.getBlockSize());
        transmogrifier.setGrammarCache(getGrammarCache());
        transmogrifier.setOutputCookie(OUTPUT_EXI_COOKIE);
        transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
        return transmogrifier;
    }
}
