package org.onehippo.forge.externalresource.synchronize.workflow;

import org.hippoecm.repository.api.*;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.forge.externalresource.api.Synchronizable;

import javax.jcr.RepositoryException;
import javax.jdo.annotations.*;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Locale;


/**
 * @version $Id$
 */
@PersistenceCapable(identityType = IdentityType.DATASTORE, cacheable = "true", detachable = "false", table = "documents")
@DatastoreIdentity(strategy = IdGeneratorStrategy.NATIVE)
@Discriminator(strategy = DiscriminatorStrategy.CLASS_NAME)
public class SynchronizedActionsWorkflowImpl extends WorkflowImpl implements SynchronizedActionsWorkflow {
    @SuppressWarnings("unused")

    private static final long serialVersionUID = 1L;

    @Persistent(column = "hippoexternal:lastModifiedSyncDate")
    protected Date lastModifiedSyncDate;

    @Persistent(column = "hippoexternal:state")
    protected String state;

    @Persistent(column = "jcr:primaryType")
    protected String type;

    @Persistent(column = ".")
    protected SynchronizableDocument synchronizableDocument;

    private Synchronizable synchronizable;

    public Synchronizable getSynchronizable() {
        return synchronizable;
    }

    public void setSynchronizable(Synchronizable synchronizable) {
        this.synchronizable = synchronizable;
    }

    /**
     * @throws java.rmi.RemoteException
     */
    public SynchronizedActionsWorkflowImpl() throws RemoteException {
        super();
    }

    public Document obtainEditableInstance() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        check();
        return synchronizableDocument;
    }

    public Document commitEditableInstance() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        commit();
        return synchronizableDocument;
    }

    public Document disposeEditableInstance() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }
              /*todo rename this*/
    public Document check() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        return null;
    }

    public void update() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void commit() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void delete() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void archive() throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void rename(String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void localizeName(Localized locale, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void localizeName(Locale locale, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void localizeName(String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }

    public void move(Document target, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document folder = getWorkflowContext().getDocument("embedded", synchronizableDocument.getIdentity());
        Workflow workflow = getWorkflowContext().getWorkflow(getFolderWorkflowCategory(), folder);
        if (workflow instanceof FolderWorkflow)
            ((FolderWorkflow) workflow).move(synchronizableDocument, target, newName);
        else
            throw new WorkflowException("cannot move document which is not contained in a folder");
    }

    private String getFolderWorkflowCategory() {
        String folderWorkflowCategory = "internal";
        RepositoryMap config = getWorkflowContext().getWorkflowConfiguration();
        if (config != null && config.exists() && config.get("folder-workflow-category") instanceof String) {
            folderWorkflowCategory = (String) config.get("folder-workflow-category");
        }
        return folderWorkflowCategory;
    }


    public void copy(Document target, String newName) throws WorkflowException, MappingException, RepositoryException, RemoteException {
    }


}

