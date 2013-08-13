package org.opendaylight.controller.sample.toaster.it;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.maven;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sample.toaster.provider.ToasterProvider;
import org.opendaylight.controller.sample.toaster.provider.api.ToastConsumer;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.ToasterService;
import org.opendaylight.yang.gen.v1.http.netconfcentral.org.ns.toaster.rev20091120.WhiteBread;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(PaxExam.class)
public class ToasterTest {

	public static final String ODL = "org.opendaylight.controller";
	public static final String YANG = "org.opendaylight.yangtools";
	public static final String SAMPLE = "org.opendaylight.controller.samples";

	@Test
	public void properInitialized() throws Exception {

		Collection<ServiceReference<ToasterService>> references = ctx
				.getServiceReferences(ToasterService.class, null);
		assertEquals(2, references.size());
		
		consumer.createToast(WhiteBread.class, 5);
		
	}

	@Inject
	BindingAwareBroker broker;

	@Inject
	ToastConsumer consumer;

	@Inject
	BundleContext ctx;

	@Configuration
	public Option[] config() {
		return options(systemProperty("osgi.console").value("2401"),
				mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
				mavenBundle("org.slf4j", "log4j-over-slf4j")
						.versionAsInProject(),
				mavenBundle("ch.qos.logback", "logback-core")
						.versionAsInProject(),
				mavenBundle("ch.qos.logback", "logback-classic")
						.versionAsInProject(),
				mavenBundle(ODL, "sal-binding-api").versionAsInProject(),
				mavenBundle(ODL, "sal-binding-broker-impl")
						.versionAsInProject(), mavenBundle(ODL, "sal-common")
						.versionAsInProject(),
				mavenBundle(ODL, "sal-common-util").versionAsInProject(),
				mavenBundle(SAMPLE, "sample-toaster").versionAsInProject(),
				mavenBundle(SAMPLE, "sample-toaster-consumer")
						.versionAsInProject(),
				mavenBundle(SAMPLE, "sample-toaster-provider")
						.versionAsInProject(),
				mavenBundle(YANG, "yang-binding").versionAsInProject(),
				mavenBundle(YANG, "yang-common").versionAsInProject(),
				mavenBundle("com.google.guava", "guava").versionAsInProject(),
				junitBundles(), mavenBundle("org.javassist", "javassist")
						.versionAsInProject());
	}

}
