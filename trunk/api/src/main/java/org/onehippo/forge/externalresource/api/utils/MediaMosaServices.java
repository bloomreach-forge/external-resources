package org.onehippo.forge.externalresource.api.utils;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.Embeddable;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.onehippo.forge.externalresource.api.MediamosaRemoteService;
import org.onehippo.forge.externalresource.api.ResourceHandler;
import org.onehippo.forge.externalresource.api.Synchronizable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public final class MediaMosaServices {

    private static final LoadingCache<String, MediaMosaServices> MAP = CacheBuilder.newBuilder().build(CacheLoader.from(new Function<String, MediaMosaServices>() {
        @Override
        public MediaMosaServices apply(String input) {
            return new MediaMosaServices(input);
        }
    }));

    public static MediaMosaServices forType(String type) {
        return Optional.fromNullable(MAP.getIfPresent(type)).get();
    }

    public static MediaMosaServices init(String type) {
        return MAP.getUnchecked(type);
    }

    public static MediaMosaServices forNode(Node node) throws RepositoryException {
        return forType(node.getPrimaryNodeType().getName());
    }

    public static MediamosaRemoteService getMediamosaRemoteService() {
        return HippoServiceRegistry.getService(MediamosaRemoteService.class);
    }

    private static final String EMBEDDABLE = "Embeddable";
    private static final String SYNCHRONIZABLE = "Synchronizable";
    private static final String RESOURCE_HANDLER = "Handler";

    private final String type;
    private final String synchronizable;
    private final String embeddable;
    private final String handler;

    private MediaMosaServices(String type) {
        this.type = type;
        embeddable = this.type + EMBEDDABLE;
        synchronizable = this.type + SYNCHRONIZABLE;
        handler = this.type + RESOURCE_HANDLER;
    }

    public ResourceHandler getResourceHandler() {
        return HippoServiceRegistry.getService(ResourceHandler.class, handler);
    }

    public Embeddable getEmbeddable() {
        return HippoServiceRegistry.getService(Embeddable.class, embeddable);
    }

    public Synchronizable getSynchronizable() {
        return HippoServiceRegistry.getService(Synchronizable.class, synchronizable);
    }

    public void register(HippoMediaMosaResourceManager service) {
        HippoServiceRegistry.registerService(service, Synchronizable.class, synchronizable);
        HippoServiceRegistry.registerService(service, ResourceHandler.class, handler);
        HippoServiceRegistry.registerService(service, Embeddable.class, embeddable);
        if (!HippoServiceRegistry.getRegistrations(MediamosaRemoteService.class).isEmpty()) {
            throw new IllegalStateException("MediamosaRemoteService can only be registered once");
        }
        HippoServiceRegistry.registerService(service, MediamosaRemoteService.class);
    }

    public void unregister(HippoMediaMosaResourceManager service) {
        HippoServiceRegistry.unregisterService(service, Synchronizable.class, synchronizable);
        HippoServiceRegistry.unregisterService(service, ResourceHandler.class, handler);
        HippoServiceRegistry.unregisterService(service, Embeddable.class, embeddable);
        HippoServiceRegistry.unregisterService(service, MediamosaRemoteService.class);
    }
}
