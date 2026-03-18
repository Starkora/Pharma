package com.pharmasys.controller;

import com.pharmasys.dto.request.LoginRequestDto;
import com.pharmasys.model.Usuario;
import com.pharmasys.security.JwtUtil;
import com.pharmasys.service.UsuarioService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String AUTH_COOKIE_NAME = "authToken";
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @Value("${app.security.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${app.security.cookie.same-site:Lax}")
    private String sameSite;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto loginRequest, HttpServletResponse response) {
        try {
            // Autenticar usuario
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            // Generar token JWT
            String token = jwtUtil.generateToken(loginRequest.getUsername());

            response.addHeader(HttpHeaders.SET_COOKIE, buildAuthCookie(token).toString());
            
            // Obtener datos del usuario
            Usuario usuario = usuarioService.buscarPorUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Crear respuesta
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Login exitoso");
            responseBody.put("usuario", crearUsuarioResponse(usuario));
            
            return ResponseEntity.ok(responseBody);
            
        } catch (BadCredentialsException e) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", false);
            responseBody.put("message", "Credenciales inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
        } catch (Exception e) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", false);
            responseBody.put("message", "No se pudo completar el login");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseBody);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, clearAuthCookie().toString());
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", "Sesión cerrada exitosamente");
        
        return ResponseEntity.ok(responseBody);
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validate(Authentication authentication, HttpServletResponse response) {
        if (authentication == null || !authentication.isAuthenticated()) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearAuthCookie().toString());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", false);
            responseBody.put("message", "Sesión no válida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
        }

        try {
            Usuario usuario = usuarioService.buscarPorUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (!usuario.getActivo()) {
                response.addHeader(HttpHeaders.SET_COOKIE, clearAuthCookie().toString());

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("success", false);
                responseBody.put("message", "Usuario inactivo");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(responseBody);
            }

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("usuario", crearUsuarioResponse(usuario));

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            response.addHeader(HttpHeaders.SET_COOKIE, clearAuthCookie().toString());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", false);
            responseBody.put("message", "Sesión no válida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
        }
    }
    
    private Map<String, Object> crearUsuarioResponse(Usuario usuario) {
        Map<String, Object> usuarioData = new HashMap<>();
        usuarioData.put("id", usuario.getId());
        usuarioData.put("username", usuario.getUsername());
        usuarioData.put("nombre", usuario.getNombre());
        usuarioData.put("apellido", usuario.getApellido());
        usuarioData.put("email", usuario.getEmail());
        usuarioData.put("telefono", usuario.getTelefono());
        usuarioData.put("roles", usuario.getRoles());
        usuarioData.put("activo", usuario.getActivo());
        return usuarioData;
    }

    private ResponseCookie buildAuthCookie(String token) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/")
            .maxAge(jwtUtil.getExpirationTime() / 1000)
            .build();
    }

    private ResponseCookie clearAuthCookie() {
        return ResponseCookie.from(AUTH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/")
            .maxAge(0)
            .build();
    }
}
