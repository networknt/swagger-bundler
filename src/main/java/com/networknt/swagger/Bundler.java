package com.networknt.swagger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Bundler {

    public static ObjectMapper mapper = new ObjectMapper();

    static Map<String, Map<String, Object>> references = new ConcurrentHashMap<>();
    static String folder = null;
    static Map<String, Object> definitions = null;

    public static void main(String ... argv) {
        //System.out.println("argv[0] = " + argv[0]);
        if(argv[0] != null) {
            folder = argv[0];
            // The input parameter is the folder that contains swagger.yaml and
            // this folder will be the base path to calculate remote references.
            Path path = Paths.get(argv[0], "swagger.yaml");
            try (InputStream is = Files.newInputStream(path)) {
                String json = null;
                Yaml yaml = new Yaml();
                Map<String, Object> map = (Map<String, Object>)yaml.load(is);
                // Now map contains everything in the swagger.yaml, create an empty
                // definitions if it doesn't exist. definition is a static variable
                // so that it is easy to use.
                definitions = (Map<String, Object>)map.get("definitions");
                if(definitions == null) {
                    definitions = new HashMap<>();
                    map.put("definitions", definitions);
                }
                // now let's handle the references.
                resolveMap(map);
                // convert the map back to json and output it.
                json = mapper.writeValueAsString(map);
                System.out.println(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("You must pass in a folder to a yaml file!");
        }
    }

    private static Map<String, Object> handlerPointer(String pointer) {
        Map<String, Object> result = new HashMap<>();
        if(pointer.startsWith("#")) {
            // local reference
            String refKey = pointer.substring(pointer.lastIndexOf("/") + 1);
            //System.out.println("refKey = " + refKey);
            Map<String, Object> refMap = (Map<String, Object>)definitions.get(refKey);
            if(isRefMapObject(refMap)) {
                // resolve any remote references in refMap.
                resolveExternalRef(refMap);
            } else {
                System.out.println("Only Object can be defined in definitions section");
                System.exit(0);
            }
            result.put("$ref", pointer);
        } else {
            // external reference and it must be a relative url
            Map<String, Object> refs = loadRef(pointer.substring(0, pointer.indexOf("#")));
            String refKey = pointer.substring(pointer.indexOf("#/") + 2);
            //System.out.println("refKey = " + refKey);
            Map<String, Object> refMap = (Map<String, Object>)refs.get(refKey);
            // now need to resolve the internal references in refMap.
            resolveLocalRef(refMap, refs);
            // check if the refMap type is object or not.
            if(isRefMapObject(refMap)) {
                // add to definitions
                definitions.put(refKey, refMap);
                // update the ref pointer to local
                result.put("$ref", "#/definitions/" + refKey);
            } else {
                // simple type, inline refMap instead.
                result = refMap;
            }
        }
        return result;
    }

    /**
     * Check if the input map is an json object or not.
     * @param refMap input map
     * @return
     */
    private static boolean isRefMapObject(Map<String, Object> refMap) {
        boolean result = false;
        for(Map.Entry<String, Object> entry: refMap.entrySet()) {
            if("type".equals(entry.getKey()) && "object".equals(entry.getValue())) {
                result = true;
            }
        }
        return result;
    }

    /**
     * load and cache remote reference. folder is a static variable assigned by argv[0]
     * it will check the cache first and only load it if it doesn't exist in cache.
     *
     * @param path the path of remote file
     * @return map of remote references
     */
    private static Map<String, Object> loadRef(String path) {
        Map<String, Object> result = references.get(path);
        if(result == null) {
            Path p = Paths.get(folder, path);
            try (InputStream is = Files.newInputStream(p)) {
                Yaml yaml = new Yaml();
                result = (Map<String, Object>)yaml.load(is);
                references.put(path, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static void resolveLocalRef(Map<String, Object> refMap, Map<String, Object> refs) {
        boolean isObject = false;
        for(Map.Entry<String, Object> entryEle: refMap.entrySet()) {
            //System.out.println("key = " + entryEle.getKey() + " value = " + entryEle.getValue());
            if("type".equals(entryEle.getKey()) && "object".equals(entryEle.getValue())) {
                isObject = true;
                continue;
            }
            if("properties".equals(entryEle.getKey()) && isObject) {
                Map<String, Object> props = (Map<String, Object>)entryEle.getValue();
                for(Map.Entry<String, Object> entryProp: props.entrySet()) {
                    //System.out.println("key = " + entryProp.getKey() + " value = " + entryProp.getValue());
                    String key = entryProp.getKey();
                    Map<String, Object> pointers = (Map<String, Object>)entryProp.getValue();
                    for(Map.Entry<String, Object> entryPointer: pointers.entrySet()) {
                        //System.out.println("key = " + entryPointer.getKey() + " value = " + entryPointer.getValue());
                        if("$ref".equals(entryPointer.getKey())) {
                            String pointer = (String)entryPointer.getValue();
                            String refKey = pointer.substring(2);
                            //System.out.println("refKey = " + refKey);
                            entryProp.setValue(refs.get(refKey));
                        }
                    }
                }
            }
        }
    }

    private static void resolveExternalRef(Map<String, Object> refMap) {
        boolean isObject = false;
        for(Map.Entry<String, Object> entryEle: refMap.entrySet()) {
            //System.out.println("key = " + entryEle.getKey() + " value = " + entryEle.getValue());
            if("type".equals(entryEle.getKey()) && "object".equals(entryEle.getValue())) {
                isObject = true;
                continue;
            }
            if("properties".equals(entryEle.getKey()) && isObject) {
                Map<String, Object> props = (Map<String, Object>)entryEle.getValue();
                for(Map.Entry<String, Object> entryProp: props.entrySet()) {
                    //System.out.println("key = " + entryProp.getKey() + " value = " + entryProp.getValue());
                    String key = entryProp.getKey();
                    Map<String, Object> pointers = (Map<String, Object>)entryProp.getValue();
                    for(Map.Entry<String, Object> entryPointer: pointers.entrySet()) {
                        //System.out.println("key = " + entryPointer.getKey() + " value = " + entryPointer.getValue());
                        if("$ref".equals(entryPointer.getKey())) {
                            String pointer = (String)entryPointer.getValue();
                            //System.out.println("pointer = " + pointer);
                            String refKey = pointer.substring(pointer.lastIndexOf("#/") + 2);
                            //System.out.println("refKey = " + refKey);
                            Map<String, Object> refs = loadRef(pointer.substring(0, pointer.indexOf("#")));
                            entryProp.setValue(refs.get(refKey));
                        }
                    }
                }
            }
        }
    }

    /**
     * It deep iterate a map object and looking for "$ref" and handle it.
     * @param map the map of swagger.yaml
     */
    public static void resolveMap(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                // check if this map is $ref, it should be size = 1
                if (((Map) value).size() == 1) {
                    Set keys = ((Map)value).keySet();
                    for (Iterator i = keys.iterator(); i.hasNext();) {
                        String k = (String)i.next();
                        if("$ref".equals(k)) {
                            String pointer = (String)((Map)value).get(k);
                            //System.out.println("pointer = " + pointer);
                            Map refMap = handlerPointer(pointer);
                            if(refMap.get("$ref") != null) {
                                // if return is another updated $ref
                                entry.setValue(handlerPointer(pointer));
                            } else {
                                // if return is inline object resolved.
                                entry.setValue(refMap);
                                continue;
                            }
                        }
                    }
                }
                resolveMap((Map) value);
            } else if (value instanceof List) {
                resolveList((List)value);
            } else {
                continue;
            }
        }
    }

    public static void resolveList(List list) {
        for (int i = 0; i < list.size(); i++) {
            if(list.get(i) instanceof Map) {
                // check if this map is $ref
                if (((Map) list.get(i)).size() == 1) {
                    Set keys = ((Map)list.get(i)).keySet();
                    for (Iterator j = keys.iterator(); j.hasNext();) {
                        String k = (String)j.next();
                        if("$ref".equals(k)) {
                            String pointer = (String)((Map)list.get(i)).get(k);
                            //System.out.println("pointer = " + pointer);
                            list.set(i, handlerPointer(pointer));
                        }
                    }
                }
                resolveMap((Map<String, Object>)list.get(i));
            } else if(list.get(i) instanceof List) {
                resolveList((List)list.get(i));
            } else {
                continue;
            }
        }
    }

}
