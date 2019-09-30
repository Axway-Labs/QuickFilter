package com.vordel.client.ext.filter.quick;

import java.util.Vector;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.vordel.circuit.ext.filter.quick.AbstractQuickFilter;
import com.vordel.circuit.ext.filter.quick.QuickScriptFilter;
import com.vordel.client.manager.Images;
import com.vordel.client.manager.filter.DefaultGUIFilter;
import com.vordel.client.manager.wizard.VordelPage;
import com.vordel.es.EntityType;

public class QuickFilterGUI extends DefaultGUIFilter {
	@Override
	public String getSmallIconId() {
		EntityType entityType = getEntityType();
		String id = entityType == null ? "filter_small" : QuickScriptFilter.getConstantStringValue(entityType, AbstractQuickFilter.QUICKFILTER_ICON);
		
		return id;
	}

	public Image getSmallImage() {
		String id = getSmallIconId();
		
		return Images.getImageRegistry().get(id);
	}

	public ImageDescriptor getSmallIcon() {
		String id = getSmallIconId();

		return Images.getImageDescriptor(id);
	}

	public String[] getCategories() {
		EntityType entityType = getEntityType();

		return entityType == null ? new String[] { "Utility" } : QuickScriptFilter.getConstantStringValues(entityType, AbstractQuickFilter.QUICKFILTER_PALETTE, true);
	}

	public String getTypeName() {
		EntityType entityType = getEntityType();
		String typeName = entityType == null ? "Quick Filter" : QuickScriptFilter.getConstantStringValue(entityType, AbstractQuickFilter.QUICKFILTER_DISPLAYNAME);

		return typeName;
	}

	public Vector<VordelPage> getPropertyPages() {
		Vector<VordelPage> pages = new Vector<VordelPage>();

		pages.add(new QuickFilterPage());
		pages.add(createLogPage());

		return pages;
	}
}