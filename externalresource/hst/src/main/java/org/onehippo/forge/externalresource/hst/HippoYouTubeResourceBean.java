package org.onehippo.forge.externalresource.hst;

import org.hippoecm.hst.content.beans.Node;

/**
 * @version
 */
@Node(jcrType = "hippoyoutube:resource")
public class HippoYouTubeResourceBean extends HippoExternalResourceBean {

    public String getId(){
        return getProperty("hippoyoutube:id");
    }


}
