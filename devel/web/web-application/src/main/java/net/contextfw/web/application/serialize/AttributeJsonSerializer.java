/**
 * Copyright 2010 Marko Lavikainen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.contextfw.web.application.serialize;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * A convinience class to handle attribute and Json serialization at the same time
 * 
 * <p>
 *  This class can be used to do all serialization for attributes and json,
 *  if the serialization method is the same.
 * </p>
 * 
 * @param <S>
 *  Type of source
 */
public abstract class AttributeJsonSerializer<S> implements JsonDeserializer<S>, JsonSerializer<S>, AttributeSerializer<S> {
    
    @Override
    public abstract String serialize(S source);
    
    public abstract S deserialize(String serialized);

    @Override
    public JsonElement serialize(S source, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(serialize(source));
    }

    @Override
    public S deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        return deserialize(json.getAsString());
    }
}
