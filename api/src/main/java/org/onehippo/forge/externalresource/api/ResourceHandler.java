package org.onehippo.forge.externalresource.api;

import java.io.InputStream;

import javax.jcr.Node;

public interface ResourceHandler {

    void create(Node node, InputStream istream, String mimetype) throws ResourceManagerException;

    void afterSave(Node node);

    void delete(Node node);
}
