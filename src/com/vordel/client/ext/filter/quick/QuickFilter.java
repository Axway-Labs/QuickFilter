package com.vordel.client.ext.filter.quick;

import java.util.Collection;

import com.vordel.circuit.DefaultFilter;
import com.vordel.circuit.FilterContainerImpl;
import com.vordel.circuit.MessageProcessor;
import com.vordel.es.ESPK;
import com.vordel.es.Entity;
import com.vordel.es.EntityStore;
import com.vordel.es.EntityType;

public class QuickFilter extends DefaultFilter {
	@Override
	public Class<? extends MessageProcessor> getMessageProcessorClass() throws ClassNotFoundException {
		return Class.forName("com.vordel.client.ext.filter.quick.QuickFilterProcessor").asSubclass(MessageProcessor.class);
	}

	@Override
	public Class<? extends FilterContainerImpl> getConfigPanelClass() throws ClassNotFoundException {
		return Class.forName("com.vordel.client.ext.filter.quick.QuickFilterGUI").asSubclass(FilterContainerImpl.class);
	}

	public static Entity getQuickFilterDefinition(EntityStore es, EntityType filterType) {
		Entity definitions = QuickFilterImport.getQuickFilterGroup(es);
		EntityType definitionType = es.getTypeForName("QuickFilterDefinition");
		Collection<ESPK> childs = es.listChildren(definitions.getPK(), definitionType);

		for (ESPK quickFilterDefinitionPK : childs) {
			Entity quickFilterDefinition = es.getEntity(quickFilterDefinitionPK);
			String name = quickFilterDefinition.getStringValue("name");

			if (name.equals(filterType.getName())) {
				return quickFilterDefinition;
			}
		}

		return null;
	}
}
