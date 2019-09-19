package com.vordel.client.ext.filter.quick;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.swt.widgets.Composite;

import com.vordel.client.manager.wizard.VordelPage;
import com.vordel.common.ResourceBase;
import com.vordel.es.Entity;
import com.vordel.es.EntityStore;
import com.vordel.es.EntityType;

public class QuickFilterPage extends VordelPage 
{    
	public QuickFilterPage() {
		super("QuickFilterPage");

		setPageComplete(false);
	}

	public String getHelpID() {
		return "QuickFilterPage.help";
	}

	public boolean performFinish() {
		return true;
	}

	public void createControl(Composite parent) {
		Entity e = getEntity();
		EntityType filterType = e.getType();
		EntityStore es = getManager().getEntityStore();
		Entity definition = QuickFilter.getQuickFilterDefinition(es, filterType);
		String xmlUI = definition.getStringValue("ui");
		ResourceBase resourceBase = new QuickFilterResourceBase(definition, getClass());
		
		setResourceBase(resourceBase);
		
		// set the title and description from the entity
		setTitle(definition.getStringValue("displayName"));
		setDescription(definition.getStringValue("description"));
		// set the UI controls from the xmlUI
		
		InputStream in = new ByteArrayInputStream(xmlUI.getBytes());
		Composite panel = render(parent,  in);
		setControl(panel);
		setPageComplete(true);
	} 
}