package com.vordel.client.ext.filter.quick;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.URI;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.vordel.es.ESPK;
import com.vordel.es.Entity;
import com.vordel.es.EntityStore;
import com.vordel.es.EntityStoreException;
import com.vordel.es.EntityStoreFactory;
import com.vordel.es.EntityType;
import com.vordel.es.Field;
import com.vordel.es.Value;
import com.vordel.es.loader.Loader;

public class QuickFilterImport {
	public static final String QUICKFILTER_CLASS = "com.vordel.client.ext.filter.quick.QuickFilter";
	public static final String ENTITYSTORE_NS = "";
	public static void main(String[] args) {
		if (args.length != 2) {
			printUsage("wrong number of arguments");
		}

		File root = new File(args[0]);

		if (!root.isDirectory()) {
			printUsage("The filter argument is not a directory");
		}

		File config = new File(new File(args[1]), "configs.xml");

		EntityStore store = retrievePrimaryStore(config, new Properties());

		File ui = new File(root, "ui.xml");

		if (!ui.isFile()) {
			printUsage("ui.xml file is missing");
		}

		File resources = new File(root, "resources.properties");

		if (!resources.isFile()) {
			printUsage("resources.properties file is missing");
		}

		File typeset = new File(root, "typedoc.xml");

		if (!typeset.isFile()) {
			printUsage("typeset.xml file is missing");
		}

		File script = locateScriptFile(root);
		Matcher matcher = SCRIPTNAME_REGEX.matcher(script.getName());

		if (!matcher.matches()) {
			/* This should not occur */
			throw new IllegalStateException();
		}

		String extension = matcher.group(1);
		String engineName = ENGINENAMES.get(extension);

		if (engineName == null) {
			printUsage("no registered engine for the given script");
		}

		Properties props = new Properties();

		try {
			props.load(new FileInputStream(resources));

			String name = props.getProperty("FILTER_DISPLAYNAME", null);
			String description = props.getProperty("FILTER_DESCRIPTION", null);
			String category = props.getProperty("FILTER_CATEGORY", "Utility");
			String icon = props.getProperty("FILTER_ICON", "filter_small");

			if ((name == null) || name.isEmpty()) {
				printUsage("filter display Name can't be null or empty");
			}

			if ((description == null) || description.isEmpty()) {
				printUsage("filter description can't be null or empty");
			}

			importQuickFilter(store, name, description, category, icon, engineName, script, ui, typeset, props);
		} catch (IOException e) {
			// XXX to be removed
			e.printStackTrace();

			printUsage("Unable to load filter resources");
		}
	}

//	private static EntityStore retrieveStore(File configFile, Properties properties) {
//		if (!configFile.isFile()) {
//			printUsage("The config argument is not a directory");
//		}
//
//		EntityStore store = null;
//
//		try {
//			store = retrieveStore(toFederatedURL(configFile), properties);
//		} catch (RuntimeException e) {
//			throw new IllegalStateException(e);
//		}
//
//		if (store == null) {
//			printUsage("could not load configuration");
//		}
//
//		return store;
//	}

	public static String toFederatedURL(File configFile) {
		URI uri = configFile.toURI();

		return String.format("federated:%s", uri.toASCIIString());
	}

	public static EntityStore retrieveStore(String url, Properties properties) {
		EntityStoreFactory factory = EntityStoreFactory.getInstance();
		EntityStore store = factory.getEntityStoreForURL(url);

		store.connect(url, properties);

		return store;
	}

	public static EntityStore retrievePrimaryStore(File configFile, Properties properties) {
		File primaryFile = new File(configFile.getParent(), "PrimaryStore.xml");

		return retrieveStore(primaryFile.getAbsoluteFile().toURI().toASCIIString(), properties);
	}

	private static final Set<String> QUICKFILTER_RESERVEDFIELDS = quickFilterReservedFields();

	private static final Set<String> quickFilterReservedFields() {
		Set<String> reserved = new HashSet<String>();

		reserved.add("successNode");
		reserved.add("failureNode");
		reserved.add("name");
		reserved.add("logMask");
		reserved.add("logMaskType");
		reserved.add("logFatal");
		reserved.add("logFailure");
		reserved.add("logSuccess");
		reserved.add("category");
		reserved.add("abortProcessingOnLogError");
		reserved.add("classloader");
		reserved.add("definition");

		return Collections.unmodifiableSet(reserved);
	}

	private static String parseScript(File script) {
		String result = null;

		try {
			RandomAccessFile raf = new RandomAccessFile(script, "r");

			try {
				long length = raf.length();

				if (length > ((long) Integer.MAX_VALUE)) {
					printUsage(String.format("script file '%s' is too long", script.getName()));
				}

				MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY, 0, length);
				byte[] buffer = new byte[(int) length];

				map.get(buffer);

				result = new String(buffer, "UTF-8");
			} finally {
				raf.close();
			}
		} catch (IOException e) {
			printUsage(String.format("script file '%s' can't be parsed", script.getName()));
		}

		return result;
	}

	private static Document parseXml(File xml) {
		Document document = null;

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			factory.setNamespaceAware(true);

			DocumentBuilder builder = factory.newDocumentBuilder();

			document = builder.parse(xml);
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		} catch (SAXException e) {
			printUsage(String.format("xml file '%s' can't be parsed", xml.getName()));
		} catch (IOException e) {
			printUsage(String.format("xml file '%s' can't be parsed", xml.getName()));
		}

		return document;
	}

	private static File createTypeSet(File typeDoc) {		
		File typeSet = new File(typeDoc.getParent(), "typeset.xml");

		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

			factory.setNamespaceAware(true);

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.newDocument();

			Element typesetNode = document.createElement("typeSet");
			Element typedocNode = document.createElement("typedoc");

			typedocNode.setAttribute("file", typeDoc.getName());

			typesetNode.appendChild(typedocNode);
			document.appendChild(typesetNode);

			Source source = new DOMSource(document.getDocumentElement());
			OutputStream output = new FileOutputStream(typeSet);
			Result result = new StreamResult(output);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();

			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "no");

			transformer.transform(source, result);

		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		} catch (TransformerException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			printUsage(String.format("can't write typeset to '%s' file", typeSet.getName()));
		}
		return typeSet;
	}

	private static String toString(Document document) {
		try {
			Source source = new DOMSource(document.getDocumentElement());
			StringWriter output = new StringWriter();
			Result result = new StreamResult(output);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();

			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "no");

			transformer.transform(source, result);

			return output.toString();
		} catch (TransformerException e) {
			throw new IllegalStateException(e);
		}
	}

	public static EntityType parseQuickFilterType(EntityStore store, File typeset) {
		EntityType result = null;

		Document document = parseXml(typeset);
		Element root = document.getDocumentElement();

		if ((root == null) || (!"entityStoreData".equals(root.getTagName())) /*|| (!ENTITYSTORE_NS.equals(root.getNamespaceURI()))*/) {
			printUsage("typeset root element is invalid (expected <entityStoreData xmlns=\"http://www.vordel.com/2005/06/24/entityStore\">)");
		}

		List<Element> quickFilters = new ArrayList<Element>();

		NodeList types = root.getElementsByTagNameNS(ENTITYSTORE_NS, "entityType");
		int typesLength = types.getLength();

		for (int typeIndex = 0; typeIndex < typesLength; typeIndex++) {
			Element type = (Element) types.item(typeIndex);
			String extend = type.getAttribute("extends");

			if (extend.equals("Filter")) {
				printUsage("filter type must extend 'QuickFilter' instead of 'Filter");
			} else if (extend.equals("QuickFilter")) {
				quickFilters.add(type);

				NodeList fields = type.getElementsByTagNameNS(ENTITYSTORE_NS, "field");
				int fieldsLength = fields.getLength();

				for (int fieldIndex = 0; fieldIndex < fieldsLength; fieldIndex++) {
					Element field = (Element) fields.item(fieldIndex);
					String fieldName = field.getAttribute("name");

					if (QUICKFILTER_RESERVEDFIELDS.contains(fieldName)) {
						printUsage(String.format("'%s' is a reserved field name", fieldName));
					}
				}
			}
		}

		if (quickFilters.isEmpty()) {
			printUsage("no quick filter declaration");
		} else if (quickFilters.size() > 1) {
			printUsage("multiple quick filter declaration");
		}

		result = EntityStoreFactory.getInstance().getEntityTypeFactory().create(store, quickFilters.get(0));

		if (!checkQuickFilterClass(result)) {
			printUsage("quick filter declaration use wrong filter class");
		}

		return result;
	}

	private static Map<String, String> ENGINENAMES = engineNameMap();

	private static Map<String, String> engineNameMap() {
		Map<String, String> engineNameMap = new HashMap<String, String>();

		engineNameMap.put("js", "nashorn");
		engineNameMap.put("py", "jython");
		engineNameMap.put("groovy", "groovy");

		return Collections.unmodifiableMap(engineNameMap);
	}

	public static Entity getQuickFilterGroup(EntityStore store) {
		EntityType quickFilterGroupType = store.getTypeForName("QuickFilterGroup");
		Collection<ESPK> quickFilterGroupEntities = store.listChildren(store.getRootPK(), quickFilterGroupType);
		Iterator<ESPK> iterator = quickFilterGroupEntities.iterator();

		if (!iterator.hasNext()) {
			printUsage("no QuickFilter Group found in configuration did you import QuickFilter TypeSet ?");
		}

		Entity quickFilterGroup = store.getEntity(iterator.next());

		if (iterator.hasNext()) {
			printUsage("got multiple QuickFilter Groups");
		}

		return quickFilterGroup;
	}

	private static boolean checkQuickFilterClass(EntityType quickFilterType) {
		boolean valid = false;

		if (quickFilterType != null) {
			Field clazz = quickFilterType.getConstantField("class");

			if (clazz != null) {
				Value[] values = clazz.getValues();

				if ((values != null) && (values.length == 1)) {
					Value value = values[0];

					valid = QUICKFILTER_CLASS.equals(value.getData());
				}
			}
		}

		return valid;
	}

	private static Map<String, Entity> getQuickFilterDefinition(EntityStore store, Entity quickFilterGroup, EntityType quickFilterDefinitionType) {
		Collection<ESPK> quickFilterDefinitionCollection = store.listChildren(quickFilterGroup.getPK(), quickFilterDefinitionType);
		Map<String, Entity> quickFilterDefinitionMap = new HashMap<String, Entity>();

		for(ESPK quickFilterDefinitionPK : quickFilterDefinitionCollection) {
			Entity quickFilterDefinition = store.getEntity(quickFilterDefinitionPK);

			quickFilterDefinitionMap.put(quickFilterDefinition.getStringValue("name"), quickFilterDefinition);
		}

		return quickFilterDefinitionMap;
	}

	public static void importQuickFilter(EntityStore store, String displayName, String description, String category, String icon, String engineName, File script, File ui, File typedoc, Properties resources) {
		EntityType quickFilterDefinitionType = store.getTypeForName("QuickFilterDefinition");

		if (quickFilterDefinitionType == null) {
			printUsage("no QuickFilterDefinition type found in configuration did you import QuickFilter TypeSet ?");
		}

		Entity quickFilterGroup = getQuickFilterGroup(store);
		Map<String, Entity> quickFilterDefinitionMap = getQuickFilterDefinition(store, quickFilterGroup, quickFilterDefinitionType);

		/* parse and perform sanity check of filter typeset */
		EntityType quickFilterType = parseQuickFilterType(store, typedoc);

		if (!checkQuickFilterClass(quickFilterType)) {
			printUsage("quick filter declaration use wrong filter class");
		}

		String name = quickFilterType.getName();

		Entity existing = quickFilterDefinitionMap.get(name);
		Entity imported = existing == null ? new Entity(quickFilterDefinitionType) : existing;

		/* filter type will be used as definition Name */
		imported.setStringField("name", name);
		imported.setStringField("displayName", displayName);
		imported.setStringField("description", description);
		imported.setStringField("icon", icon);
		imported.setStringField("palette", category);
		imported.setStringField("engineName", engineName);
		imported.setStringField("script", parseScript(script));
		imported.setStringField("ui", toString(parseXml(ui)));

		StringWriter builder = new StringWriter();
		
		try {
			resources.store(builder, null);

			imported.setStringField("resources", builder.toString());
		} catch (IOException e) {
			/* ignore */
		}
		
		/*
		 * Import or update the script definition
		 */
		if (existing != null) {
			store.updateEntity(imported);
		} else {
			ESPK importedPK = store.addEntity(quickFilterGroup.getPK(), imported);

			imported = store.getEntity(importedPK);
		}

		try {
			File typeset = createTypeSet(typedoc);
			Loader loader = new Loader();
			
			loader.load(store, typeset);
		} catch (EntityStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static final Pattern SCRIPTNAME_REGEX = Pattern.compile("script\\.(js|py|groovy)");

	private static File locateScriptFile(File root) {
		List<File> scripts = new ArrayList<File>();

		for (File script : root.listFiles()) {
			if (script.isFile() && SCRIPTNAME_REGEX.matcher(script.getName()).matches()) {
				scripts.add(script);
			}
		}

		int count = scripts.size();

		if (count == 0) {
			printUsage("script is missing (one of script.js, script.py or script.groovy)");
		} else if (count > 1) {
			printUsage("multiple scripts found");
		}

		return scripts.get(0);
	}

	private static void printUsage(String message) {
		if (message != null) {
			System.err.println(message);
		}

		System.err.println("Usage: QuickFilterImport <filter directory name> <config directory name>");
		System.exit(-1);
	}
}
