package org.onehippo.forge.externalresource.api.scheduler.synchronization;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.Date;

/**
 * @version $Id$
 */
public class SynchronizationExecutorJob implements RepositoryJob {
public static class AutoClosableSession implements AutoCloseable, Supplier<Session> {
    private final Session session;

    public AutoClosableSession(Session session) {
        this.session = Preconditions.checkNotNull(session);
    }

    @Override
    public void close() throws Exception {
        session.logout();
    }

    @Override
    public Session get() {
        return session;
    }
}
    @Override
    public void execute(RepositoryJobExecutionContext context) {
        try {
            Session session = context.createSystemSession();
            try {
                RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);


                NodeIterator it = session.getWorkspace()
                        .getQueryManager()
                        .createQuery("content/videos//element(*,hippoexternal:synchronizable)", Query.XPATH)
                        .execute()
                        .getNodes();

                while (it.hasNext()) {
                    Node node = it.nextNode();
                    RepositoryJobInfo jobInfo = new RepositoryJobInfo(node.getIdentifier(), HippoMediaMosaResourceManager.MASS_SYNC_JOB_GROUP, SynchronizationJob.class);
                    jobInfo.setAttribute("identifier", node.getIdentifier());
                    RepositoryJobSimpleTrigger now = new RepositoryJobSimpleTrigger("now", new Date());
                    repositoryScheduler.scheduleJob(jobInfo, now);
                }
                //TODO call listener
//            if(jobDataMap.containsKey("listener")){
//                SynchronizationListener listener = (SynchronizationListener) jobDataMap.get("listener");
//                listener.onFinished();
//            }
            } finally {
                session.logout();
            }
        } catch (RepositoryException e) {
        }

    }

}
