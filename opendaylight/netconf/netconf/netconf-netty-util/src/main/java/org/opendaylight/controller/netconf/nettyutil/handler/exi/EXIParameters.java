/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil.handler.exi;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class EXIParameters {
    private static final String EXI_PARAMETER_ALIGNMENT = "alignment";
    static final String EXI_PARAMETER_BYTE_ALIGNED = "byte-aligned";
    static final String EXI_PARAMETER_BIT_PACKED = "bit-packed";
    static final String EXI_PARAMETER_COMPRESSED = "compressed";
    static final String EXI_PARAMETER_PRE_COMPRESSION = "pre-compression";

    private static final String EXI_PARAMETER_FIDELITY = "fidelity";
    private static final String EXI_FIDELITY_DTD = "dtd";
    private static final String EXI_FIDELITY_LEXICAL_VALUES = "lexical-values";
    private static final String EXI_FIDELITY_COMMENTS = "comments";
    private static final String EXI_FIDELITY_PIS = "pis";
    private static final String EXI_FIDELITY_PREFIXES = "prefixes";

    private final EXIOptions options;

    private EXIParameters(final EXIOptions options) {
        this.options = Preconditions.checkNotNull(options);
    }


    public static EXIParameters fromXmlElement(final XmlElement root) throws EXIOptionsException {
        final EXIOptions options =  new EXIOptions();

        options.setAlignmentType(AlignmentType.bitPacked);

        final NodeList alignmentElements = root.getElementsByTagName(EXI_PARAMETER_ALIGNMENT);
        if (alignmentElements.getLength() > 0) {
            final Element alignmentElement = (Element) alignmentElements.item(0);
            final String alignmentTextContent = alignmentElement.getTextContent().trim();

            switch (alignmentTextContent) {
            case EXI_PARAMETER_BIT_PACKED:
                options.setAlignmentType(AlignmentType.bitPacked);
                break;
            case EXI_PARAMETER_BYTE_ALIGNED:
                options.setAlignmentType(AlignmentType.byteAligned);
                break;
            case EXI_PARAMETER_COMPRESSED:
                options.setAlignmentType(AlignmentType.compress);
                break;
            case EXI_PARAMETER_PRE_COMPRESSION:
                options.setAlignmentType(AlignmentType.preCompress);
                break;
            }
        }

        if (root.getElementsByTagName(EXI_PARAMETER_FIDELITY).getLength() > 0) {
            if (root.getElementsByTagName(EXI_FIDELITY_DTD).getLength() > 0) {
                options.setPreserveDTD(true);
            }
            if (root.getElementsByTagName(EXI_FIDELITY_LEXICAL_VALUES).getLength() > 0) {
                options.setPreserveLexicalValues(true);
            }
            if (root.getElementsByTagName(EXI_FIDELITY_COMMENTS).getLength() > 0) {
                options.setPreserveComments(true);
            }
            if (root.getElementsByTagName(EXI_FIDELITY_PIS).getLength() > 0) {
                options.setPreservePIs(true);
            }
            if (root.getElementsByTagName(EXI_FIDELITY_PREFIXES).getLength() > 0) {
                options.setPreserveNS(true);
            }
        }
        return new EXIParameters(options);
    }

    public final EXIOptions getOptions() {
        return options;
    }
}
