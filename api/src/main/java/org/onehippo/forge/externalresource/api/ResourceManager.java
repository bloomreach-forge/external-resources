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
abstract public class ResourceManager extends Plugin {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ResourceManager.class);

    protected String type;

    public ResourceManager(IPluginConfig config, ResourceInvocationType invocationType) {
        super(null, config);
        if (config.containsKey("type")) {
            this.type = config.getString("type");
        }
        switch (invocationType) {
            case CMS:
                initCmsPlugin();
                break;
            case SITE:
                initSitePlugin();
                break;
            default:
                break;
        }
    }

    public void initSitePlugin() {
    }

    public void initCmsPlugin() {
    }

    abstract public void create(Node node, InputStream istream, String mimetype) throws Exception;

    abstract public void afterSave(Node node);

    abstract public void delete(Node node);

    protected RepositoryScheduler getRepositoryScheduler() {
        return HippoServiceRegistry.getService(RepositoryScheduler.class);
    }
}
