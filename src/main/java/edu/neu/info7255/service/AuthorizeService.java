package edu.neu.info7255.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
public class AuthorizeService {

    static RSAKey rsaPublicJWK = null;
    static RSAKey rsaJWK = null;
     static {
         try {

             // RSA signatures require a public and private RSA key pair, the public key
             // must be made known to the JWS recipient in order to verify the signatures
             rsaJWK = new RSAKeyGenerator(2048)
                     .keyID("123")
                     .generate();
             rsaPublicJWK = rsaJWK.toPublicJWK();

         } catch (JOSEException e) {
             e.printStackTrace();
         }

     }

    public String generateToken() throws JOSEException {


        // Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(rsaJWK);

        // Prepare JWT with claims set
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .expirationTime(new Date(new Date().getTime() + 3000 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
                claimsSet);

        // Compute the RSA signature
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }

    public String authorize(String authorization){

        if(authorization == null || authorization.isEmpty()){
            return "No Token Found";
        }

        if (!authorization.contains("Bearer ")) {
            return "Improper Format of Token";
        }

        String token = authorization.split(" ")[1];

            try {

                SignedJWT signedJWT = SignedJWT.parse(token);
                JWSVerifier verifier = new RSASSAVerifier(this.rsaPublicJWK);

                // token is not valid
                if(!signedJWT.verify(verifier)){
                    return "Invalid Token";
                }

                Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();

                // token ttl has expired
                if(new Date().after(expirationTime)) {
                    return "Token has expired";
                }
            } catch (JOSEException | ParseException e ){
                return "Invalid Token";
            }

        return "Valid Token";
    }
}
