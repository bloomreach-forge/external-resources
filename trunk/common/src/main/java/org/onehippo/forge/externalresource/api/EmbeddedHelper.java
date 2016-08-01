package org.onehippo.forge.externalresource.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EmbeddedHelper implements Embeddable {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedHelper.class);

    public abstract void initialize(Map<String, Object> properties);

    public void initialize(Node node) {
        Map<String, Object> propertyMap = new HashMap<>();
        try {
            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                Property property = properties.nextProperty();
                Object value = getValue(property);
                if (value != null) {
                    propertyMap.put(property.getName(), value);
                }
            }
        } catch (RepositoryException e) {
            LOG.warn("Failed to convert some properties", e);
        }
        initialize(propertyMap);
    }

    private Object getValue(Property property) throws RepositoryException {
        if (!property.isMultiple()) {
            return getValue(property.getValue());
        } else {
            List<Object> result = new ArrayList<>();
            for (Value propertyValue : property.getValues()) {
                Object value = getValue(propertyValue);
                if (value != null) {
                    result.add(value);
                }
            }
            return result.toArray();
        }
    }

    private Object getValue(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.STRING:
                return value.getString();
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.LONG:
                return value.getLong();
            default:
                LOG.debug("no conversion for type {}", value.getType());
                return null;
        }
    }
}
