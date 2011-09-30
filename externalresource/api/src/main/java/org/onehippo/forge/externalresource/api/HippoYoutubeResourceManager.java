package org.onehippo.forge.externalresource.api;

import org.apache.velocity.VelocityContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.io.InputStream;

/**
 * @version $Id$
 */
public class HippoYoutubeResourceManager extends ResourceManager {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoYoutubeResourceManager.class);

    public HippoYoutubeResourceManager(IPluginConfig config) {
        super(config);
    }

    public HippoYoutubeResourceManager() {
    }


    @Override
    public void create(Node node, InputStream istream, String mimetype) throws Exception {
    }

    @Override
    public void afterSave(Node node) {
    }

    @Override
    public void delete(Node node) {
    }



    public String getVelocityTemplateString() {
        return "youtube.vm";
    }


    public VelocityContext getVelocityContext() {
        final VelocityContext context = new VelocityContext();

        context.put("title", "title");

        return context;
    }


    public String getEmbed(Node node) {
        return null;
    }
}
