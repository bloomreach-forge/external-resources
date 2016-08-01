package org.onehippo.forge.externalresource.api.scheduler.synchronization;

/**
 * @version $Id$
 */
public interface SynchronizationListener {

    void onFinished();

    void onFailed();

}
