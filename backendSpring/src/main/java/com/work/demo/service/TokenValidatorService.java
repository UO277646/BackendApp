package com.work.demo.service;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.work.demo.exceptions.InvalidParameterException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;

@Service
public class TokenValidatorService {
    private static final String CLIENT_ID="1043590116943-7mn3qj6i76kuvmv65sb1nbfp2sml5lo4.apps.googleusercontent.com";

    public String verify(String idTokenString) throws GeneralSecurityException, IOException {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            // Generar el JWT personalizado
            String jwt = Jwts.builder()
                    .setSubject(email)  // Usa el email como identificador
                    .setIssuedAt(new Date())  // Fecha de emisión
                    .setExpiration(new Date(System.currentTimeMillis() + 86400000))  // Expira en 24 horas
                    .signWith(SignatureAlgorithm.HS256, "juaneselmejor47gmailcomjuaneselmejor47gmailcom")  // Usa una clave secreta para firmar
                    .compact();

            return jwt;  // Devolver el JWT al frontend
        } else {
            throw new InvalidParameterException("Invalid ID token");
        }
    }
    public Boolean verifyCamera(String idTokenString) throws GeneralSecurityException, IOException {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(CLIENT_ID))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        return idToken!=null;
    }
}
