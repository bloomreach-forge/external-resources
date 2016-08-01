package org.onehippo.forge.externalresource.api;

import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.io.InputStream;

/**
 * @version $Id$
 */
abstract public class ResourceManager implements ResourceHandler {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ResourceManager.class);

    private final ResourceInvocationType type;

    public ResourceManager(final ResourceInvocationType type) {
        this.type = type;
    }


    protected RepositoryScheduler getRepositoryScheduler() {
        return HippoServiceRegistry.getService(RepositoryScheduler.class);
    }

    public ResourceInvocationType getType() {
        return type;
    }
}
