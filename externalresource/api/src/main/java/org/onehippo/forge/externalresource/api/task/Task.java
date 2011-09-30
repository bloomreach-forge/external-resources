package org.onehippo.forge.externalresource.api.task;

import org.onehippo.forge.externalresource.api.utils.WorkaroundModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * @version $Id$
 */
abstract class ResourceTask {

    private long timeout;
    private long interval;
    private long start = System.currentTimeMillis();
    private long current = System.currentTimeMillis();
    private boolean alive = true;
    private Session session;

    private static Logger log = LoggerFactory.getLogger(ResourceTask.class);

    public ResourceTask(long timeout, long interval) {
        this.session = WorkaroundModule.getSession();
        this.timeout = timeout;
        this.interval = interval;
        try {
            while (current < (start + timeout) && alive) {
                Thread.sleep(interval);
                execute();
                current = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            log.error("interrupted cycle", e);
        }
    }

    abstract protected void execute();

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public void Interrupt() {
        setAlive(false);
    }

    public Session getSession() {
        return session;
    }
}
