package org.onehippo.forge.externalresource.reports.plugins.synchronization;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.reports.plugins.ReportPanel;
import org.onehippo.cms7.reports.plugins.ReportUtil;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.onehippo.forge.externalresource.reports.plugins.synchronization.column.SynchronizationListColumns;
import org.onehippo.forge.externalresource.reports.plugins.synchronization.store.SynchronizationStore;
import org.onehippo.forge.externalresource.reports.temp.IDocumentListColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * @version $Id$
 */
@ExtClass("Hippo.Reports.SynchronizationListPanel")
public class SynchronizationListPanel extends ReportPanel {

    private static final String CONFIG_COLUMNS = "columns";
    private static final String CONFIG_AUTO_EXPAND_COLUMN = "auto.expand.column";
    private static final String TITLE_TRANSLATOR_KEY = "title";
    private static final String NO_DATA_TRANSLATOR_KEY = "no-data";

    private final Logger log = LoggerFactory.getLogger(SynchronizationListPanel.class);
    private final IPluginContext context;
    private final int pageSize;
    private final SynchronizationListColumns columns;
    // private final BrokenLinksListColumns columns;
    private final ExtJsonStore<Object> store;
    private final ExternalResourceService externalService;

    private static final CssResourceReference MYCOMPONENT_CSS = new CssResourceReference(SynchronizationListPanel.class, "Hippo.Reports.SynchronizationListPanel.css");
    private static final JavaScriptResourceReference MYCOMPONENT_JS = new JavaScriptResourceReference(SynchronizationListPanel.class, "Hippo.Reports.SynchronizationListPanel.js");

    public SynchronizationListPanel(final IPluginContext context, final IPluginConfig config, final String query) {
        super(context, config);
        this.context = context;
        pageSize = config.getInt("page.size", 10);
        columns = new SynchronizationListColumns(config.getStringArray(CONFIG_COLUMNS));
        externalService = context.getService(config.getString("external.processor.id",
                "external.processor.service"), ExternalResourceService.class);
        store = new SynchronizationStore(query, columns, pageSize, externalService);

        add(store);


        addEventListener("documentSelected", new ExtEventListener() {

            public void onEvent(final AjaxRequestTarget ajaxRequestTarget, Map<String, JSONArray> parameters) {
                Request request = RequestCycle.get().getRequest();
                String path = null;
                try {
                    path = parameters.get("path").getString(0);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                browseToDocument(path);
            }
        });

        addEventListener("performSyncAction", new ExtEventListener() {
            public void onEvent(final AjaxRequestTarget ajaxRequestTarget, Map<String, JSONArray> parameters) {
                try {
                    final Request request = RequestCycle.get().getRequest();
                    final String path = parameters.get("path").getString(0);
                    final String mediumName = parameters.get("medium").getString(0);
                    Node node = getNode(path);
                    try {
                        SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager().getWorkflow("synchronization", node);
                        ResourceManager manager = externalService.getResourceProcessor(node.getPrimaryNodeType().getName());
                        Synchronizable synchronizable = externalService.getSynchronizableProcessor(node.getPrimaryNodeType().getName());
                        if (mediumName.equals("synchronize")) {
                            workflow.update(synchronizable);
                        } else if (mediumName.equals("check")) {
                            workflow.check(synchronizable);
                        } else if (mediumName.equals("delete")) {
                            workflow.delete(manager);
                        }
                    } catch (RepositoryException e) {
                        log.error("", e);
                    } catch (RemoteException e) {
                        log.error("", e);
                    } catch (WorkflowException e) {
                        log.error("", e);
                    }
                    ajaxRequestTarget.appendJavaScript("if (typeof Hippo.Reports.RefreshObservableInstance !== 'undefined') { Hippo.Reports.RefreshObservableInstance.fireEvent('refresh'); }");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });


        /*addEventListener("performMassSync", new ExtEventListener() {
            public void onEvent(final AjaxRequestTarget ajaxRequestTarget, Map<String, JSONArray> parameters) {
                ResourceManager manager = externalService.getResourceProcessor("hippomediamosa:resource");
                JobDataMap dataMap = new JobDataMap();
                dataMap.put("resourcemanager", manager);
                dataMap.put("synchronizable", manager);
                SynchronizationListener listener = new SynchronizationListener() {
                    public void onFinished() {
                        System.out.println("finished");
                        ajaxRequestTarget.appendJavascript("if (typeof Hippo.Reports.RefreshObservableInstance !== 'undefined') { Hippo.Reports.RefreshObservableInstance.fireEvent('refresh'); }");
                    }

                    public void onFailed() {
                    }
                };
                dataMap.put("listener", listener);
                manager.scheduleNowOnce(SynchronizationExecutorJob.class, dataMap);
            }
        });*/
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(MYCOMPONENT_CSS));
        response.render(JavaScriptHeaderItem.forReference(MYCOMPONENT_JS));

    }

    private Node getNode(String path) {
        try {
            Session session = ((UserSession) getSession()).getJcrSession();
            return session.getNode(path);
        } catch (RepositoryException e) {
            log.warn("Unable to get the node " + path, e);
        }
        return null;
    }

    private void browseToDocument(String path) {
        if (path == null || path.length() == 0) {
            log.warn("No document path to browse to");
            return;
        }

        final Node node = getNode(path);
        if (node == null) {
            return;
        }

        JcrNodeModel nodeModel = new JcrNodeModel(node);
        IBrowseService browseService = context.getService("service.browse", IBrowseService.class);
        browseService.browse(nodeModel);
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        properties.put("columns", getColumnsConfig());
        properties.put("store", new JSONIdentifier(store.getJsObjectId()));
        properties.put("pageSize", this.pageSize);
        properties.put("paging", config.getAsBoolean("paging", true));
        properties.put("noDataText", ReportUtil.getTranslation(this, NO_DATA_TRANSLATOR_KEY, StringUtils.EMPTY));

        if (config.containsKey(CONFIG_AUTO_EXPAND_COLUMN)) {
            String autoExpandColumn = config.getString(CONFIG_AUTO_EXPAND_COLUMN);

            if (!columns.containsColumn(autoExpandColumn)) {
                // prevent an auto-expand column that is not an actual column name, otherwise ExtJs stops rendering
                log.warn("Ignoring unknown auto-expand column '{}'", autoExpandColumn);
            } else {
                properties.put("autoExpandColumn", autoExpandColumn);
            }
        }

        final String title = ReportUtil.getTranslation(this, TITLE_TRANSLATOR_KEY, StringUtils.EMPTY);
        if (StringUtils.isNotEmpty(title)) {
            properties.put("title", title);
        }

        if (columns.containsColumn("syncActions")) {
            final RequestCycle rc = RequestCycle.get();
            final JSONArray syncActions = new JSONArray();

            JSONObject updateAction = new JSONObject();
            updateAction.put("name", "synchronize");
            updateAction.put("icon", rc.urlFor((IRequestHandler) new PackageResourceReference(SynchronizationListPanel.class, "update-16.png")));
            syncActions.put(updateAction);

            JSONObject deleteAction = new JSONObject();
            deleteAction.put("name", "delete");
            deleteAction.put("icon", rc.urlFor((IRequestHandler) new PackageResourceReference(SynchronizationListPanel.class, "delete-16.png")));
            syncActions.put(deleteAction);

            JSONObject checkAction = new JSONObject();
            checkAction.put("name", "check");
            checkAction.put("icon", rc.urlFor((IRequestHandler) new PackageResourceReference(SynchronizationListPanel.class, "refresh-icon.png")));
            syncActions.put(checkAction);

            properties.put("syncActions", syncActions);
        }
    }

    private JSONArray getColumnsConfig() throws JSONException {
        JSONArray result = new JSONArray();

        for (IDocumentListColumn column : columns.getAllColumns()) {
            JSONObject config = column.getExtColumnConfig();
            if (config != null) {
                result.put(config);
            }
        }

        return result;
    }

}
