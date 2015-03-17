package org.onehippo.forge.externalresource.api;

import javax.jcr.Node;

public interface ExternalService {

    void configure(Node node);

    void unregister();
}
