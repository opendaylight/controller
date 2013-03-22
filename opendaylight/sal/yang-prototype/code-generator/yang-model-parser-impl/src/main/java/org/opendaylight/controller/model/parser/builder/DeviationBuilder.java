/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import org.opendaylight.controller.model.parser.api.Builder;
import org.opendaylight.controller.model.parser.util.YangModelBuilderHelper;
import org.opendaylight.controller.yang.model.api.Deviation;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Deviation.Deviate;


public class DeviationBuilder implements MustAwareBuilder, Builder {

	private final DeviationImpl instance;
	private MustDefinitionBuilder mustDefinitionBuilder;

	DeviationBuilder(String targetPathStr) {
		SchemaPath targetPath = YangModelBuilderHelper.parsePath(targetPathStr);
		instance = new DeviationImpl(targetPath);
	}

	@Override
	public Deviation build() {
		// MUST definition
		if(mustDefinitionBuilder != null) {
			MustDefinition md = mustDefinitionBuilder.build();
			instance.setMustDefinition(md);
		}
		return instance;
	}

	public void setDeviate(String deviate) {
		if(deviate.equals("not-supported")) {
			instance.setDeviate(Deviate.NOT_SUPPORTED);
		} else if(deviate.equals("add")) {
			instance.setDeviate(Deviate.ADD);
		} else if(deviate.equals("replace")) {
			instance.setDeviate(Deviate.REPLACE);
		} else if(deviate.equals("delete")) {
			instance.setDeviate(Deviate.DELETE);
		} else {
			throw new IllegalArgumentException("Unsupported type of 'deviate' statement: "+ deviate);
		}
	}

	public void setReference(String reference) {
		instance.setReference(reference);
	}

	@Override
	public void setMustDefinitionBuilder(MustDefinitionBuilder mustDefinitionBuilder) {
		this.mustDefinitionBuilder = mustDefinitionBuilder;
	}

	private static class DeviationImpl implements Deviation {

		private SchemaPath targetPath;
		private Deviate deviate;
		private String reference;
		private MustDefinition mustDefinition;

		private DeviationImpl(SchemaPath targetPath) {
			this.targetPath = targetPath;
		}

		@Override
		public SchemaPath getTargetPath() {
			return targetPath;
		}

		@Override
		public Deviate getDeviate() {
			return deviate;
		}
		private void setDeviate(Deviate deviate) {
			this.deviate = deviate;
		}

		@Override
		public String getReference() {
			return reference;
		}
		private void setReference(String reference) {
			this.reference = reference;
		}

		@Override
		public String toString() {
			return DeviationImpl.class.getSimpleName() +"[targetPath="+ targetPath +", deviate="+ deviate +", reference="+ reference +", mustDefinition="+ mustDefinition +"]";
		}

		@Override
		public MustDefinition getMustDefinition() {
			return mustDefinition;
		}
		private void setMustDefinition(MustDefinition mustDefinition) {
			this.mustDefinition = mustDefinition;
		}

	}

}
