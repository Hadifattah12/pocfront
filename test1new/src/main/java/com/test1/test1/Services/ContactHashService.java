package com.test1.test1.Services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Service
public class ContactHashService {

    private final SecretKeySpec keySpec;

    public ContactHashService(@Value("${contact-sync.hash-secret}") String secret) {
        //create key object from the secret string
//      SecretKeySpec represents the key in binary form (byte[]) and associates it with an algorithm
        this.keySpec = new SecretKeySpec(
                //defines how to convert text → bytes
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    public byte[] hashPhoneNumber(String normalizedPhoneNumber) {
        try {
            //Message Authentication Code => HMAC => SECRET
            Mac mac = Mac.getInstance("HmacSHA256");

            //use this secret key for hashing
            mac.init(keySpec);

            //now hashing happens
            return mac.doFinal(normalizedPhoneNumber.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("FAILED_TO_HASH_PHONE_NUMBER", e);
        }
    }
}