/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.xml;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;

import com.google.common.base.Preconditions;

public final class EXIParameters {
    private static final String EXI_PARAMETER_ALIGNMENT = "alignment";
    private static final String EXI_PARAMETER_BYTE_ALIGNED = "byte-aligned";
    private static final String EXI_PARAMETER_BIT_PACKED = "bit-packed";
    private static final String EXI_PARAMETER_COMPRESSED = "compressed";
    private static final String EXI_PARAMETER_PRE_COMPRESSION = "pre-compression";

    private static final String EXI_PARAMETER_FIDELITY = "fidelity";
    private static final String EXI_FIDELITY_DTD = "dtd";
    private static final String EXI_FIDELITY_LEXICAL_VALUES = "lexical-values";
    private static final String EXI_FIDELITY_COMMENTS = "comments";
    private static final String EXI_FIDELITY_PIS = "pis";
    private static final String EXI_FIDELITY_PREFIXES = "prefixes";

    private static final String EXI_PARAMETER_SCHEMA = "schema";
    private static final String EXI_PARAMETER_SCHEMA_NONE = "none";
    private static final String EXI_PARAMETER_SCHEMA_BUILT_IN = "builtin";
    private static final String EXI_PARAMETER_SCHEMA_BASE_1_1 = "base:1.1";

    private final EXIOptions options;

    private EXIParameters(final EXIOptions options) {
        this.options = Preconditions.checkNotNull(options);
    }

    public static EXIParameters fromNetconfMessage(final NetconfMessage root) throws EXIOptionsException {
        return fromXmlElement(XmlElement.fromDomDocument(root.getDocument()));
    }

    public static EXIParameters fromXmlElement(final XmlElement root) throws EXIOptionsException {
        final EXIOptions options =  new EXIOptions();

        options.setAlignmentType(AlignmentType.bitPacked);
        if (root.getElementsByTagName(EXI_PARAMETER_ALIGNMENT).getLength() > 0) {
            if (root.getElementsByTagName(EXI_PARAMETER_BIT_PACKED).getLength() > 0) {
                options.setAlignmentType(AlignmentType.bitPacked);
            } else if (root.getElementsByTagName(EXI_PARAMETER_BYTE_ALIGNED).getLength() > 0) {
                options.setAlignmentType(AlignmentType.byteAligned);
            } else if (root.getElementsByTagName(EXI_PARAMETER_COMPRESSED).getLength() > 0) {
                options.setAlignmentType(AlignmentType.compress);
            } else if (root.getElementsByTagName(EXI_PARAMETER_PRE_COMPRESSION).getLength() > 0) {
                options.setAlignmentType(AlignmentType.preCompress);
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

        if (root.getElementsByTagName(EXI_PARAMETER_SCHEMA).getLength() > 0) {
/*
                        GrammarFactory grammarFactory = GrammarFactory.newInstance();
                        if (operationElement
                                .getElementsByTagName(EXI_PARAMETER_SCHEMA_NONE)
                                .getLength() > 0) {
                            this.grammars = grammarFactory.createSchemaLessGrammars();
                        }

                        if (operationElement.getElementsByTagName(
                                EXI_PARAMETER_SCHEMA_BUILT_IN).getLength() > 0) {
                            this.grammars = grammarFactory.createXSDTypesOnlyGrammars();
                        }

                        if (operationElement.getElementsByTagName(
                                EXI_PARAMETER_SCHEMA_BASE_1_1).getLength() > 0) {
                            this.grammars = grammarFactory
                                    .createGrammars(NETCONF_XSD_LOCATION);
                        }
*/

        }

        return new EXIParameters(options);
    }

    public final EXIOptions getOptions() {
        return options;
    }
}
