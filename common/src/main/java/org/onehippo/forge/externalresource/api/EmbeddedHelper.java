package org.onehippo.forge.externalresource.api;


import javax.jcr.*;
import java.util.HashMap;
import java.util.Map;

public abstract class EmbeddedHelper implements Embeddable {

    public abstract void initialize(Map<String, Object> properties);

    public void initialize(Node node){
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        try {
            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                Property property = properties.nextProperty();
                switch (property.getType()) {
                    case PropertyType.STRING:
                        propertyMap.put(property.getName(), property.getString());
                        break;
                    case PropertyType.BOOLEAN:
                        propertyMap.put(property.getName(), property.getBoolean());
                        break;
                    case PropertyType.LONG:
                        propertyMap.put(property.getName(), property.getLong());
                        break;
                }
            }
        } catch (RepositoryException e) {

        }
        initialize(propertyMap);
    }

}
