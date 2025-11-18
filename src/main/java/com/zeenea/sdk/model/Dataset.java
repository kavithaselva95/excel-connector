
package com.zeenea.sdk.model;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Minimal dataset representation for the scaffold.
 */
public class Dataset {
    public String name;
    public String description;
    public Map<String,Object> properties = new HashMap<>();
    public List<Field> fields = new ArrayList<>();

    public void addField(Field f) { fields.add(f); }

    public static class Field {
        public String name;
        public String type;
        public Map<String,Object> properties = new HashMap<>();
        public List<ItemReference> sourceFields = new ArrayList<>();
    }

    public static class ItemReference {
        public Map<String,String> datasetId = new HashMap<>();
        public String fieldName;
    }

    public String toJsonString() throws Exception {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return m.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }
}
