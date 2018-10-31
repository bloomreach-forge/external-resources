package org.onehippo.forge.externalresource.hst.beans;

import org.hippoecm.hst.content.beans.Node;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.HippoMediamosaNamespace;
import org.onehippo.forge.externalresource.api.Embeddable;

import javax.jcr.RepositoryException;

/**
 * @version $Id$
 */

@Node(jcrType = HippoMediamosaNamespace.RESOURCE)
public class HippoMediaMosaResourceBean extends HippoExternalVideoResourceBean {

    public String getAssetId() {
        return getProperty(HippoMediamosaNamespace.ASSETID);
    }

    public String getMediaId() {
        return getProperty(HippoMediamosaNamespace.MEDIAID);
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
