/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import org.opendaylight.controller.model.parser.api.Builder;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;


public class MustDefinitionBuilder implements Builder {

	private final MustDefinitionImpl instance;

	MustDefinitionBuilder(String xpathStr) {
		instance = new MustDefinitionImpl(xpathStr);
	}

	@Override
	public MustDefinition build() {
		return instance;
	}

	public void setDescription(String description) {
		instance.setDescription(description);
	}

	public void setReference(String reference) {
		instance.setReference(reference);
	}

	private static class MustDefinitionImpl implements MustDefinition {

		private final String xpathStr;
		private String description;
		private String reference;

		private MustDefinitionImpl(String xpathStr) {
			this.xpathStr = xpathStr;
		}

		@Override
		public String getDescription() {
			return description;
		}
		private void setDescription(String description) {
			this.description = description;
		}

		@Override
		public String getErrorAppTag() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getErrorMessage() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getReference() {
			return reference;
		}
		private void setReference(String reference) {
			this.reference = reference;
		}

		@Override
		public RevisionAwareXPath getXpath() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toString() {
			return MustDefinitionImpl.class.getSimpleName() +"[xpathStr="+ xpathStr +"]";
		}
	}

}
