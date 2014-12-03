package org.opendaylight.controller.config.spi;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of Module. This implementation contains base logic for Module reconfiguration with associated fields.
 * @param <M> Type of module implementation. Enables easier implementation for the {@link #isSame} method
 */
public abstract class AbstractModule<M extends AbstractModule<M>> implements org.opendaylight.controller.config.spi.Module {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractModule.class);

    private final M oldModule;
    private final AutoCloseable oldInstance;
    protected final ModuleIdentifier identifier;
    private AutoCloseable instance;
    protected final DependencyResolver dependencyResolver;

    /**
     * Called when module is configured.
     *
     * @param identifier id of current instance.
     * @param dependencyResolver resolver used in dependency injection and validation.
     */
    public AbstractModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        this(identifier, dependencyResolver, null, null);
    }

    /**
     * Called when module is reconfigured.
     *
     * @param identifier id of current instance.
     * @param dependencyResolver resolver used in dependency injection and validation.
     * @param oldModule old instance of module that is being reconfigred(replaced) by current instance. The old instance can be examined for reuse.
     * @param oldInstance old instance wrapped by the old module. This is the resource that is actually being reused if possible or closed otherwise.
     */
    public AbstractModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver, M oldModule, AutoCloseable oldInstance) {
        this.identifier = identifier;
        this.dependencyResolver = dependencyResolver;
        this.oldModule = oldModule;
        this.oldInstance = oldInstance;
    }

    @Override
    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    /**
     *
     * General algorithm for spawning/closing and reusing wrapped instances.
     *
     * @return current instance of wrapped resource either by reusing the old one (if present) or constructing a brand new.
     */
    @Override
    public final AutoCloseable getInstance() {
        if(instance==null) {
            if(oldInstance!=null && canReuseInstance(oldModule)) {
                resolveDependencies();
                instance = reuseInstance(oldInstance);
            } else {
                if(oldInstance!=null) {
                    try {
                        oldInstance.close();
                    } catch(Exception e) {
                        LOG.error("An error occurred while closing old instance {} for module {}", oldInstance, getIdentifier(), e);
                    }
                }
                resolveDependencies();
                instance = createInstance();
                if (instance == null) {
                    throw new IllegalStateException("Error in createInstance - null is not allowed as return value. Module: " + getIdentifier());
                }
            }
        }
        return instance;
    }

    /**
     * @return Brand new instance of wrapped class in case no previous instance is present or reconfiguration is impossible.
     */
    protected abstract AutoCloseable createInstance();

    @Override
    public final boolean canReuse(Module oldModule) {
        // Just cast into a specific instance
        // TODO unify this method with canReuseInstance (required Module interface to be generic which requires quite a lot of changes)
        return getClass().isInstance(oldModule) ? canReuseInstance((M) oldModule) : false;
    }

    /**
     *
     * Users are welcome to override this method to provide custom logic for advanced reusability detection.
     *
     * @param oldModule old instance of a Module
     * @return true if the old instance is reusable false if a new one should be spawned
     */
    protected abstract boolean canReuseInstance(final M oldModule);

    /**
     * By default the oldInstance is returned since this method is by default called only if the oldModule had the same configuration and dependencies configured.
     * Users are welcome to override this method to provide custom logic for advanced reusability.
     *
     * @param oldInstance old instance of a class wrapped by the module
     * @return reused instance
     */
    protected AutoCloseable reuseInstance(AutoCloseable oldInstance) {
        // implement if instance reuse should be supported. Override canReuseInstance to change the criteria.
        return oldInstance;
    }

    /**
     * Inject all the dependencies using dependency resolver instance.
     */
    protected abstract void resolveDependencies();
}
