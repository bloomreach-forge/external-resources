package org.onehippo.forge.externalresource.hst.beans;

import org.hippoecm.hst.content.beans.Node;
import org.onehippo.forge.externalresource.HippoExternalNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Node(jcrType = HippoExternalNamespace.VIDEO)
public class HippoExternalVideoResourceBean extends HippoExternalResourceBean {

    private HippoExternalDisplayImageBean thumbnail;

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoExternalVideoResourceBean.class);

    public String getTitle() {
        return getProperty(HippoExternalNamespace.TITLE);
    }

    public String getDescription() {
        return getProperty(HippoExternalNamespace.DESCRIPTION);
    }

    public Long getWidth() {
        return getProperty(HippoExternalNamespace.WIDTH);
    }

    public Long getHeight() {
        return getProperty(HippoExternalNamespace.HEIGHT);
    }

    public String getDuration() {
        return getProperty(HippoExternalNamespace.DURATION);
    }

    public HippoExternalDisplayImageBean getThumbnail() {
        if (thumbnail == null) {
            // lazy within one request
            thumbnail = getBean(HippoExternalNamespace.THUMBNAIL, HippoExternalDisplayImageBean.class);
        }
        return thumbnail;
    }

    public EmbeddedBean getEmbedded() {
        return getBean(HippoExternalNamespace.JCR_TYPE);
    }

}
