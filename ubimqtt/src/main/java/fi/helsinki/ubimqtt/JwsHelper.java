package fi.helsinki.ubimqtt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;

import java.security.interfaces.ECPrivateKey;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

public class JwsHelper {


    public static boolean verifySignature(String json, String publicKey) throws java.text.ParseException, IOException, JOSEException, ParseException {
        // Parse the EC key pair

    return verifySignatureCompact(jsonToCompact(json), publicKey);
    }

    public static boolean verifySignatureCompact(String compact, String publicKey) throws java.text.ParseException, IOException, JOSEException {
        // Parse the EC key pair
        //PEMParser pemParser = new PEMParser(new InputStreamReader(new FileInputStream("ec512-key-pair.pem")));
        PEMParser pemParser = new PEMParser(new StringReader(publicKey));
        SubjectPublicKeyInfo pemPublicKey = (SubjectPublicKeyInfo)pemParser.readObject();

        // Convert to Java (JCA) format
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        ECPublicKey ecPublicKey = (ECPublicKey)converter.getPublicKey(pemPublicKey);

        pemParser.close();

        // Get private + public EC key
        //ECPrivateKey ecPrivateKey = (ECPrivateKey)keyPair.getPrivate();



        String[] parts = compact.split("\\.");

        Base64URL header = new Base64URL(parts[0]);
        Base64URL payload = new Base64URL(parts[1]);
        Base64URL signature = new Base64URL(parts[2]);

        JWSObject jwsObject = new JWSObject(header, payload, signature);
        JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

        return jwsObject.verify(verifier);
    }

    public static String signMessage( String message, String privateKey) throws JOSEException, PEMException, IOException {
        return compactToJson(signMessageToCompact(message, privateKey));
    }

    public static String signMessageToCompact( String message, String privateKey) throws JOSEException, PEMException, IOException {
        //ECKey jwk = (ECKey) ECKey.parseFromPEMEncodedObjects(pemEncodedRSAPrivateKey);

        // Parse the EC key pair
        //PEMParser pemParser = new PEMParser(new InputStreamReader(new FileInputStream("ec512-key-pair.pem")));
        PEMParser pemParser = new PEMParser(new StringReader(privateKey));
        PEMKeyPair pemKeyPair = (PEMKeyPair)pemParser.readObject();

        // Convert to Java (JCA) format
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair = converter.getKeyPair(pemKeyPair);
        pemParser.close();

        // Get private + public EC key
        ECPrivateKey ecPrivateKey = (ECPrivateKey)keyPair.getPrivate();

        //ECPublicKey publicKey = (ECPublicKey)keyPair.getPublic();

        // Sign test
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.ES512), new Payload(message));
        jwsObject.sign(new ECDSASigner(ecPrivateKey));


        /*
        JWSSigner signer = new ECDSASigner(ecKey);

        JWSObject jwsObject = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(ecKey.getKeyID()).build(),
                new Payload(message));

        jwsObject.sign(signer);
        */

        String s = jwsObject.serialize();

        return s;
    }

    public static String compactToJson(String compact) {
        String[] parts = compact.split("\\.");

        String header = new Base64URL(parts[0]).decodeToString();
        String payload = new Base64URL(parts[1]).decodeToString();
        String signature = parts[2];

        System.out.println("header: " +header);
        System.out.println("payload: " +payload);
        System.out.println("signature: " +signature);

        JSONObject obj = new JSONObject();

        JSONObject signatureObject = new JSONObject();

        signatureObject.put("protected", header);
        signatureObject.put("signature", signature);

        JSONArray signaturesArray = new JSONArray();
        signaturesArray.add(signatureObject);

        obj.put("payload", payload);
        obj.put("signatures", signaturesArray);

        return obj.toJSONString();
    }

  public static String jsonToCompact(String json) throws ParseException {
      JSONParser parser = new JSONParser();
      JSONObject obj = (JSONObject) parser.parse(json);

      String payload = (String)obj.get("payload");

      JSONArray signaturesArray = (JSONArray)obj.get("signatures");
      JSONObject signatureObject = (JSONObject)signaturesArray.get(0);

      String header = (String)signatureObject.get("protected");
      String signature = (String)signatureObject.get("signature");


      String compact = Base64URL.encode(header)+"."+Base64URL.encode(payload)+"."+signature;
      return compact;
    }
}
