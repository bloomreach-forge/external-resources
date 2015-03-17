package org.onehippo.forge.externalresource.api;

import javax.jcr.Node;

import org.onehippo.forge.externalresource.api.utils.SynchronizationState;

/**
 * @version $Id$
 */
//@SingletonService
public interface Synchronizable {

    boolean update(Node node);

    boolean commit(Node node);

    SynchronizationState check(Node node);
}
