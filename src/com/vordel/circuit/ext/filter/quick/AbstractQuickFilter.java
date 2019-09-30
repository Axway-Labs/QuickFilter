package com.vordel.circuit.ext.filter.quick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vordel.circuit.DefaultFilter;
import com.vordel.circuit.FilterContainerImpl;
import com.vordel.config.ConfigContext;
import com.vordel.es.Entity;
import com.vordel.es.EntityStoreException;
import com.vordel.es.EntityType;
import com.vordel.es.Field;
import com.vordel.es.Value;

public abstract class AbstractQuickFilter extends DefaultFilter {
	public static final String QUICKFILTER_DISPLAYNAME = "displayName";
	public static final String QUICKFILTER_DESCRIPTION = "description";
	public static final String QUICKFILTER_ICON = "icon";
	public static final String QUICKFILTER_PALETTE = "palette";
	public static final String QUICKFILTER_RESOURCES = "resources";
	public static final String QUICKFILTER_UI = "ui";
	public static final String QUICKFILTER_REQUIRED = "required";
	public static final String QUICKFILTER_CONSUMED = "consumed";
	public static final String QUICKFILTER_GENERATED = "generated";

	@Override
	public Class<? extends FilterContainerImpl> getConfigPanelClass() throws ClassNotFoundException {
		return Class.forName("com.vordel.client.ext.filter.quick.QuickFilterGUI").asSubclass(FilterContainerImpl.class);
	}

	@Override
	public void configure(ConfigContext context, Entity entity) throws EntityStoreException {
		super.configure(context, entity);
		
		EntityType type = entity.getType();
		
		String[] required = getConstantStringValues(type, AbstractQuickFilter.QUICKFILTER_REQUIRED, true);
		String[] consumed = getConstantStringValues(type, AbstractQuickFilter.QUICKFILTER_CONSUMED, true);
		String[] generated = getConstantStringValues(type, AbstractQuickFilter.QUICKFILTER_GENERATED, true);
		
		if (required != null) {
			addPropDefs(reqProps, Arrays.asList(required));
		}
		
		if (consumed != null) {
			addPropDefs(consProps, Arrays.asList(consumed));
		}
		
		if (generated != null) {
			addPropDefs(genProps, Arrays.asList(generated));
		}
	}

	public static String getConstantStringValue(EntityType entity, String name) {
		String result = null;

		if (entity != null) {
			Field clazz = entity.getConstantField(name);

			if (clazz != null) {
				Value[] values = clazz.getValues();

				if ((values != null) && (values.length == 1)) {
					Value value = values[0];
					Object data = value == null ? null : value.getData();

					if (data instanceof String) {
						result = (String) data;
					}
				}
			}
		}

		return result;
	}

	public static String[] getConstantStringValues(EntityType entity, String name, boolean trim) {
		return splitValues(getConstantStringValue(entity, name), trim);
	}

	public static String[] splitValues(String property, boolean trim) {
		List<String> values = new ArrayList<String>();

		if (property != null) {
			String[] array = property.split(",");

			for(String value : array) {
				if (trim) {
					value = value.trim();
				}

				if (!value.isEmpty()) {
					values.add(value);
				}
			}
		}

		return values.isEmpty() ? null : values.toArray(new String[0]);
	}
}
