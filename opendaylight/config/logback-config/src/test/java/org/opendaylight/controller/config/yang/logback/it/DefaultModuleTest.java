package org.opendaylight.controller.config.yang.logback.it;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.ModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.logback.appender.AbstractAppenderModuleTest;
import org.opendaylight.controller.config.yang.logback.appender.ConsoleAppenderModuleFactory;
import org.opendaylight.controller.config.yang.logback.appender.FileAppenderModuleFactory;
import org.opendaylight.controller.config.yang.logback.appender.RollingFileAppenderModuleFactory;
import org.opendaylight.controller.config.yang.logback.config.LogbackModuleFactory;
import org.osgi.framework.BundleContext;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class DefaultModuleTest extends AbstractAppenderModuleTest {
    private static final ModuleFactory LOGBACK_MODULE_FACTORY = new LogbackModuleFactory();
    private static final ConsoleAppenderModuleFactory CONSOLE_APPENDER_MODULE_FACTORY = new ConsoleAppenderModuleFactory();
    private static final FileAppenderModuleFactory FILE_APPENDER_MODULE_FACTORY = new FileAppenderModuleFactory();
    private static final RollingFileAppenderModuleFactory ROLLING_FILE_APPENDER_MODULE_FACTORY = new RollingFileAppenderModuleFactory();

    final List<ModuleFactory> firstPass = asList(LOGBACK_MODULE_FACTORY);
    final List<ModuleFactory> secondPass = asList(LOGBACK_MODULE_FACTORY, CONSOLE_APPENDER_MODULE_FACTORY, FILE_APPENDER_MODULE_FACTORY);
    final List<ModuleFactory> thirdPass = asList(LOGBACK_MODULE_FACTORY, CONSOLE_APPENDER_MODULE_FACTORY, FILE_APPENDER_MODULE_FACTORY,
            ROLLING_FILE_APPENDER_MODULE_FACTORY);

    final List<List<ModuleFactory>> listOfLists = asList(firstPass, secondPass, thirdPass);

    @Override
    protected Collection<? extends ModuleFactory> getTestedFactories() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUp() {


        super.initConfigTransactionManagerImpl(new ModuleFactoriesResolver() {
            int idx = 0;

            @Override
            public Map<String, Entry<ModuleFactory, BundleContext>> getAllFactories() {
                List<ModuleFactory> moduleFactories = listOfLists.get(idx++);

                return new HardcodedModuleFactoriesResolver(moduleFactories.toArray(new ModuleFactory[]{})).getAllFactories();
            }
        });
    }

    @Test
    public void simulateOSGi() throws Exception {
        // multiple transactions, until all required appenders are found, logback-config should not be instantiated
        resettingLogbackTestBase.reconfigureUsingClassPathFile("/real_world_logback.xml");
        {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            assertFactoriesMatch(firstPass, transaction);
            CommitStatus status = transaction.commit();
            assertStatus(status, 0, 0, 0);
        }
        {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            assertFactoriesMatch(secondPass, transaction);
            CommitStatus status = transaction.commit();
            assertStatus(status, 2 /* appenders */, 0, 0);
        }
        {
            ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
            assertFactoriesMatch(thirdPass, transaction);
            CommitStatus status = transaction.commit();
            assertStatus(status, 2 /* appender + logback */, 0, 2);
        }
    }

    private void assertFactoriesMatch(List<ModuleFactory> factories, ConfigTransactionJMXClient transactionJMXClient) {
        Function<ModuleFactory,String> moduleFactoryToFactoryNameFunction = new Function<ModuleFactory, String>() {
            @Nullable
            @Override
            public String apply(@Nullable ModuleFactory input) {
                return input.getImplementationName();
            }
        };
        assertEquals(new HashSet<>(Lists.transform(factories, moduleFactoryToFactoryNameFunction)), transactionJMXClient.getAvailableModuleNames());
    }
}
