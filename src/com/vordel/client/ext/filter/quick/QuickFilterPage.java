package com.vordel.client.ext.filter.quick;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.swt.widgets.Composite;

import com.vordel.circuit.ext.filter.quick.AbstractQuickFilter;
import com.vordel.circuit.ext.filter.quick.QuickScriptFilter;
import com.vordel.client.manager.wizard.VordelPage;
import com.vordel.common.ResourceBase;
import com.vordel.es.Entity;
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
		String xmlUI = QuickScriptFilter.getConstantStringValue(filterType, AbstractQuickFilter.QUICKFILTER_UI);
		ResourceBase resourceBase = new QuickFilterResourceBase(filterType, getClass());
		
		setResourceBase(resourceBase);
		
		// set the title and description from the entity
		setTitle(QuickScriptFilter.getConstantStringValue(filterType, AbstractQuickFilter.QUICKFILTER_DISPLAYNAME));
		setDescription(QuickScriptFilter.getConstantStringValue(filterType, AbstractQuickFilter.QUICKFILTER_DESCRIPTION));

		// set the UI controls from the xmlUI
		InputStream in = new ByteArrayInputStream(xmlUI.getBytes());
		Composite panel = render(parent,  in);
		setControl(panel);
		setPageComplete(true);
	} 
}