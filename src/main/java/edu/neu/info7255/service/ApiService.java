package edu.neu.info7255.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class ApiService {

	@Autowired
	JedisPool jedisPool;
	
	@Autowired
	EtagService etagService;
	
	

//    private JedisPool getJedisPool() {
//        if (this.jedisPool == null) {
//            this.jedisPool = new JedisPool();
//        }
//        return this.jedisPool;
//    }
	// function to save the object in redis
	public String saveObject(JSONObject jsonObject, String key) {

//		String objectKey = (String) jsonObject.get("objectId");
//		Jedis jedis = jedisPool.getResource();
//		jedis.set(objectKey, jsonObject.toString());
//		jedis.close();
        convertToMap(jsonObject);
        //String objectKey = (String) jsonObject.get("objectId");
		
		return setEtag(key,jsonObject);

	}
    public String getEtag(String key) {
		Jedis jedis = jedisPool.getResource();
		String etag = jedis.hget(key, "eTag");
		jedis.close();
        return etag;
    }
    
    public String setEtag(String key, JSONObject jsonObject){
		Jedis jedis = jedisPool.getResource();

        String eTag = etagService.getEtag(jsonObject);
        jedis.hset(key, "eTag", eTag);
        jedis.close();
        return eTag;
    }
	
	// function to check if the object exists in redis
	public boolean checkKeyExists(String objectKey) {

		Jedis jedis = jedisPool.getResource();
		boolean res = jedis.exists(objectKey);
		jedis.close();
        return res;

	}
	
	 public Map<String, Object> getPlanObject(String objectKey) {
		 Map<String, Object> outputMap = new HashMap<String, Object>();
	        getOrDeleteData(objectKey, outputMap, false);
	        return outputMap;
	    }
	 
	 public void deletePlan(String objectKey) {
	        getOrDeleteData(objectKey, null, true);

	 }
	 
	    public Map<String, Map<String, Object>> convertToMap(JSONObject jsonObject) {
	        Jedis jedis = jedisPool.getResource();

	        Map<String, Map<String, Object>> map = new HashMap<>();
	        Map<String, Object> valueMap = new HashMap<>();
	        Iterator<String> iterator = jsonObject.keys();

	        while (iterator.hasNext()){

	            String redisKey = jsonObject.get("objectType") + ":" + jsonObject.get("objectId");
	            String key = iterator.next();
	            Object value = jsonObject.get(key);

	            if (value instanceof JSONObject) {

	                value = convertToMap((JSONObject) value);
	                HashMap<String, Map<String, Object>> val = (HashMap<String, Map<String, Object>>) value;
	                jedis.sadd(redisKey + ":" + key, val.entrySet().iterator().next().getKey());
	                jedis.close();

	            } else if (value instanceof JSONArray) {
	                value = convertToList((JSONArray) value);
	                for (HashMap<String, HashMap<String, Object>> entry : (List<HashMap<String, HashMap<String, Object>>>) value) {
	                    for (String listKey : entry.keySet()) {
	                        jedis.sadd(redisKey + ":" + key, listKey);
	                        jedis.close();
	                        System.out.println(redisKey + ":" + key + " : " + listKey);
	                    }
	                }
	            } else {
	                jedis.hset(redisKey, key, value.toString());
	                jedis.close();
	                valueMap.put(key, value);
	                map.put(redisKey, valueMap);
	            }
	        }

	        System.out.println("MAP: " + map.toString());
	        jedis.close();
	        return map;

	    }

	    private List<Object> convertToList(JSONArray array) {
	        List<Object> list = new ArrayList<>();
	        for (int i = 0; i < array.length(); i++) {
	            Object value = array.get(i);
	            if (value instanceof JSONArray) {
	                value = convertToList((JSONArray) value);
	            } else if (value instanceof JSONObject) {
	                value = convertToMap((JSONObject) value);
	            }
	            list.add(value);
	        }
	        return list;
	    }

	    private boolean isStringInt(String s) {
	        try {
	            Integer.parseInt(s);
	            return true;
	        } catch (NumberFormatException ex) {
	            return false;
	        }
	    }

	    public JSONObject mergeData(JSONObject jsonObject, String redisKey){

	       JSONObject savedObject = new JSONObject(this.getPlanObject(redisKey));
	        if(savedObject == null){
	            return null;
	        }

	        Iterator<String> iterator = jsonObject.keys();
	        while (iterator.hasNext()){

	            String jsonKey = iterator.next();
	            Object jsonValue = jsonObject.get(jsonKey);

	            if(savedObject.get(jsonKey) == null){
	                savedObject.put(jsonKey, jsonValue);
	            } else {

	                if (jsonValue instanceof JSONObject) {
	                    JSONObject jsonValueObject = (JSONObject)jsonValue;
	                    String jsonObjKey = jsonKey + ":" + jsonValueObject.get("objectId");
	                    if (((JSONObject)savedObject.get(jsonKey)).get("objectId").equals(jsonValueObject.get("objectId"))) {
	                        savedObject.put(jsonKey, jsonValue);
	                    } else {
	                        JSONObject updatedJsonValue = this.mergeData(jsonValueObject, jsonObjKey);
	                        savedObject.put(jsonKey, updatedJsonValue);
	                    }
	                } else if (jsonValue instanceof JSONArray) {
	                    JSONArray jsonValueArray = (JSONArray) jsonValue;
	                    JSONArray savedJSONArray = savedObject.getJSONArray(jsonKey);
	                    for (int i = 0; i < jsonValueArray.length(); i++) {
	                        JSONObject arrayItem = (JSONObject)jsonValueArray.get(i);
	                        //check if objectId already exists in savedJSONArray
	                        int index = getIndexOfObjectId(savedJSONArray, (String)arrayItem.get("objectId"));
	                        if(index >= 0) {
	                            savedJSONArray.remove(index);
	                        }
	                        savedJSONArray.put(arrayItem);
	                    }
	                    savedObject.put(jsonKey, savedJSONArray);
	                } else {
	                    savedObject.put(jsonKey, jsonValue);
	                }

	            }

	        }

	        return  savedObject;
	    }

	    private int getIndexOfObjectId(JSONArray array, String objectId) {
	        int i = -1;

	        for (i = 0; i < array.length(); i++) {
	            JSONObject arrayObj = (JSONObject)array.get(i);
	            String itemId = (String)arrayObj.get("objectId");
	            if (objectId.equals(itemId)){
	                return i;
	            }
	        }

	        return i;
	    }


	    private Map<String, Object> getOrDeleteData(String redisKey, Map<String, Object> outputMap, boolean isDelete) {
	        Jedis jedis = jedisPool.getResource();

	        Set<String> keys = jedis.keys(redisKey + ":*");
	        keys.add(redisKey);
	        System.out.println("Keys for "+redisKey+": " + keys);
	        jedis.close();
	        for (String key : keys) {
	            if (key.equals(redisKey)) {
	                if (isDelete) {
	                    jedis.del(new String[]{key});
	                    jedis.close();
	                } else {
	                    Map<String, String> val = jedis.hgetAll(key);
	                    jedis.close();
	                    for (String name : val.keySet()) {
	                        if (!name.equalsIgnoreCase("eTag")) {
	                            outputMap.put(name,
	                                    isStringInt(val.get(name)) ? Integer.parseInt(val.get(name)) : val.get(name));
	                        }
	                    }
	                }

	            } else {
	                String newStr = key.substring((redisKey + ":").length());
	                System.out.println("Key to be serched :" + key + "--------------" + newStr);
	                Set<String> members = jedis.smembers(key);
	                jedis.close();
	                System.out.println("Members" + ":" + members);
	                if (members.size() > 1 || newStr.equals("linkedPlanServices")) {
	                    List<Object> listObj = new ArrayList<Object>();
	                    for (String member : members) {
	                        if (isDelete) {
	                            getOrDeleteData(member, null, true);
	                        } else {
	                            Map<String, Object> listMap = new HashMap<String, Object>();
	                            listObj.add(getOrDeleteData(member, listMap, false));

	                        }
	                    }
	                    if (isDelete) {
	                        jedis.del(new String[]{key});
	                        jedis.close();
	                    } else {
	                        outputMap.put(newStr, listObj);
	                    }

	                } else {
	                    if (isDelete) {
	                        jedis.del(new String[]{members.iterator().next(), key});
	                        jedis.close();
	                    } else {
	                        Map<String, String> val = jedis.hgetAll(members.iterator().next());
	                        jedis.close();
	                        Map<String, Object> newMap = new HashMap<String, Object>();
	                        for (String name : val.keySet()) {
	                            newMap.put(name,
	                                    isStringInt(val.get(name)) ? Integer.parseInt(val.get(name)) : val.get(name));
	                        }
	                        outputMap.put(newStr, newMap);
	                    }
	                }
	            }
	        }
	        jedis.close();
	        return outputMap;
	    }
}
