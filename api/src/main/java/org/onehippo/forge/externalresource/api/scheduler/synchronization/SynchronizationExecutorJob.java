package org.onehippo.forge.externalresource.api.scheduler.synchronization;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizationExecutorJob.class);

    @Override
    public void execute(RepositoryJobExecutionContext context) {
        Session session = null;
        try {
            session = context.createSystemSession();
            RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

            NodeIterator it = session.getWorkspace()
                    .getQueryManager()
                    .createQuery("content/videos//element(*,hippoexternal:synchronizable)", Query.XPATH)
                    .execute()
                    .getNodes();

            while (it.hasNext()) {
                Node node = it.nextNode();
                RepositoryJobInfo jobInfo = new RepositoryJobInfo(node.getIdentifier(), HippoMediaMosaResourceManager.MASS_SYNC_JOB_GROUP, SynchronizationJob.class);
                jobInfo.setAttribute(SynchronizationJob.IDENTIFIER_ATTRIBUTE, node.getIdentifier());
                RepositoryJobSimpleTrigger trigger = new RepositoryJobSimpleTrigger("now", new Date());
                repositoryScheduler.scheduleJob(jobInfo, trigger);
            }
            //TODO call listener for SynchronizationListPanel
//            if(jobDataMap.containsKey("listener")){
//                SynchronizationListener listener = (SynchronizationListener) jobDataMap.get("listener");
//                listener.onFinished();
//            }
        } catch (RepositoryException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("External resources cannot find hippoexternal:synchronizable nodes", e);
            } else {
                LOG.error("External resources cannot find hippoexternal:synchronizable nodes");
            }
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }
}
