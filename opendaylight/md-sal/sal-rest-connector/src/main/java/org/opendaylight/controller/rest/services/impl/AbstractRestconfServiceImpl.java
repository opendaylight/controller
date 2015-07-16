/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.services.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.annotation.Nonnull;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.errors.RestconfDocumentedException;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorTag;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractRestconfServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRestconfServiceImpl.class);

    protected final RestBrokerFacade broker;
    protected final RestSchemaController schemaController;

    private static final URI NAMESPACE_EVENT_SUBSCRIPTION_AUGMENT = URI.create("urn:sal:restconf:event:subscription");
    protected static final String NETCONF_BASE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    protected static final String NETCONF_BASE_PAYLOAD_NAME = "data";
    protected static final QName NETCONF_BASE_QNAME;
    protected static final QNameModule SAL_REMOTE_AUGMENT;
    protected static final YangInstanceIdentifier.AugmentationIdentifier SAL_REMOTE_AUG_IDENTIFIER;

    static {
        try {
            final Date eventSubscriptionAugRevision = new SimpleDateFormat("yyyy-MM-dd").parse("2014-07-08");
            NETCONF_BASE_QNAME = QName.create(QNameModule.create(new URI(NETCONF_BASE), null),
                    NETCONF_BASE_PAYLOAD_NAME);
            SAL_REMOTE_AUGMENT = QNameModule.create(NAMESPACE_EVENT_SUBSCRIPTION_AUGMENT, eventSubscriptionAugRevision);
            SAL_REMOTE_AUG_IDENTIFIER = new YangInstanceIdentifier.AugmentationIdentifier(Sets.newHashSet(
                    QName.create(SAL_REMOTE_AUGMENT, "scope"), QName.create(SAL_REMOTE_AUGMENT, "datastore")));
        } catch (final ParseException e) {
            final String errMsg = "It wasn't possible to convert revision date of sal-remote-augment to date";
            LOG.debug(errMsg);
            throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED);
        } catch (final URISyntaxException e) {
            final String errMsg = "It wasn't possible to create instance of URI class with " + NETCONF_BASE + " URI";
            throw new RestconfDocumentedException(errMsg, ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED);
        }
    }

    /**
     * Default initialization method for all services
     *
     * @param broker
     * @param schemaController
     */
    public AbstractRestconfServiceImpl(@Nonnull final RestBrokerFacade broker,
            @Nonnull final RestSchemaController schemaController) {
        this.broker = Preconditions.checkNotNull(broker);
        this.schemaController = Preconditions.checkNotNull(schemaController);
    }

    protected Module getRestconfModule() {
        final Module restconfModule = schemaController.getRestconfModule();
        if (restconfModule == null) {
            LOG.debug("ietf-restconf module was not found.");
            throw new RestconfDocumentedException("ietf-restconf module was not found.", ErrorType.APPLICATION,
                    ErrorTag.OPERATION_NOT_SUPPORTED);
        }

        return restconfModule;
    }

    protected static <T> T resolveAsEnum(final Class<T> classDescriptor, final String value) {
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
