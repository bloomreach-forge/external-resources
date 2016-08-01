package org.onehippo.forge.externalresource.frontend.plugins.synchronize.actions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.MediaMosaServices;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class SynchronizedActionsWorkflowPlugin extends RenderPlugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(SynchronizedActionsWorkflowPlugin.class);

    StdWorkflow updateAction;
    StdWorkflow commitAction;


    public SynchronizedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(updateAction = new StdWorkflow("update", new StringResourceModel("update-label", this, null), getModel()) {
                    @Override
                    protected ResourceReference getIcon() {
                        return new PackageResourceReference(getClass(), "update-16.png");
                    }

                    @Override
                    protected String execute(Workflow wf) throws Exception {
                        SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;

                        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                        WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                        if (workflowDescriptor != null) {
                            Node documentNode = workflowDescriptorModel.getNode();
                            String type = documentNode.getPrimaryNodeType().getName();
                            Synchronizable sync =  MediaMosaServices.forType(type).getSynchronizable();
                            //sync.update(documentNode);
                            workflow.update(sync);
                        }
                        return null;
                    }
                }

        );

        add(commitAction = new StdWorkflow("commit", new StringResourceModel("commit-label", this, null), getModel()) {
                    @Override
                    protected ResourceReference getIcon
                            () {
                        return new PackageResourceReference(getClass(), "commit-16.png");
                    }

                    @Override
                    protected String execute
                            (Workflow wf) throws Exception {
                        SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;

                        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                        WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                        if (workflowDescriptor != null) {
                            Node documentNode = workflowDescriptorModel.getNode();
                            Synchronizable sync = MediaMosaServices.forNode(documentNode).getSynchronizable();
                            //sync.commit(documentNode);
                            workflow.commit(sync);
                        }

                        return null;
                    }
                }

        );
    }

    @Override
    public WorkflowDescriptorModel getModel() {
        return (WorkflowDescriptorModel) getDefaultModel();
    }
    @Override
    protected void onModelChanged
            () {
        super.onModelChanged();
        try {
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
            if (workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
            }
        } catch (RepositoryException | WorkflowException | RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }


}
