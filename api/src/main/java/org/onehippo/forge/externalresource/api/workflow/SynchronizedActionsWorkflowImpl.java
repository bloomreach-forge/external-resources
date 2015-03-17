package org.onehippo.forge.externalresource.api.workflow;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflowImpl;
import org.onehippo.forge.externalresource.api.ResourceHandler;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;

/**
 * @version $Id$
 */
public class SynchronizedActionsWorkflowImpl extends DefaultWorkflowImpl implements SynchronizedActionsWorkflow {

    protected String state;

    protected Document document;
    protected Session rootSession;

    public SynchronizedActionsWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RepositoryException {
        super(context, userSession, rootSession, subject);
        this.document = new Document(subject.getIdentifier());
        this.rootSession = rootSession;
    }


    public SynchronizationState check(Synchronizable synchronizable) throws WorkflowException, RepositoryException, RemoteException {
        SynchronizationState synchronizationState = synchronizable.check(this.rootSession.getNodeByIdentifier(document.getIdentity()));
        this.state = synchronizationState.getState();
        return synchronizationState;
    }

    public Document update(Synchronizable synchronizable) throws WorkflowException, RepositoryException, RemoteException {
        synchronizable.update(this.rootSession.getNodeByIdentifier(document.getIdentity()));
        return document;
    }

    public Document commit(Synchronizable synchronizable) throws WorkflowException, RepositoryException, RemoteException {
        synchronizable.commit(this.rootSession.getNodeByIdentifier(document.getIdentity()));
        return document;
    }

    public void delete(ResourceHandler resourceManager) throws WorkflowException, RepositoryException, RemoteException {
        super.delete();
        resourceManager.delete(this.rootSession.getNodeByIdentifier(document.getIdentity()));
    }
}
