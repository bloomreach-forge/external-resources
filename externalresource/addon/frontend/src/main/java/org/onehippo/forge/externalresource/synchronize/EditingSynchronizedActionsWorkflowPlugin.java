package org.onehippo.forge.externalresource.synchronize;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.ActionDescription;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.feedback.YuiFeedbackPanel;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.validation.IValidationResult;
import org.hippoecm.frontend.validation.IValidationService;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.forge.externalresource.VideobankPlugin;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.synchronize.workflow.SynchronizedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * @version $Id$
 */
public class EditingSynchronizedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(EditingSynchronizedActionsWorkflowPlugin.class);

    static class FeedbackLogger extends Component {
        private static final long serialVersionUID = 1L;

        public FeedbackLogger() {
            super("id");
        }

        @Override
        protected void onRender(MarkupStream markupStream) {

        }

    }

     protected VideobankPlugin getVideoService() {
        IPluginContext context = getPluginContext();
        VideobankPlugin service = context.getService(getPluginConfig().getString("video.processor.id",
                "video.processor.service"), VideobankPlugin.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    private Fragment feedbackContainer;
    private transient boolean closing = false;
    private boolean isValid = true;

    public EditingSynchronizedActionsWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        final CompatibilityWorkflowPlugin plugin = this;
        final IEditor editor = context.getService(config.getString("editor.id"), IEditor.class);

        add(new WorkflowAction("save", new StringResourceModel("save", this, null, "Save").getString(),
                new ResourceReference(EditingSynchronizedActionsWorkflowPlugin.class, "document-save-16.png")) {
            @Override
            protected String execute(Workflow wf) throws Exception {
                validate();
                if (!isValid()) {
                    return null;
                }
                //todo does this do anything? check 119
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;
                workflow.commitEditableInstance();

                //date for feedback todo
                DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                new FeedbackLogger().info(new StringResourceModel("saved", EditingSynchronizedActionsWorkflowPlugin.this,
                        null, new Object[]{df.format(new Date())}).getString());
                //showFeedback();

                UserSession session = (UserSession) Session.get();
                session.getJcrSession().refresh(false);



                //getting node synchronize actions commiting
                Node docNode = ((WorkflowDescriptorModel) EditingSynchronizedActionsWorkflowPlugin.this.getDefaultModel())
                        .getNode();

                Synchronizable synchronizable = getVideoService().getSynchronizableProcessor(docNode.getPrimaryNodeType().getName());
                synchronizable.commit(docNode);

                  // get new instance of the workflow, previous one may have invalidated itself
                EditingSynchronizedActionsWorkflowPlugin.this.getDefaultModel().detach();
                WorkflowDescriptor descriptor = (WorkflowDescriptor) (EditingSynchronizedActionsWorkflowPlugin.this
                        .getDefaultModel().getObject());
                session.getJcrSession().refresh(true);
                WorkflowManager manager = session.getWorkflowManager();
                workflow = (SynchronizedActionsWorkflow) manager.getWorkflow(descriptor);

                /* Document draft = */
                workflow.obtainEditableInstance();
                return null;
            }
        });

        add(new WorkflowAction("done", new StringResourceModel("done", this, null, "Done").getString(),
                new ResourceReference(EditingSynchronizedActionsWorkflowPlugin.class, "document-saveclose-16.png")) {
            @Override
            public String execute(Workflow wf) throws Exception {

                //validates the form
                validate();
                if (!isValid()) {
                    return null;
                }

                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;
                workflow.commitEditableInstance();
                ((UserSession) Session.get()).getJcrSession().refresh(true);

                //getting the node
                Node docNode = ((WorkflowDescriptorModel) EditingSynchronizedActionsWorkflowPlugin.this.getDefaultModel())
                        .getNode();

                //handles the mode change
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    } else {
                        editor.setMode(IEditor.Mode.VIEW);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }

                //synchronizes the document with the videoservice
                Synchronizable synchronizable = getVideoService().getSynchronizableProcessor(docNode.getPrimaryNodeType().getName());
                synchronizable.commit(docNode);

                return null;
            }
        });

        //Feedback fb = new Feedback();
        //feedbackContainer = (Fragment) fb.getFragment("text");
        //add(fb);
    }


    protected void showFeedback() {
        YuiFeedbackPanel yfp = (YuiFeedbackPanel) feedbackContainer.get("feedback");
        yfp.render(AjaxRequestTarget.get());
    }

    void validate() throws ValidationException {
        isValid = true;
        List<IValidationService> validators = getPluginContext().getServices(
                getPluginConfig().getString(IValidationService.VALIDATE_ID), IValidationService.class);
        if (validators != null) {
            for (IValidationService validator : validators) {
                validator.validate();
                IValidationResult result = validator.getValidationResult();
                isValid = isValid && result.isValid();
            }
        }
    }

    boolean isValid() {
        return isValid;
    }

    class Feedback extends ActionDescription {
        private static final long serialVersionUID = 1L;

        public Feedback() {
            super("info");

            Fragment feedbackFragment = new Fragment("text", "feedback", EditingSynchronizedActionsWorkflowPlugin.this);
            feedbackFragment.add(new YuiFeedbackPanel("feedback", new IFeedbackMessageFilter() {
                private static final long serialVersionUID = 1L;

                public boolean accept(FeedbackMessage message) {
                    return FeedbackLogger.class.isInstance(message.getReporter());
                }
            }));
            add(feedbackFragment);
        }

        @Override
        protected void invoke() {
        }

    }
}