package edu.neu.info7255.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


import edu.neu.info7255.DemoApplication;
import edu.neu.info7255.service.ApiService;
import edu.neu.info7255.service.AuthorizeService;
import edu.neu.info7255.service.EtagService;

@RestController
public class ApiController {
	
	@Autowired
	ApiService apiService;
	
	@Autowired
	EtagService etagService;
	
	@Autowired
    AuthorizeService authorizeService;

    @Autowired
    private RabbitTemplate template;
    
   
	// api to store the object in redis
    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPlan(@RequestBody(required = false) String planObject, @RequestHeader HttpHeaders requestHeaders) {
        String authorization = requestHeaders.getFirst("Authorization");
        String result = authorizeService.authorize(authorization);
        if(result != "Valid Token"){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", result).toString());
        }
        
        if (planObject == null || planObject.isBlank()) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Invalid input!").toString());
        }

        JSONObject plan = new JSONObject(planObject);
        JSONObject schemaJSON = new JSONObject(new JSONTokener(Objects.requireNonNull(ApiController.class.getResourceAsStream("/schema.json"))));
        Schema schema = SchemaLoader.load(schemaJSON);
        try {
            schema.validate(plan);
        } catch (ValidationException e) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new JSONObject().put("error",e.getAllMessages()).toString());
        }

        if(apiService.checkKeyExists(plan.get("objectType") + ":" + plan.get("objectId"))){
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new JSONObject().put("message", "Plan already exists!").toString());
        }

        String key = plan.get("objectType") + ":" + plan.get("objectId");

        String etag = apiService.saveObject(plan,key);
       

        HttpHeaders headersToSend = new HttpHeaders();
        headersToSend.setETag(etag);
        
        // index object
        Map<String, String> actionMap = new HashMap<>();
        actionMap.put("operation", "SAVE");
        actionMap.put("body", planObject);

        System.out.println("Sending message: " + actionMap);

        template.convertAndSend(DemoApplication.queueName, actionMap);

        return new ResponseEntity<>("{\"objectId\": \"" + plan.getString("objectId") + "\"}", headersToSend, HttpStatus.CREATED);
    }

    // api to get object based on the object id from the redis
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/{objectType}/{objectID}")
    public ResponseEntity<?> getPlan(@PathVariable String objectID,@PathVariable String objectType, @RequestHeader HttpHeaders requestHeaders){
    	
        String authorization = requestHeaders.getFirst("Authorization");
        String result = authorizeService.authorize(authorization);
        if(result != "Valid Token"){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", result).toString());
        }
        
        String key = objectType + ":" + objectID;

        if(!apiService.checkKeyExists(key)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("message", "Object does not exists!").toString());
        } else {

//        	 Map<String, Object> plan = apiService.getPlanObject(key);
//            String etag = apiService.getEtag(key);
//
//            List<String> ifNotMatch;
//            try{
//                ifNotMatch = requestHeaders.getIfNoneMatch();
//            } catch (Exception e){
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).
//                        body(new JSONObject().put("error", "ETag value is invalid! If-None-Match value should be string.").toString());
//            }
//            JSONObject jsonObject = new JSONObject(plan);
//
//            if(!etagService.verifyETag(jsonObject, ifNotMatch)){
//                return ResponseEntity.ok().eTag(etag).body(jsonObject.toString());
//            } else {
//                return  ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
//            }
        	
        	String ifNotMatch;
            try{
                ifNotMatch = requestHeaders.getFirst("If-None-Match");
            } catch (Exception e){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                        body(new JSONObject().put("error", "ETag value is invalid! If-None-Match value should be string.").toString());
            }
            if(objectType.equals("plan")){
                String actualEtag = this.apiService.getEtag(key);
                if (ifNotMatch != null && ifNotMatch.equals(actualEtag)) {
                    return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(actualEtag).build();
                }
            }

            Map<String, Object> plan = this.apiService.getPlanObject(key);

            if (objectType.equals("plan")) {
                String actualEtag = this.apiService.getEtag(key);
                return ResponseEntity.ok().eTag(actualEtag).body(new JSONObject(plan).toString());
            }

            return ResponseEntity.ok().body(new JSONObject(plan).toString());
            
        }
    }
    
    // api to delete object from the redis
    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/{objectType}/{objectID}")
    public ResponseEntity<?> deletePlanObj(@RequestHeader HttpHeaders requestHeaders,@PathVariable String objectID, @PathVariable String objectType){
        String authorization = requestHeaders.getFirst("Authorization");
        String result = authorizeService.authorize(authorization);
        if(result != "Valid Token"){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", result).toString());
        }
        
        String key = objectType + ":" + objectID;

        if(!apiService.checkKeyExists(key)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("message", "ObjectId does not exists!").toString());
        }
        String actualEtag = apiService.getEtag(key);
        String eTag = requestHeaders.getFirst("If-Match");
        if (eTag == null || eTag.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("message", "eTag not provided in request!!").toString());
        }
        if (eTag != null && !eTag.equals(actualEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag)
                    .body(new JSONObject().put("message", "Plan has been updated by another user!!").toString());
        }
        
        Map<String, Object> plan = this.apiService.getPlanObject(key);

        Map<String, String> actionMap = new HashMap<>();
        actionMap.put("operation", "DELETE");
        actionMap.put("body",  new JSONObject(plan).toString());

        System.out.println("Sending message: " + actionMap);

        template.convertAndSend(DemoApplication.queueName, actionMap);
        
        
        apiService.deletePlan(key);
        return ResponseEntity.noContent().build();
    }
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE, path = "/plan/{objectID}")
    public ResponseEntity updatePlan( @RequestHeader HttpHeaders requestHeaders, @RequestBody(required = false) String jsonData,
                                             @PathVariable String objectID) throws IOException {

        String authorization = requestHeaders.getFirst("Authorization");
        String result = authorizeService.authorize(authorization);
        if(result != "Valid Token"){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", result).toString());
        }

        if (jsonData == null || jsonData.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new JSONObject().put("error", "Request body is Empty. Kindly provide the JSON").toString());
        }

        JSONObject jsonPlan = new JSONObject(jsonData);
        String key = "plan:" + objectID;

        if(!apiService.checkKeyExists(key)){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("message", "ObjectId does not exists!!").toString());
        }

        String actualEtag = apiService.getEtag(key);
        String eTag = requestHeaders.getFirst("If-Match");
        if (eTag == null || eTag.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("message", "eTag not provided in request!!").toString());
        }
        if (eTag != null && !eTag.equals(actualEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag)
                    .body(new JSONObject().put("message", "Plan has been updated by another user!!").toString());
        }

        JSONObject schemaJSON = new JSONObject(new JSONTokener(Objects.requireNonNull(ApiController.class.getResourceAsStream("/schema.json"))));
        Schema schema = SchemaLoader.load(schemaJSON);
        try {
            schema.validate(jsonPlan);
        } catch (ValidationException e) {
        	return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new JSONObject().put("error",e.getAllMessages()).toString());
        }
        
        Map<String, Object> plan = this.apiService.getPlanObject(key);

        Map<String, String> actionMap = new HashMap<>();
        actionMap.put("operation", "DELETE");
        actionMap.put("body",  new JSONObject(plan).toString());

        System.out.println("Sending message: " + actionMap);

        template.convertAndSend(DemoApplication.queueName, actionMap);
        

        apiService.deletePlan(key);
        String newEtag = apiService.saveObject(jsonPlan,key);
        
        Map<String, String> newActionMap = new HashMap<>();
        newActionMap.put("operation", "SAVE");
        newActionMap.put("body", jsonData);
        System.out.println("Sending message: " + newActionMap);

        template.convertAndSend(DemoApplication.queueName, newActionMap);

        return ResponseEntity.ok().eTag(newEtag)
                .body(new JSONObject().put("message: ", "Resource updated successfully!!").toString());
    }

    @RequestMapping(method =  RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE, path = "/plan/{objectID}")
    public ResponseEntity<Object> patchPlan(@RequestHeader HttpHeaders requestHeaders, @RequestBody(required = false) String jsonData,
                                            @PathVariable String objectID) throws IOException {

        String authorization = requestHeaders.getFirst("Authorization");
        String result = authorizeService.authorize(authorization);
        if(result != "Valid Token"){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new JSONObject().put("Error: ", result).toString());
        }

        if (jsonData == null || jsonData.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new JSONObject().put("error", "Request body is Empty. Kindly provide the JSON").toString());
        }

        JSONObject jsonPlan = new JSONObject(jsonData);
        String key = "plan:" + objectID;
        if (!apiService.checkKeyExists(key)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JSONObject().put("message", "ObjectId does not exists!!").toString());
        }

        String actualEtag = apiService.getEtag(key);
        String eTag = requestHeaders.getFirst("If-Match");
        if (eTag == null || eTag.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new JSONObject().put("message", "eTag not provided in request!!").toString());
        }
        if (eTag != null && !eTag.equals(actualEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag)
                    .body(new JSONObject().put("message", "Plan has been updated by another user!!").toString());
        }


        String newEtag = apiService.saveObject(jsonPlan,key);
        

        // index object
        Map<String, Object> plan = this.apiService.getPlanObject(key);

        Map<String, String> actionMap = new HashMap<>();
        actionMap.put("operation", "SAVE");
        actionMap.put("body", new JSONObject(plan).toString());

        System.out.println("Sending message: " + actionMap);

        template.convertAndSend(DemoApplication.queueName, actionMap);
        

        return ResponseEntity.ok().eTag(newEtag)
                .body(new JSONObject().put("message: ", "Resource updated successfully!!").toString());
    }

    
    
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE, value = "/token")
    public ResponseEntity<?> getToken(){

        String token;
        try {
            token = authorizeService.generateToken();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).
                    body(new JSONObject().put("error", e.getMessage()).toString());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(new JSONObject().put("token", token).toString());

    }
}
