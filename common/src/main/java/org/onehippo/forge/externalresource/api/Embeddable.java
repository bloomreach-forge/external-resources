package org.onehippo.forge.externalresource.api;

import javax.jcr.Node;

/**
 * @version $Id: Embeddable.java 38 2011-11-18 23:09:24Z ksalic $
 */
public interface Embeddable {

    public String getEmbedded(Node node);
}
