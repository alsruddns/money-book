package com.moneybook.backend.recovery;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Generates high-entropy one-time recovery material and fixed-length digests. */
@Component
public class RecoverySecretGenerator {
    private static final char[] ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom random=new SecureRandom();
    public List<String> newRecoveryCodes(int count){List<String> out=new ArrayList<>(count);for(int i=0;i<count;i++){StringBuilder raw=new StringBuilder(20);for(int j=0;j<20;j++)raw.append(ALPHABET[random.nextInt(ALPHABET.length)]);out.add(raw.substring(0,4)+"-"+raw.substring(4,8)+"-"+raw.substring(8,12)+"-"+raw.substring(12,16)+"-"+raw.substring(16));}return out;}
    public String newNumericCode(){return "%06d".formatted(random.nextInt(1_000_000));}
    public String newGrant(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public String newTemporaryPassword(){StringBuilder value=new StringBuilder(18);for(int i=0;i<18;i++)value.append(ALPHABET[random.nextInt(ALPHABET.length)]);return value.substring(0,6)+"-"+value.substring(6,12)+"-"+value.substring(12);}
    public String sha256(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
