package org.opendaylight.controller.sal.rest.impl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.events.StartElement;

import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.IdentityValue;
import org.opendaylight.controller.sal.restconf.impl.IdentityValuesDTO.Predicate;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

public final class RestUtil {

    public final static TypeDefinition<?> resolveBaseTypeFrom(TypeDefinition<?> type) {
        TypeDefinition<?> superType = type;
        while (superType.getBaseType() != null) {
            superType = superType.getBaseType();
        }
        return superType;
    }

    public static IdentityValuesDTO asInstanceIdentifier(String value, PrefixesMaping prefixMap) {
        String valueTrimmed = value.trim();
        String[] xPathParts = valueTrimmed.split("/");
        if (xPathParts[0].trim().equals("")) {
            if (xPathParts.length > 1) {
                IdentityValuesDTO identityValuesDTO = new IdentityValuesDTO();
                for (int i = 1; i < xPathParts.length; i++) {
                    String xPathPartTrimmed = xPathParts[i].trim();
                    String xPathPartStr = getIdAndPrefixAsStr(xPathPartTrimmed);
                    String predicatesStr = getPredicatesAsStr(xPathPartTrimmed);

                    IdentityValue identityValue = toIdentity(xPathPartStr, prefixMap);
                    List<Predicate> predicates = toPredicates(predicatesStr, prefixMap);

                    identityValue.setPredicates(predicates);
                    identityValuesDTO.add(identityValue);
                }
                return identityValuesDTO.getValuesWithNamespaces().isEmpty() ? null : identityValuesDTO;
            } else {
                throw new ParsingException(value + " doesn't contain character '/'.");
            }
        } else {
            throw new ParsingException("Character '/' is missing at the begining of the path.");
        }
    }

    private static List<Predicate> toPredicates(String predicatesStr, PrefixesMaping prefixMap) {
        List<Predicate> result = new ArrayList<>();
        String[] predicates = predicatesStr.split("\\[");
        String beforeFirstBraceTrimmed = predicates[0].trim();
        if (beforeFirstBraceTrimmed.equals("")) {
            for (int i = 1; i < predicates.length; i++) {
                String predicateStrTrimmed = predicates[i].trim();
                if (predicateStrTrimmed.lastIndexOf("]") != predicateStrTrimmed.length() - 1) {
                    throw new ParsingException("Behind the predicate brace ']' is wrong character sequence >"
                            + predicateStrTrimmed + "<.");
                } else {
                    predicateStrTrimmed = predicateStrTrimmed.substring(0, predicateStrTrimmed.length() - 1);
                    predicateStrTrimmed = predicateStrTrimmed.trim();
                }
                int indexOfEqualityMark = predicateStrTrimmed.indexOf("=");
                if (indexOfEqualityMark != -1) {
                    IdentityValue identityValue = toIdentity(predicateStrTrimmed.substring(0, indexOfEqualityMark),
                            prefixMap);
                    String predicateValue = toPredicateValue(predicateStrTrimmed.substring(indexOfEqualityMark + 1));
                    result.add(new Predicate(identityValue, predicateValue));
                } else {
                    throw new ParsingException("Character '=' in predicate " + predicateStrTrimmed + " is missing.");
                }
            }
        } else {
            throw new ParsingException("Character sequence >" + beforeFirstBraceTrimmed + "< is before '[' character.");
        }

        return result;
    }

    private static String toPredicateValue(String predicatedValue) {
        String predicatedValueTrimmed = predicatedValue.trim();
        if (predicatedValueTrimmed.length() >= 2) {
            if (predicatedValueTrimmed.charAt(0) != (char) '"'
                    || predicatedValueTrimmed.charAt(predicatedValueTrimmed.length() - 1) != (char) '"') {
                throw new ParsingException("Predicate " + predicatedValueTrimmed
                        + " doesn't start and end with '=' character.");
            } else {
                return predicatedValueTrimmed.substring(1, predicatedValueTrimmed.length() - 1);
            }
        } else {
            throw new ParsingException("Predicate " + predicatedValueTrimmed
                    + " doesn't contains at least 2 '=' marks.");
        }
    }

    private static IdentityValue toIdentity(String xPathPart, PrefixesMaping prefixMap) {
        String xPathPartTrimmed = xPathPart.trim();
        int lastIndexOfColon = xPathPartTrimmed.lastIndexOf(":");
        String prefix = null;
        String identifier = null;
        if (lastIndexOfColon == -1) { // it is not "prefix:value"
            throw new ParsingException(
                    "All node names in an instance-identifier value MUST be qualified with explicit namespace prefix (see RFC 6020 9.13.3)");
        } else {
            prefix = xPathPart.substring(0, lastIndexOfColon).trim();
            if (prefix != null && !prefix.isEmpty()) {
                identifier = xPathPart.substring(lastIndexOfColon + 1).trim();
            }
        }
        if (prefix != null) {
            return new IdentityValue(prefixMap.getNamespace(prefix), identifier, prefix);
        } else {
            throw new ParsingException("Namespace value can't be found for identifier " + identifier + ".");
        }
    }

    private static String getPredicatesAsStr(String pathPart) {
        int predicateStartIndex = pathPart.indexOf("[");
        return predicateStartIndex == -1 ? "" : pathPart.substring(predicateStartIndex);
    }

    private static String getIdAndPrefixAsStr(String pathPart) {
        int predicateStartIndex = pathPart.indexOf("[");
        return predicateStartIndex == -1 ? pathPart : pathPart.substring(0, predicateStartIndex);
    }

    public interface PrefixesMaping {
        public String getNamespace(String prefix);
    }

    public static class PrefixMapingFromXml implements PrefixesMaping {
        StartElement startElement = null;

        public PrefixMapingFromXml(StartElement startElement) {
            this.startElement = startElement;
        }

        @Override
        public String getNamespace(String prefix) {
            return startElement.getNamespaceContext().getNamespaceURI(prefix);
        }
    }

    public static class PrefixMapingFromJson implements PrefixesMaping {

        @Override
        public String getNamespace(String prefix) {
            return prefix;
        }
    }

}
