package org.onehippo.forge.externalresource.folder;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.FolderWorkflowPlugin;
import org.onehippo.forge.externalresource.VideobankPlugin;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.folder.browser.MediaMosaImportDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @version $Id$
 */
public class ExternalResourceWorkflowFolderPlugin extends FolderWorkflowPlugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExternalResourceWorkflowFolderPlugin.class);

     protected VideobankPlugin getVideoService() {
        IPluginContext context = getPluginContext();
        VideobankPlugin service = context.getService(getPluginConfig().getString("video.processor.id",
                "video.processor.service"), VideobankPlugin.class);
        if (service != null) {
            return service;
        }
        return null;
    }


    public ExternalResourceWorkflowFolderPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new WorkflowAction("import", new StringResourceModel("import-title", this, null)) {

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "import-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                JcrNodeModel nodeModel = null;
                try {
                    WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) ExternalResourceWorkflowFolderPlugin.this.getDefaultModel();
                    Node node = workflowDescriptorModel.getNode();
                    nodeModel = new JcrNodeModel(node);
                } catch (RepositoryException e) {
                    log.error("", e);
                }

                HippoMediaMosaResourceManager manager = (HippoMediaMosaResourceManager) getVideoService().getResourceProcessor("hippomediamosa:resource");
                return new MediaMosaImportDialog(nodeModel, context, config);
                //return new RenameDocumentDialog(this, new StringResourceModel("import-title", ExternalResourceWorkflowFolderPlugin.this, null));
            }

           /* @Override
            protected void execute(WorkflowDescriptorModel model) throws Exception {
                // FIXME: this assumes that folders are always embedded in other folders
                // and there is some logic here to look up the parent.  The real solution is
                // in the visual component to merge two workflows.
                HippoNode node = (HippoNode) model.getNode();
                String nodeName = getNodeNameCodec().encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                FolderWorkflow folderWorkflow = (FolderWorkflow) manager.getWorkflow("embedded", node.getParent());
                if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                    folderWorkflow.rename(node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : ""), nodeName);
                }
                if (!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
            }*/
        });
    }


}
