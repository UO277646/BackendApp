package com.work.demo.rest;

import com.google.api.client.auth.oauth2.TokenRequest;
import com.work.demo.rest.dto.ProyectoApiDto;
import com.work.demo.rest.dto.TokenRequestDto;
import com.work.demo.service.ProyectoService;
import com.work.demo.service.TokenValidatorService;
import com.work.demo.service.dto.ProyectoServiceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    private TokenValidatorService tokenValidatorService;
    @Autowired
    private ProyectoService proyectoService;
    @PostMapping("/verify")
    public TokenRequestDto verifyToken(@RequestBody TokenRequestDto tokenRequest) {
        try {

            // Llama al servicio que valida el token y genera el JWT
            String jwt = tokenValidatorService.verify(tokenRequest.getToken());
            TokenRequestDto t=new TokenRequestDto();
            t.setToken(jwt);
            if (jwt != null) {
                return t;
            } else {
                return null;
            }
        } catch (GeneralSecurityException | IOException e) {
            // Manejar posibles excepciones relacionadas con la verificación del token
            return null;
        }
    }
    @GetMapping("/check/{proyectId}/{email}")
    public Boolean getProyectosByEmail(@PathVariable Long proyectId, @PathVariable String email) {
        return proyectoService.checkProject(proyectId,email);

    }
}
