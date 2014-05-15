package org.opendaylight.controller.hosttracker.internal;

import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.tooling.exam.options.KarafDistributionConfigurationOption;
//import org.apache.karaf.tooling.exam.options.configs.CustomProperties;

//import static org.ops4j.pax.exam.CoreOptions.maven;

import org.junit.Test;
//import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ops4j.pax.exam.junit.Configuration;
//import org.ops4j.pax.exam.junit.ExamReactorStrategy;
//import org.ops4j.pax.exam.junit.JUnit4TestRunner;
//import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

//import EDU.oswego.cs.dl.util.concurrent.Callable;

@RunWith(JUnit4.class)
public class HostTrackerShellIT {
    private static final int COMMAND_TIMEOUT = 1000;
    private ExecutorService executor;

    @Configuration
    public KarafDistributionConfigurationOption config() {
        return new KarafDistributionConfigurationOption(
               "mvn:org.opendaylight.controller/distribution.opendaylight-karaf/1.4.2-SNAPSHOT/zip","odl","1.4.2-SNAPSHOT");
    }

    @Inject
    CommandProcessor commandProcessor;
    protected String executeCommands(final String... commands) {
        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final CommandSession commandSession = commandProcessor.createSession(
                System.in, printStream, System.err);
        FutureTask<String> commandFuture = new FutureTask<String>(
                new Callable<String>() {
                    public String call() {
                        try {
                            for (String command : commands) {
                                System.err.println(command);
                                commandSession.execute(command);
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        }
                        return byteArrayOutputStream.toString();
                    }
                });

        try {
            executor.submit(commandFuture);
            response = commandFuture
                    .get(COMMAND_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        }
        return response;
    }

    @Test
    public void test() throws Exception {
        assertTrue(true);
    }
    public void createInstances() {
        executeCommands("hosttracker:testDumpPendingARPReqList");
        executeCommands("testDumpFailedARPReqList");
    }
}


// testDumpPendingARPReqList()
// testDumpFailedARPReqList()