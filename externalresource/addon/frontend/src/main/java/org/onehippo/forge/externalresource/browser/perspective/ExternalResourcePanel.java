package org.onehippo.forge.externalresource.browser.perspective;

import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class ExternalResourcePanel extends Panel {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExternalResourcePanel.class);

    public ExternalResourcePanel(String id, IPluginConfig config, IPluginContext context) {
        super(id);


    }

    public void renderHead(IHeaderResponse response) {
    }
}
