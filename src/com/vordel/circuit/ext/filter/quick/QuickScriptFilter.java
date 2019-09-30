package com.vordel.circuit.ext.filter.quick;

import com.vordel.circuit.MessageProcessor;

public class QuickScriptFilter extends AbstractQuickFilter {
	public static final String QUICKFILTER_ENGINENAME = "engineName";
	public static final String QUICKFILTER_SCRIPT = "script";
	
	@Override
	public Class<? extends MessageProcessor> getMessageProcessorClass() throws ClassNotFoundException {
		return Class.forName("com.vordel.circuit.ext.filter.quick.QuickFilterProcessor").asSubclass(MessageProcessor.class);
	}
}
