package org.onehippo.forge.externalresource.hst.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.onehippo.forge.externalresource.HippoExternalNamespace;

@Node(jcrType = HippoExternalNamespace.JCR_TYPE)
public class EmbeddedBean extends HippoDocument {

    public String getEmbedded() {
        return getProperty(HippoExternalNamespace.EMBEDDED);
    }
}
