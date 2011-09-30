package org.onehippo.forge.externalresource.hst;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoResource;

import java.util.Date;

/**
 * @version
 */
@Node(jcrType = "hippoexternal:resource")
public class HippoExternalResourceBean extends HippoDocument {

    public String getName() {
        return getProperty("hippoexternal:name");
    }

    public String getTitle() {
        return getProperty("hippoexternal:title");
    }

    public String getDescription() {
        return getProperty("hippoexternal:description");
    }

    public String[] getTags() {
        return getProperty("hippoexternal:tags");
    }

    public String getMimeType() {
        return getProperty("hippoexternal:mimeType");
    }

    public Date getLastModified() {
        return getProperty("hippoexternal:lastModified");
    }

    public Long getSize() {
        return getProperty("hippoexternal:size");
    }

    public Long getWidth() {
        return getProperty("hippoexternal:width");
    }

    public Long getHeight() {
        return getProperty("hippoexternal:height");
    }

    public HippoResource getPreview() {
        return getBean("hippoexternal:preview");
    }

    public HippoResource getThumbnail() {
        return getProperty("hippoexternal:thumbnail");
    }

}
