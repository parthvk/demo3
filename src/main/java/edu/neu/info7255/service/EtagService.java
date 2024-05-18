package edu.neu.info7255.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

// service to manage Etag operations
@Service
public class EtagService {

	// function to generate the Etag
	public String getEtag(JSONObject obj) {
		String encoded=null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(obj.toString().getBytes(StandardCharsets.UTF_8));
            encoded = Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "\""+encoded+"\"";
	}
	
	// function to verify the Etag
	public boolean verifyETag(JSONObject json, List<String> etags) {
        if(etags.isEmpty())
            return false;
        String encoded=getEtag(json);
        return etags.contains(encoded);

    }
}
