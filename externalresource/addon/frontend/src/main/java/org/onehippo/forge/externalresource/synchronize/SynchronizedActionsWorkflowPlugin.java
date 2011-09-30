package org.onehippo.forge.externalresource.synchronize;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.externalresource.VideobankPlugin;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.synchronize.workflow.SynchronizedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * @version $Id$
 */
public class SynchronizedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(SynchronizedActionsWorkflowPlugin.class);

    WorkflowAction updateAction;
    WorkflowAction commitAction;

    protected VideobankPlugin getVideoService() {
        IPluginContext context = getPluginContext();
        VideobankPlugin service = context.getService(getPluginConfig().getString("video.processor.id",
                "video.processor.service"), VideobankPlugin.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    public SynchronizedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(updateAction = new WorkflowAction("update", new StringResourceModel("update-label", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "update-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;

                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                if (workflowDescriptor != null) {
                    Node documentNode = workflowDescriptorModel.getNode();
                    String type = documentNode.getPrimaryNodeType().getName();
                    Synchronizable sync = getVideoService().getSynchronizableProcessor(type);
                    sync.update(documentNode);
                }
                workflow.update();
                return null;
            }
        }

        );

        add(commitAction = new WorkflowAction("commit", new StringResourceModel("commit-label", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon
                    () {
                return new ResourceReference(getClass(), "commit-16.png");
            }

            @Override
            protected String execute
                    (Workflow wf) throws Exception {
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;

                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                if (workflowDescriptor != null) {
                    Node documentNode = workflowDescriptorModel.getNode();
                    String type = documentNode.getPrimaryNodeType().getName();
                    Synchronizable sync = getVideoService().getSynchronizableProcessor(type);
                    sync.commit(documentNode);
                }

                workflow.commit();
                return null;
            }
        }

        );
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
                //Synchronizable sync = getVideoService().getSynchronizableProcessor(documentNode.getPrimaryNodeType().getName());
                //sync.check(documentNode);
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (WorkflowException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }


}
