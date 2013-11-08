package org.opendaylight.controller.netconf.util.xml;

import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.FidelityOptions;
import com.siemens.ct.exi.GrammarFactory;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.grammars.Grammars;

public class ExiParameters {

    private static final String EXI_PARAMETER_ALIGNMENT = "alignment";
    private static final String EXI_PARAMETER_BYTE_ALIGNED = "byte-aligned";
    private static final String EXI_PARAMETER_COMPRESSED = "compressed";

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

    private static final String NETCONF_XSD_LOCATION = "";

    private FidelityOptions fidelityOptions;
    private Grammars grammars;
    private CodingMode codingMode = CodingMode.BIT_PACKED;

    public void setParametersFromXmlElement(XmlElement operationElement)
            throws EXIException {

        if (operationElement.getElementsByTagName(EXI_PARAMETER_ALIGNMENT)
                .getLength() > 0) {

            if (operationElement.getElementsByTagName(
                    EXI_PARAMETER_BYTE_ALIGNED)
                    .getLength() > 0) {
                this.codingMode = CodingMode.BYTE_PACKED;
            }

            if (operationElement.getElementsByTagName(
                    EXI_PARAMETER_BYTE_ALIGNED).getLength() > 0) {
                this.codingMode = CodingMode.BYTE_PACKED;
            }
            if (operationElement.getElementsByTagName(EXI_PARAMETER_COMPRESSED)
                    .getLength() > 0) {
                this.codingMode = CodingMode.COMPRESSION;
            }
        }

        if (operationElement.getElementsByTagName(EXI_PARAMETER_FIDELITY)
                .getLength() > 0) {

            this.fidelityOptions = FidelityOptions.createDefault();

            if (operationElement.getElementsByTagName(EXI_FIDELITY_DTD)
                    .getLength() > 0) {
                this.fidelityOptions.setFidelity(FidelityOptions.FEATURE_DTD,
                        true);
            }
            if (operationElement.getElementsByTagName(
                    EXI_FIDELITY_LEXICAL_VALUES)
                    .getLength() > 0) {
                this.fidelityOptions.setFidelity(
                        FidelityOptions.FEATURE_LEXICAL_VALUE, true);
            }

            if (operationElement.getElementsByTagName(EXI_FIDELITY_COMMENTS)
                    .getLength() > 0) {
                this.fidelityOptions.setFidelity(
                        FidelityOptions.FEATURE_COMMENT, true);
            }

            if (operationElement.getElementsByTagName(EXI_FIDELITY_PIS)
                    .getLength() > 0) {
                this.fidelityOptions.setFidelity(FidelityOptions.FEATURE_PI,
                        true);
            }

            if (operationElement.getElementsByTagName(EXI_FIDELITY_PREFIXES)
                    .getLength() > 0) {
                this.fidelityOptions.setFidelity(
                        FidelityOptions.FEATURE_PREFIX, true);
            }

        }

        if (operationElement.getElementsByTagName(EXI_PARAMETER_SCHEMA)
                .getLength() > 0) {

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

        }
    }

    public FidelityOptions getFidelityOptions() {
        return fidelityOptions;
    }

    public Grammars getGrammars() {
        return grammars;
    }

    public CodingMode getCodingMode() {
        return codingMode;
    }

}
