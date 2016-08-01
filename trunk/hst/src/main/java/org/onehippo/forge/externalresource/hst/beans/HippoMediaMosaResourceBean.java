package org.onehippo.forge.externalresource.hst.beans;

import org.hippoecm.hst.content.beans.Node;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.Embeddable;

import javax.jcr.RepositoryException;

/**
 * @version $Id$
 */

@Node(jcrType = "hippomediamosa:resource")
public class HippoMediaMosaResourceBean extends HippoExternalVideoResourceBean {

    public String getAssetId() {
        return getProperty("hippomediamosa:assetid");
    }

    public String getMediaId() {
        return getProperty("hippomediamosa:mediaid");
    }

    public String getEmbeddableVideo() throws RepositoryException {
        String nodeType = getNode().getPrimaryNodeType().getName();
        Embeddable embeddable = HippoServiceRegistry.getService(Embeddable.class, nodeType + "Embeddable");
        if (embeddable == null) {
            throw new RepositoryException("No Embeddable service registered for " + nodeType);
        }
        return  embeddable.getEmbedded(getNode());
    }


}
