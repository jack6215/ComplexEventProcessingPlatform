/*******************************************************************************
 * Signavio Core Components
 * Copyright (C) 2012  Signavio GmbH
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.signavio.warehouse.business.util.jpdl4;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NamedNodeMap;

public class Custom extends Node {

	private String clazz;
	private String expr;
	private String lang;
	private String factory;
	private String method;
	private String autoWire;
	private String cache;
	
	private List<Field> fields;
	private List<Property> properties;
	private List<Arg> args;
	
	public Custom(JSONObject java) {

		this.name = JsonToJpdl.getAttribute(java, "name");
		this.clazz = JsonToJpdl.getAttribute(java, "class");
		this.expr = JsonToJpdl.getAttribute(java, "expr");
		this.lang = JsonToJpdl.getAttribute(java, "lang");
		this.factory = JsonToJpdl.getAttribute(java, "factory");
		this.method = JsonToJpdl.getAttribute(java, "method");
		this.autoWire = JsonToJpdl.getAttribute(java, "autowire");
		this.cache = JsonToJpdl.getAttribute(java, "cache");
		
		this.bounds = JsonToJpdl.getBounds(java);

		fields = new ArrayList<Field>();
		try {
			JSONArray parameters = java.getJSONObject("properties")
					.getJSONObject("fields").getJSONArray("items");
			for (int i = 0; i < parameters.length(); i++) {
				JSONObject item = parameters.getJSONObject(i);
				fields.add(new Field(item));
			}
		} catch (JSONException e) {
		}

		properties = new ArrayList<Property>();
		try {
			JSONArray parameters = java.getJSONObject("properties")
					.getJSONObject("properties").getJSONArray("items");
			for (int i = 0; i < parameters.length(); i++) {
				JSONObject item = parameters.getJSONObject(i);
				properties.add(new Property(item));
			}
		} catch (JSONException e) {
		}
		
		args = new ArrayList<Arg>();
		try {
			JSONArray parameters = java.getJSONObject("properties")
					.getJSONObject("args").getJSONArray("items");
			for (int i = 0; i < parameters.length(); i++) {
				JSONObject item = parameters.getJSONObject(i);
				args.add(new Arg(item));
			}
		} catch (JSONException e) {
		}

		this.outgoings = JsonToJpdl.getOutgoings(java);

	}

	public Custom(org.w3c.dom.Node custom) {
		this.uuid = "oryx_" + UUID.randomUUID().toString();
		NamedNodeMap attributes = custom.getAttributes();
		this.name = JpdlToJson.getAttribute(attributes, "name");
		this.clazz = JpdlToJson.getAttribute(attributes, "class");
		this.expr = JpdlToJson.getAttribute(attributes, "expr");
		this.lang = JpdlToJson.getAttribute(attributes, "lang");
		this.factory = JpdlToJson.getAttribute(attributes, "factory");
		this.method = JpdlToJson.getAttribute(attributes, "method");
		this.autoWire = JpdlToJson.getAttribute(attributes, "autoWire");
		this.cache = JpdlToJson.getAttribute(attributes, "cache");
		this.bounds = JpdlToJson.getBounds(attributes.getNamedItem("g"));
		// TODO add properties and fields
	}

	@Override
	public String toJpdl() throws InvalidModelException {
		StringWriter jpdl = new StringWriter();
		jpdl.write("  <custom ");

		jpdl.write(JsonToJpdl.transformAttribute("name", name));

		try {
			if(clazz != null && !clazz.equals("")) {
				jpdl.write(JsonToJpdl.transformRequieredAttribute("class", clazz));
			}
			if(expr != null && !expr.equals("")) {
				jpdl.write(JsonToJpdl.transformRequieredAttribute("expr", expr));
			}
			if(lang != null && !lang.equals("")) {
				jpdl.write(JsonToJpdl.transformRequieredAttribute("lang", lang));
			}
			if(factory != null && !factory.equals("")) {
				jpdl.write(JsonToJpdl.transformRequieredAttribute("factory", factory));
			}
			if(method != null && !method.equals("")) {
				jpdl.write(JsonToJpdl.transformRequieredAttribute("method", method));
			}
			if(autoWire != null && !autoWire.equals("")) {
				jpdl.write(JsonToJpdl.transformRequieredAttribute("auto-wire", autoWire));
			}
			if(cache != null && !cache.equals("")) {
				jpdl.write(JsonToJpdl.transformRequieredAttribute("cache", cache)); 
			}
			
		} catch (InvalidModelException e) {
			throw new InvalidModelException("Invalid Java activity. "
					+ e.getMessage());
		}

		if (bounds != null) {
			jpdl.write(bounds.toJpdl());
		} else {
			throw new InvalidModelException(
					"Invalid Java activity. Bounds is missing.");
		}

		jpdl.write(" >\n");

		for (Field f : fields) {
			jpdl.write(f.toJpdl());
		}

		for (Property a : properties) {
			jpdl.write(a.toJpdl());
		}
		
		for (Arg a : args) {
			jpdl.write(a.toJpdl());
		}

		for (Transition t : outgoings) {
			jpdl.write(t.toJpdl());
		}

		jpdl.write("  </custom>\n\n");

		return jpdl.toString();
	}

	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public String getExpr() {
		return expr;
	}

	public void setExpr(String expr) {
		this.expr = expr;
	}

	@Override
	public JSONObject toJson() throws JSONException {
		JSONObject stencil = new JSONObject();
		stencil.put("id", "java");

		JSONArray outgoing = JpdlToJson.getTransitions(outgoings);

		JSONObject properties = new JSONObject();
		properties.put("bgcolor", "#ffffcc");
		if (name != null)
			properties.put("name", name);
		if (clazz != null)
			properties.put("class", clazz);
		if (expr != null)
			properties.put("method", expr);

		// TODO add fields and args

		JSONArray childShapes = new JSONArray();

		return JpdlToJson.createJsonObject(uuid, stencil, outgoing, properties,
				childShapes, bounds.toJson());
	}

}
