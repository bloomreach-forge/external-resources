package org.onehippo.forge.externalresource.api.scheduler;

import org.quartz.Scheduler;
import org.quartz.core.QuartzScheduler;
import org.quartz.impl.StdScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * @version $Id: ExternalResourceScheduler.java 310 2015-02-24 13:46:19Z gilgamesh $
 */
public class ExternalResourceScheduler extends StdScheduler implements Scheduler {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExternalResourceScheduler.class);

    private QuartzScheduler qs;


    /**
     * <p>
     * Construct a <code>StdScheduler</code> instance to proxy the given
     * <code>QuartzScheduler</code> instance, and with the given <code>SchedulingContext</code>.
     * </p>
     */
    public ExternalResourceScheduler(QuartzScheduler sched) {
        super(sched);
        this.qs = sched;

    }


    public ExternalResourceScheduler(ExternalResourceScheduler sched) {
        this(sched.qs);
    }
}
