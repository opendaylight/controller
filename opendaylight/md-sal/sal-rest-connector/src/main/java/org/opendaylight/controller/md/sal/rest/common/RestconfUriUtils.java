/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.AugmentationNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Feb 5, 2015
 */
public class RestconfUriUtils {

    private RestconfUriUtils () {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final YangInstanceIdentifier.AugmentationIdentifier SAL_REMOTE_AUG_IDENTIFIER;
    private static final QNameModule SAL_REMOTE_AUGMENT;

    static {
        try {
            final Date eventSubscriptionAugRevision = RestconfInternalConstants.REVISION_FORMAT.parse("2014-07-08");
            SAL_REMOTE_AUGMENT = QNameModule.create(RestconfInternalConstants.NAMESPACE_EVENT_SUBSCRIPTION_AUGMENT,
                    eventSubscriptionAugRevision);
            SAL_REMOTE_AUG_IDENTIFIER = new YangInstanceIdentifier.AugmentationIdentifier(Sets.newHashSet(QName.create(SAL_REMOTE_AUGMENT, "scope"),
                    QName.create(SAL_REMOTE_AUGMENT, "datastore")));
        } catch (final ParseException e) {
            final String errMsg = "It wasn't possible to convert revision date of sal-remote-augment to date";
            throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED);
        }
    }

    public static Map<String, String> resolveValuesFromUri(final String uri) {
        final Map<String, String> result = new HashMap<>();
        final String[] tokens = uri.split("/");
        for (int i = 1; i < tokens.length; i++) {
            final String[] parameterTokens = tokens[i].split("=");
            if (parameterTokens.length == 2) {
                result.put(parameterTokens[0], parameterTokens[1]);
            }
        }
        return result;
    }

    public static boolean parsePrettyPrintParameter(final UriInfo info) {
        final String param = info.getQueryParameters(false)
                .getFirst(RestconfInternalConstants.URI_PARAM_PRETTY_PRINT);
        return Boolean.parseBoolean(param);
    }

    public Integer parseDepthParameter(final UriInfo info) {
        final String param = info.getQueryParameters(false).getFirst(RestconfInternalConstants.URI_PARAM_DEPTH);
        if (Strings.isNullOrEmpty(param) || "unbounded".equals(param)) {
            return null;
        }

        try {
            final Integer depth = Integer.valueOf(param);
            RestconfValidationUtils.checkDocumentedError(depth > 1, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "The depth parameter must be an integer > 1 or \"unbounded\"");
            return depth;
        } catch (final NumberFormatException e) {
            throw new RestconfDocumentedException(new RestconfError(ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "Invalid depth parameter: " + e.getMessage(), null,
                    "The depth parameter must be an integer > 1 or \"unbounded\""));
        }
    }

    /**
     * Load parameter for subscribing to stream from input composite node
     *
     * @param compNode
     *            contains value
     * @return enum object if its string value is equal to {@code paramName}. In other cases null.
     */
    public static <T> T parseEnumTypeParameter(final ContainerNode value, final Class<T> classDescriptor,
            final String paramName) {
        final Optional<DataContainerChild<? extends PathArgument, ?>> augNode = value.getChild(SAL_REMOTE_AUG_IDENTIFIER);
        if (!augNode.isPresent() && !(augNode instanceof AugmentationNode)) {
            return null;
        }
        final Optional<DataContainerChild<? extends PathArgument, ?>> enumNode =
                ((AugmentationNode) augNode.get()).getChild(new NodeIdentifier(QName.create(SAL_REMOTE_AUGMENT, paramName)));
        if (!enumNode.isPresent()) {
            return null;
        }
        final Object rawValue = enumNode.get().getValue();
        if (!(rawValue instanceof String)) {
            return null;
        }

        return resolveAsEnum(classDescriptor, (String) rawValue);
    }

    /**
     * Checks whether {@code value} is one of the string representation of enumeration {@code classDescriptor}
     *
     * @return enum object if string value of {@code classDescriptor} enumeration is equal to {@code value}. Other cases
     *         null.
     */
    public static <T> T parserURIEnumParameter(final Class<T> classDescriptor, final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        return resolveAsEnum(classDescriptor, value);
    }

    private static <T> T resolveAsEnum(final Class<T> classDescriptor, final String value) {
        final T[] enumConstants = classDescriptor.getEnumConstants();
        if (enumConstants != null) {
            for (final T enm : classDescriptor.getEnumConstants()) {
                if (((Enum<?>) enm).name().equals(value)) {
                    return enm;
                }
            }
        }
        return null;
    }
}
