package org.onehippo.forge.externalresource.api.scheduler.synchronization;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.HippoExtConst;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.rmi.RemoteException;

/**
 * @version $Id$
 */
public class SynchronizationJob implements RepositoryJob {
    private static final Logger log = LoggerFactory.getLogger(SynchronizationJob.class);
    public static final String IDENTIFIER_ATTRIBUTE = "identifier";

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {
        Session session = null;
        try {
            String uuid = context.getAttribute(IDENTIFIER_ATTRIBUTE);
            log.debug("External resources synchronizing {}", uuid);
            session = context.createSystemSession();
            Node node = ((HippoNode) session.getNodeByIdentifier(uuid)).getCanonicalNode();
            // TODO  remove mediamosa const.
            Synchronizable synchronizable = HippoServiceRegistry.getService(Synchronizable.class, HippoExtConst.HIPPO_MEDIAMOSA_ID + HippoExtConst.SYNCHRONIZABLE);
            SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) ((HippoWorkspace) session.getWorkspace()).getWorkflowManager().getWorkflow("synchronization", node);
            SynchronizationState state = workflow.check(synchronizable);

            switch (state) {
                case UNSYNCHRONIZED:
                    workflow.update(synchronizable);
                    break;
                case BROKEN:
                    //workflow.delete(resourceManager);
                    break;
                default:
                    break;
            }
        } catch (RepositoryException | RemoteException | WorkflowException e) {
            if (log.isDebugEnabled()) {
                log.error("External resources synchronization job failed", e);
            } else {
                log.error("External resources synchronization job failed");
            }
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }
}
