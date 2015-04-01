package org.onehippo.forge.externalresource.api;

import java.io.InputStream;

import javax.jcr.Node;

import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class HippoYoutubeResourceManager extends ResourceManager implements ExternalService {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoYoutubeResourceManager.class);

    public HippoYoutubeResourceManager(ResourceInvocationType type) {
        super(type);
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

    @Override
    public void configure(final Node node) {

    }

    @Override
    public void close() {
    }
}
