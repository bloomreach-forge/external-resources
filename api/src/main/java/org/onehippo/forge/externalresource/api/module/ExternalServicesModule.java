package org.onehippo.forge.externalresource.api.module;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.forge.externalresource.api.ExternalService;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = {Synchronizable.class})
public class ExternalServicesModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(ExternalServicesModule.class);
    public static final String PLUGIN_CLASS = "plugin.class";
    private final Map<String, ExternalService> oldServices = new HashMap<>();

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        log.info("Configuring ExternalServicesModule");
        final NodeIterator nodes = moduleConfig.getNodes();
        /**
         * TODO: refactor this part, currently as a workaround
         * because retrieving instance and un-registering it doesn't  work
         */
        for (ExternalService service : oldServices.values()) {
            service.close();
        }
        oldServices.clear();
        // process config nodes & register services
        while (nodes.hasNext()) {
            final Node node = nodes.nextNode();
            if (node.hasProperty(PLUGIN_CLASS)) {
                final String clazz = node.getProperty(PLUGIN_CLASS).getString();
                log.info("Configuring ExternalService for class: {}", clazz);
                try {
                    final Class<?> processorClass = Class.forName(clazz);
                    final Constructor constructor = processorClass.getConstructor(ResourceInvocationType.class);
                    final ExternalService service = (ExternalService) constructor.newInstance(ResourceInvocationType.CMS);
                    service.configure(node);
                    oldServices.put(clazz, service);
                } catch (Exception e) {
                    log.error("Error configuring class: " + clazz, e);
                }
            } else {
                log.error("Skipping configuration for: {}, no plugin.class defined", node.getPath());
            }
        }
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {

    }

    @Override
    protected void doShutdown() {

    }
}
