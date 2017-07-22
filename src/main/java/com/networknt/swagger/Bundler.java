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
        System.out.println("argv[0] = " + argv[0]);
        if(argv[0] != null) {
            folder = argv[0];
            Path path = Paths.get(argv[0], "swagger.yaml");
            try (InputStream is = Files.newInputStream(path)) {
                String json = null;
                Yaml yaml = new Yaml();
                Map<String, Object> map = (Map<String, Object>)yaml.load(is);
                definitions = new HashMap<>();
                map.put("definitions", definitions);
                // now let's handle the references.
                resolveMap(map);
                //updateResponseCodeSchemaRef(argv[0], map);



                json = mapper.writeValueAsString(map);
                System.out.println(json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("You must pass in a path to a yaml file!");
        }
    }

    /*
    private static void updateResponseCodeSchemaRef(String folder, Map<String, Object> map) {
        Map<String, Object> paths = (Map<String, Object>)map.get("paths");
        for(Map.Entry<String, Object> entryPath: paths.entrySet()) {
            //System.out.println("key = " + entryPath.getKey() + " value = " + entryPath.getValue());
            Map<String, Object> operations = (Map<String, Object>)entryPath.getValue();
            for(Map.Entry<String, Object> entryOp: operations.entrySet()) {
                //System.out.println("key = " + entryOp.getKey() + " value = " + entryOp.getValue());
                if(entryOp.getKey().startsWith("x-")) {
                    continue;
                }
                Map<String, Object> attrs = (Map<String, Object>) entryOp.getValue();
                for(Map.Entry<String, Object> entryAttr: attrs.entrySet()) {
                    //System.out.println("key = " + entryAttr.getKey() + " value = " + entryAttr.getValue());
                    if("responses".equals(entryAttr.getKey())) {
                        Map<Integer, Object> responses = (Map<Integer, Object>)entryAttr.getValue();
                        for(Map.Entry<Integer, Object> entryRes: responses.entrySet()) {
                            //System.out.println("key = " + entryRes.getKey() + " value = " + entryRes.getValue());
                            Map<String, Object> codes = (Map<String, Object>)entryRes.getValue();
                            for(Map.Entry<String, Object> entryCode: codes.entrySet()) {
                                //System.out.println("key = " + entryCode.getKey() + " value = " + entryCode.getValue());
                                if("schema".equals(entryCode.getKey())) {
                                    Map<String, Object> entrySchema = (Map<String, Object>)entryCode.getValue();
                                    for(Map.Entry<String, Object> entryRef: entrySchema.entrySet()) {
                                        //System.out.println("key = " + entryRef.getKey() + " value = " + entryRef.getValue());
                                        if("$ref".equals(entryRef.getKey())) {
                                            String pointer = (String)entryRef.getValue();
                                            System.out.println("pointer = " + pointer);
                                            handlerPointer(pointer, folder, map, entryRef);
                                        }
                                        if("properties".equals(entryRef.getKey())) {
                                            // in this case, the $ref will be in the next level.
                                            Map<String, Object> props = (Map<String, Object>)entryRef.getValue();
                                            for(Map.Entry<String, Object> entryProp: props.entrySet()) {
                                                //System.out.println("key = " + entryProp.getKey() + " value = " + entryProp.getValue());
                                                Map<String, Object> pointers = (Map<String, Object>)entryProp.getValue();
                                                for(Map.Entry<String, Object> entryPointer: pointers.entrySet()) {
                                                    System.out.println("key = " + entryProp.getKey() + " value = " + entryProp.getValue());
                                                    if("$ref".equals(entryPointer.getKey())) {
                                                        String pointer = (String)entryPointer.getValue();
                                                        System.out.println("pointer = " + pointer);
                                                        handlerPointer(pointer, folder, map, entryProp);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void handlerPointer(String pointer, String folder, Map<String, Object> map, Map.Entry<String, Object> entryRef) {
        if(pointer.startsWith("#")) {
            // local reference

        } else {
            // external reference and it must be a relative url
            Map<String, Object> refs = loadAndResolveRef(folder, pointer.substring(0, pointer.indexOf("#")));
            String refKey = pointer.substring(pointer.indexOf("#/") + 2);
            System.out.println("refKey = " + refKey);
            Map<String, Object> refMap = (Map<String, Object>)refs.get(refKey);
            // now need to resolve the internal references in refMap.
            resolveLocalRef(refMap, refs);
            // add to definitions
            Map<String, Object> definitions = (Map<String, Object>)map.get("definitions");
            definitions.put(refKey, refMap);
            // update the ref pointer to local
            entryRef.setValue("#/" + refKey);
        }

    }
    */

    private static String handlerPointer(String pointer) {
        String newPointer = null;
        if(pointer.startsWith("#")) {
            // local reference

        } else {
            // external reference and it must be a relative url
            Map<String, Object> refs = loadAndResolveRef(folder, pointer.substring(0, pointer.indexOf("#")));
            String refKey = pointer.substring(pointer.indexOf("#/") + 2);
            System.out.println("refKey = " + refKey);
            Map<String, Object> refMap = (Map<String, Object>)refs.get(refKey);
            // now need to resolve the internal references in refMap.
            resolveLocalRef(refMap, refs);
            // add to definitions
            definitions.put(refKey, refMap);
            // update the ref pointer to local
            newPointer = "#/" + refKey;
        }
        return newPointer;
    }

    private static Map<String, Object> loadAndResolveRef(String folder, String path) {
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
                        System.out.println("key = " + entryPointer.getKey() + " value = " + entryPointer.getValue());
                        if("$ref".equals(entryPointer.getKey())) {
                            String pointer = (String)entryPointer.getValue();
                            String refKey = pointer.substring(2);
                            entryProp.setValue(refs.get(refKey));
                        }
                    }
                }
            }
        }
    }

    public static void resolveMap(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                // check if this map is $ref
                if (((Map) value).size() == 1) {
                    Set keys = ((Map)value).keySet();
                    for (Iterator i = keys.iterator(); i.hasNext();) {
                        String k = (String)i.next();
                        if("$ref".equals(k)) {
                            String pointer = (String)((Map)value).get(k);
                            System.out.println("pointer = " + pointer);
                            entry.setValue(handlerPointer(pointer));
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
                            System.out.println("pointer = " + pointer);
                            ((Map)list.get(i)).put("$ref", handlerPointer(pointer));
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
