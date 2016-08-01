package org.onehippo.forge.externalresource.api.workflow;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.onehippo.forge.externalresource.api.ResourceHandler;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;

/**
 * @version $Id$
 */
public interface SynchronizedActionsWorkflow extends DefaultWorkflow, EditableWorkflow {

    SynchronizationState check(Synchronizable synchronizable)
            throws WorkflowException, RepositoryException, RemoteException;

    Document update(Synchronizable synchronizable)
            throws WorkflowException, RepositoryException, RemoteException;

    Document commit(Synchronizable synchronizable)
            throws WorkflowException, RepositoryException, RemoteException;

    void delete(ResourceHandler manager)
            throws WorkflowException, RepositoryException, RemoteException;
}