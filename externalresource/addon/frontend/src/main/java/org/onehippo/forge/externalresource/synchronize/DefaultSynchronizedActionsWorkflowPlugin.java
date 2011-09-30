package org.onehippo.forge.externalresource.synchronize;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.*;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.workflow.CopyNameHelper;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.*;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
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
public class DefaultSynchronizedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(DefaultSynchronizedActionsWorkflowPlugin.class);

    private String synchState = "";
    StdWorkflow infoAction;
    WorkflowAction editAction;
    WorkflowAction deleteAction;
    WorkflowAction renameAction;
    WorkflowAction copyAction;
    WorkflowAction moveAction;
    WorkflowAction whereUsedAction;

    protected VideobankPlugin getVideoService() {
        IPluginContext context = getPluginContext();
        VideobankPlugin service = context.getService(getPluginConfig().getString("video.processor.id",
                "video.processor.service"), VideobankPlugin.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    public DefaultSynchronizedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
           //System.out.println("init default, checking stuff" + getDefaultModel());
        /*try {
          *//*  Node docNode = ((WorkflowDescriptorModel) DefaultSynchronizedActionsWorkflowPlugin.this.getDefaultModel())
                    .getNode();
            Synchronizable sync = getVideoService().getSynchronizableProcessor(docNode.getPrimaryNodeType().getName());
            sync.check(docNode);
            System.out.println("init default, checking stuff");
            docNode.getSession().save();*//*
        } catch (RepositoryException e) {
            log.error("", e);
        }*/

        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel("hippoexternal:synchronizable"));
        add(infoAction = new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                try {
                    System.out.println("gettitle");
                    //WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                   // WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                    //System.out.println(workflowDescriptor);

                    //getting node synchronize actions commiting
                    Node docNode = ((WorkflowDescriptorModel) DefaultSynchronizedActionsWorkflowPlugin.this.getDefaultModel())
                            .getNode();
                    Synchronizable sync = getVideoService().getSynchronizableProcessor(docNode.getPrimaryNodeType().getName());
                    System.out.println(docNode.getPrimaryNodeType().getName());
                    synchState = sync.check(docNode).getStringValue();

                   // docNode.getSession().save();
                    System.out.println("wihtout save"+synchState);

                    /*if (workflowDescriptor != null) {
                        Node documentNode = workflowDescriptorModel.getNode();
                        Synchronizable sync = getVideoService().getSynchronizableProcessor(documentNode.getPrimaryNodeType().getName());
                        sync.check(documentNode);
                    }*/
                } catch (RepositoryException e) {
                    log.error("", e);
                }
                return translator.getValueName("hippoexternal:state", new PropertyModel(
                        DefaultSynchronizedActionsWorkflowPlugin.this, "synchState"));
            }

            @Override
            protected String execute(Workflow workflow) throws Exception {
                try {
                    WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
                    WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
                    if (workflowDescriptor != null) {
                        Node documentNode = workflowDescriptorModel.getNode();
                        Synchronizable sync = getVideoService().getSynchronizableProcessor(documentNode.getPrimaryNodeType().getName());
                        sync.check(documentNode);
                    }
                } catch (RepositoryException e) {
                    log.error("", e);
                }
                return null;
            }

            @Override
            protected void invoke() {
            }
        });

        add(editAction = new WorkflowAction("edit", new StringResourceModel("edit-label", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "edit-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();

                ((UserSession) getSession()).getJcrSession().refresh(true);
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    } else {
                        editor.setMode(IEditor.Mode.EDIT);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }

                return null;
            }
        });

        add(renameAction = new WorkflowAction("rename", new StringResourceModel("rename-label", this, null)) {
            public String targetName;
            public String uriName;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "rename-16.png");
            }


            @Override
            protected String execute(Workflow wf) throws Exception {
                if (targetName == null || targetName.trim().equals("")) {
                    throw new WorkflowException("No name for destination given");
                }
                HippoNode node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                String nodeName = getNodeNameCodec().encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);
                if ("".equals(nodeName)) {
                    throw new IllegalArgumentException("You need to enter a name");
                }
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                    ((SynchronizedActionsWorkflow) wf).rename(nodeName);
                }
                if (!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
                return null;
            }
        });

        add(copyAction = new WorkflowAction("copy", new StringResourceModel("copy-label", this, null)) {
            NodeModelWrapper destination = null;
            String name = null;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "copy-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                CopyNameHelper copyNameHelper = new CopyNameHelper(getNodeNameCodec(), new StringResourceModel(
                        "copyof", DefaultSynchronizedActionsWorkflowPlugin.this, null).getString());
                try {
                    name = copyNameHelper.getCopyName(((HippoNode) ((WorkflowDescriptorModel) getDefaultModel())
                            .getNode()).getLocalizedName(), destination.getNodeModel().getNode());
                } catch (RepositoryException ex) {
                    return new ExceptionDialog(ex);
                }
                IDialogService.Dialog dialog = new WorkflowAction.DestinationDialog(
                        new StringResourceModel("copy-title", DefaultSynchronizedActionsWorkflowPlugin.this, null),
                        new StringResourceModel("copy-name", DefaultSynchronizedActionsWorkflowPlugin.this, null),
                        new PropertyModel(this, "name"),
                        destination) {
                    {
                        setOkEnabled(true);
                    }
                };
                return dialog;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                JcrNodeModel folderModel = new JcrNodeModel("/");
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                }
                StringCodec codec = getNodeNameCodec();
                String nodeName = codec.encode(name);
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;

                workflow.copy(new Document(folderModel.getNode().getUUID()), nodeName);
                JcrNodeModel resultModel = new JcrNodeModel(folderModel.getItemModel().getPath() + "/" + nodeName);
                Node result = resultModel.getNode();

                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", result.getNode(nodeName));
                defaultWorkflow.localizeName(getLocalizeCodec().encode(name));

                browseTo(resultModel);
                return null;
            }
        });

        add(moveAction = new WorkflowAction("move", new StringResourceModel("move-label", this, null)) {
            NodeModelWrapper destination;

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "move-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                return new WorkflowAction.DestinationDialog(new StringResourceModel("move-title",
                        DefaultSynchronizedActionsWorkflowPlugin.this, null), null, null, destination);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                JcrNodeModel folderModel = new JcrNodeModel("/");
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                }
                String nodeName = (((WorkflowDescriptorModel) getDefaultModel()).getNode()).getName();
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;
                workflow.move(new Document(folderModel.getNode().getUUID()), nodeName);

                browseTo(new JcrNodeModel(folderModel.getItemModel().getPath() + "/" + nodeName));
                return null;
            }
        });

        add(deleteAction = new WorkflowAction("delete",
                new StringResourceModel("delete-label", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "delete-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message",
                        DefaultSynchronizedActionsWorkflowPlugin.this, null, new Object[]{getDocumentName()});
                IModel<String> title = new StringResourceModel("delete-title", DefaultSynchronizedActionsWorkflowPlugin.this,
                        null, new Object[]{getDocumentName()});
                return new DeleteDialog(title, message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) wf;
                workflow.delete();
                return null;
            }
        });

        add(whereUsedAction = new WorkflowAction("where-used", new StringResourceModel("where-used-label", this, null)
                .getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "where-used-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                return new WhereUsedDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

    }

    private JcrNodeModel getFolder() {
        JcrNodeModel folderModel = new JcrNodeModel("/");
        try {
            WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
            if (wdm != null) {
                HippoNode node = (HippoNode) wdm.getNode();
                if (node != null) {
                    folderModel = new JcrNodeModel(node.getParent().getParent());
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Could not determine folder path", ex);
        }
        return folderModel;
    }

    private IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        System.out.println("monitor: onmodelchange defaultsychactions");
        try {
            WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) getDefaultModelObject();
            if (workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();
                if (documentNode != null && documentNode.hasProperty("hippoexternal:state")) {
                    synchState = documentNode.getProperty("hippoexternal:state").getString();
                }
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if (!documentNode.hasProperty("hippostd:stateSummary") || (info.containsKey("obtainEditableInstance") &&
                        info.get("obtainEditableInstance") instanceof Boolean &&
                        !((Boolean) info.get("obtainEditableInstance")).booleanValue())) {
                    editAction.setVisible(true);
                }
                /* if (info.containsKey("publish") && info.get("publish") instanceof Boolean
                        && !((Boolean) info.get("publish")).booleanValue()) {

                    // publishAction.setVisible(false);
                    //schedulePublishAction.setVisible(false);
                }*/
                /* if (info.containsKey("depublish") && info.get("depublish") instanceof Boolean
                        && !((Boolean) info.get("depublish")).booleanValue()) {
                    //depublishAction.setVisible(false);
                    //scheduleDepublishAction.setVisible(false);
                }*/
                if (info.containsKey("delete") && info.get("delete") instanceof Boolean
                        && !((Boolean) info.get("delete")).booleanValue()) {
                    deleteAction.setVisible(false);
                }
                if (info.containsKey("rename") && info.get("rename") instanceof Boolean
                        && !((Boolean) info.get("rename")).booleanValue()) {
                    renameAction.setVisible(false);
                }
                if (info.containsKey("move") && info.get("move") instanceof Boolean
                        && !((Boolean) info.get("move")).booleanValue()) {
                    moveAction.setVisible(false);
                }
                if (info.containsKey("copy") && info.get("copy") instanceof Boolean
                        && !((Boolean) info.get("copy")).booleanValue()) {
                    copyAction.setVisible(false);
                }
                if (info.containsKey("status") && info.get("status") instanceof Boolean
                        && !((Boolean) info.get("status")).booleanValue()) {
                    infoAction.setVisible(false);
                    whereUsedAction.setVisible(false);
                    // historyAction.setVisible(false);
                }
                /*if (info.containsKey("inUseBy") && info.get("inUseBy") instanceof String) {
                    inUseBy = (String) info.get("inUseBy");
                    infoEditAction.setVisible(true);
                } else {
                    infoEditAction.setVisible(false);
                }
                infoEditAction.setVisible(true);*/
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (WorkflowException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Get the name of the node this workflow operates on
     *
     * @return The name of the node that the workflow operates on or an empty String if an error occurs
     * @throws RepositoryException
     */
    //    private String getInputNodeName() {
    //        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getModel();
    //        try {
    //            return new NodeTranslator(new JcrNodeModel(workflowDescriptorModel.getNode())).getNodeName().getObject().toString();
    //        } catch (RepositoryException e) {
    //            log.error("Error translating node name", e);
    //        }
    //        return "";
    //    }

    /**
     * Use the IBrowseService to select the node referenced by parameter path
     *
     * @param nodeModel Absolute path of node to browse to
     * @throws RepositoryException
     */
    private void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        ((UserSession) org.apache.wicket.Session.get()).getJcrSession().refresh(false);

        getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class)
                .browse(nodeModel);
    }

    IModel<String> getDocumentName() {
        try {
            return (new NodeTranslator(new JcrNodeModel(((WorkflowDescriptorModel) getDefaultModel()).getNode())))
                    .getNodeName();
        } catch (RepositoryException ex) {
            try {
                return new Model<String>(((WorkflowDescriptorModel) getDefaultModel()).getNode().getName());
            } catch (RepositoryException e) {
                return new StringResourceModel("unknown", this, null);
            }
        }
    }

    public class RenameDocumentDialog extends WorkflowAction.WorkflowDialog {
        private IModel title;
        private TextField nameComponent;
        private TextField uriComponent;
        private boolean uriModified;

        public RenameDocumentDialog(WorkflowAction action, IModel title) {
            action.super();
            this.title = title;

            final PropertyModel<String> nameModel = new PropertyModel<String>(action, "targetName");
            final PropertyModel<String> uriModel = new PropertyModel<String>(action, "uriName");

            String s1 = nameModel.getObject();
            String s2 = uriModel.getObject();
            uriModified = !s1.equals(s2);

            nameComponent = new TextField<String>("name", nameModel);
            nameComponent.setRequired(true);
            nameComponent.setLabel(new StringResourceModel("name-label", this, null));
            nameComponent.add(new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!uriModified) {
                        uriModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
                        target.addComponent(uriComponent);
                    }
                }
            }.setThrottleDelay(Duration.milliseconds(500)));
            nameComponent.setOutputMarkupId(true);
            setFocus(nameComponent);
            add(nameComponent);

            add(uriComponent = new TextField<String>("uriinput", uriModel) {
                @Override
                public boolean isEnabled() {
                    return uriModified;
                }
            });

            uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? "grayedin" : "grayedout";
                }
            }));
            uriComponent.setOutputMarkupId(true);

            AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    uriModified = !uriModified;
                    if (!uriModified) {
                        uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                                nameModel.getObject()));
                    } else {
                        target.focusComponent(uriComponent);
                    }
                    target.addComponent(RenameDocumentDialog.this);
                }
            };
            uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? getString("url-reset") : getString("url-edit");
                }
            }));
            add(uriAction);
        }

        @Override
        public IModel getTitle() {
            return title;
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }
    }


}
