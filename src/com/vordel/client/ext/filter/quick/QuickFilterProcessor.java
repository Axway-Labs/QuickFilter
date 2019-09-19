package com.vordel.client.ext.filter.quick;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.vordel.circuit.CircuitAbortException;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.config.Circuit;
import com.vordel.config.ConfigContext;
import com.vordel.es.Entity;
import com.vordel.es.EntityStoreException;
import com.vordel.trace.Trace;

public class QuickFilterProcessor extends MessageProcessor {
	/**
	 * Attach script global function (optional)
	 */
	private static final String ATTACH_FUNCTION_NAME = "attach";
	/**
	 * Detach script global function (optional)
	 */
	private static final String DETACH_FUNCTION_NAME = "detach";
	/**
	 * Invoke script global function
	 */
	private static final String INVOKE_FUNCTION_NAME = "invoke";

	private ScriptEngine engine = null;

	/**
	 * Evaluate the script and try to invoke the 'attach' function.
	 * 
	 * @param ctx
	 * @param entity
	 * @throws EntityStoreException
	 */
	@Override
	public void filterAttached(ConfigContext ctx, com.vordel.es.Entity entity) throws EntityStoreException {
		super.filterAttached(ctx, entity);

		ScriptEngineManager mgr = new ScriptEngineManager();

		try {
			Entity definition = QuickFilter.getQuickFilterDefinition(ctx.getStore(), entity.getType());
			
			/* retrieve and create a script engine */
			engine = mgr.getEngineByName(definition.getStringValue("engineName"));

			/* execute the script, this will create script functions */
			engine.eval(definition.getStringValue("script"));

			try {
				/* try to invoke the attach function */
				invokeFunction(engine, ATTACH_FUNCTION_NAME, ctx, entity);
			} catch (NoSuchMethodException ex) {
				Trace.error(String.format("can't invoke the quick filter '%s' method", ATTACH_FUNCTION_NAME), ex);
			}
		} catch (ScriptException ex) {
			Throwable cause = ex.getCause();

			if (cause instanceof EntityStoreException) {
				/* if an entity store exception has been recognised, throw it */
				throw (EntityStoreException) cause;
			} else {
				/* otherwise, juste trace the error */
				Trace.error("There was a problem loading the script: " + ex.getMessage());
				Trace.debug(ex);
			}
		}
	}

	@Override
	public void filterDetached() {
		try {
			/* try to invoke the detach function */
			invokeFunction(engine, DETACH_FUNCTION_NAME);
		} catch (ScriptException ex) {
			Trace.error("There was a problem unloading the script: " + ex.getMessage());
			Trace.debug(ex);
		} catch (NoSuchMethodException ex) {
			Trace.error(String.format("can't invoke the quick filter '%s' method", DETACH_FUNCTION_NAME), ex);
		}
	}
	
	@Override
	public boolean invoke(Circuit c, Message m) throws CircuitAbortException {
		try {
			/* invoke script main function */
			Object result = invokeFunction(engine, INVOKE_FUNCTION_NAME, c, m);

			Trace.debug("Return from script is: " + String.valueOf(result));

			if (!(result instanceof Boolean)) {
				Trace.error("The script function \"invoke() " + "\" must return true or false.");
				Class<? extends Object> clazz = result == null ? null : result.getClass();

				throw new CircuitAbortException("Script must return a boolean value." + "The script returned: " + String.valueOf(clazz));
			}

			Trace.debug("ScriptProcessor.invoke: finished with status " + result);

			return ((Boolean) result).booleanValue();
		} catch (ScriptException ex) {
			throw asCircuitAbortException(ex);
		} catch (NoSuchMethodException ex) {
			Trace.error(String.format("can't invoke the quick filter '%s' method", INVOKE_FUNCTION_NAME), ex);

			throw new CircuitAbortException(ex);
		}
	}

	public static CircuitAbortException asCircuitAbortException(ScriptException ex) {
		CircuitAbortException result = null;

		if (ex != null) {
			Throwable cause = ex.getCause();

			/*
			 * Is the underlying script launched a CircuitAbortException, just
			 * unwrap it. Error location is lost, but it allows to use
			 * exceptions from script.
			 */
			if (cause instanceof CircuitAbortException) {
				result = (CircuitAbortException) cause;
			} else {
				result = new CircuitAbortException(ex);
			}
		}

		return result;
	}

	/**
	 * Simple convenience method to invoke functions in the script.
	 * 
	 * @param name
	 *            function name
	 * @param args
	 *            variable arguments array
	 * @return function result (as an object)
	 * @throws NoSuchMethodException
	 *             if the function does not exists
	 * @throws ScriptException
	 *             in case an error is raised within the script
	 */
	private static Object invokeFunction(ScriptEngine engine, String name, Object... args) throws NoSuchMethodException, ScriptException {
		Invocable invocableEngine = (Invocable) engine;

		return invocableEngine.invokeFunction(name, args);
	}
}
