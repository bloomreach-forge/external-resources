package org.onehippo.forge.externalresource.folder.browser;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.onehippo.forge.externalresource.VideobankPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

/**
 * @version $Id$
 */
abstract class ExternalResourceDialog<T> extends AbstractDialog<T> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExternalResourceDialog.class);

    protected IPluginConfig config;
    protected IPluginContext context;

    protected ExternalResourceDialog(IModel<T> tiModel) {
        super(tiModel);
    }

    public ExternalResourceDialog(IModel<T> model, IPluginContext context, IPluginConfig config) {
        super(model);
        this.context = context;
        this.config = config;
    }

     /**
     * Use the IBrowseService to select the node referenced by parameter path
     *
     * @param nodeModel Absolute path of node to browse to
     * @throws javax.jcr.RepositoryException
     */
    protected void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        ((UserSession) org.apache.wicket.Session.get()).getJcrSession().refresh(false);

        getContext().getService(getConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class)
                .browse(nodeModel);
    }

    protected VideobankPlugin getVideoService() {
        IPluginContext context = getContext();
        VideobankPlugin service = context.getService(getConfig().getString("video.processor.id",
                "video.processor.service"), VideobankPlugin.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    public IPluginConfig getConfig() {
        return config;
    }

    public void setConfig(IPluginConfig config) {
        this.config = config;
    }

    public IPluginContext getContext() {
        return context;
    }

    public void setContext(IPluginContext context) {
        this.context = context;
    }
}
