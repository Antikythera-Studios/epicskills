package com.yesman.epicskills.util;

import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public final class JsonUtil {
	private JsonUtil() {}
	
	public static JsonElement removeNullElements(JsonElement json) {
		if (json instanceof JsonArray jsonArray) {
			Iterator<JsonElement> arrayElement = jsonArray.iterator();
			
			while (arrayElement.hasNext()) {
				JsonElement element = arrayElement.next();
				
				if (element == null || element == JsonNull.INSTANCE) {
					arrayElement.remove();
				} else {
					removeNullElements(element);
				}
			}
		} else if (json instanceof JsonObject jsonObject) {
			jsonObject.entrySet().removeIf(entry -> {
				return entry.getValue() == null || entry.getValue() == JsonNull.INSTANCE;
			});
			
			jsonObject.entrySet().forEach(entry -> {
				removeNullElements(entry.getValue());
			});
		}
		
		return json;
	}
}
