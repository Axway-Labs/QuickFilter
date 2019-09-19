package com.vordel.client.ext.filter.quick;

import java.util.Vector;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.vordel.client.manager.Images;
import com.vordel.client.manager.Manager;
import com.vordel.client.manager.filter.DefaultGUIFilter;
import com.vordel.client.manager.wizard.VordelPage;
import com.vordel.es.Entity;
import com.vordel.es.EntityStore;
import com.vordel.es.EntityType;

public class QuickFilterGUI extends DefaultGUIFilter {
	private Entity definition = null;

	@Override
	public void setEntityType(EntityType type) {
		super.setEntityType(type);
		
		Manager manager = Manager.getInstance();
		EntityStore es = manager.getEntityStore();
		
		this.definition  = QuickFilter.getQuickFilterDefinition(es, type);
	}

	@Override
	public String getSmallIconId() {
		String id = definition == null ? "filter_small" : definition.getStringValue("icon");
		
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
		String category = definition == null ? "Utility" : definition.getStringValue("palette");
		
		return new String[] { category };
	}

	public String getTypeName() {
		String typeName = definition == null ? "Quick Filter" : definition.getStringValue("displayName");

		return typeName;
	}

	public Vector<VordelPage> getPropertyPages() {
		Vector<VordelPage> pages = new Vector<VordelPage>();

		pages.add(new QuickFilterPage());
		pages.add(createLogPage());

		return pages;
	}
}