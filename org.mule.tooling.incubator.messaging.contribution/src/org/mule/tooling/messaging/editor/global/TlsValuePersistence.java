package org.mule.tooling.messaging.editor.global;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.mule.tooling.model.messageflow.PropertyCollection;
import org.mule.tooling.model.messageflow.decorator.PropertyCollectionMap;
import org.mule.tooling.ui.modules.core.widgets.meta.AbstractValuePersistence;

public final class TlsValuePersistence extends AbstractValuePersistence {
    public static final String AUXILIARY_PROPERTY_PREFIX = "auxiliary;";
    public static final String TLS_NAMESPACE = "http://www.mulesoft.org/schema/mule/tls/context";
    public static final String TLS_STORE_ID_PREFIX = "@" + TLS_NAMESPACE + ";";
    
    @Override
    public String getId(final PropertyCollectionMap newProperties, PropertyCollectionMap parentProperties, String id) {
        Map<String, PropertyCollectionMap> propertyCollections = newProperties.getPropertyCollections();
        for (PropertyCollectionMap propertyCollection : propertyCollections.values()) {
            if (!getPersistenceProperties(propertyCollection).isEmpty()) {
                return getStoreId(id);
            }
        }
        return null;
    }

    private List<String> getPersistenceProperties(PropertyCollectionMap propertyCollection) {
        List<String> persistenceProperties = new ArrayList<>();
        final Set<String> collectionNames = propertyCollection.getPropertiesMap().keySet();
        for (String name : collectionNames) {
            if (!name.startsWith(AUXILIARY_PROPERTY_PREFIX)) {
                persistenceProperties.add(name);
            }
        }
        return persistenceProperties;
    }

    @Override
    public PropertyCollection adjust(final List<PropertyCollection> defs2, String id) {
        for (PropertyCollection propertyCollection : defs2) {
            if (propertyCollection.getName().startsWith(TLS_STORE_ID_PREFIX)) {
                return propertyCollection;
            }
        }

        return new PropertyCollection();
    }

    private String getStoreId(String nodeId) {
        if (StringUtils.isNotBlank(nodeId)) {
            // generate id for nested element with ;0 since there should be only one of each (therefore ordering -the index- doesn't matter)
            return "@" + nodeId + ";" + 0;
        }
        return null;
    }
    
    @Override
    public String convertModelToXML(final String str) {
        return null;
    }

    @Override
    public String convertXMLToModel(final String str) {
        return null;
    }
}