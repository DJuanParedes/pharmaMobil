package pe.edu.upeu.PharmaBackend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Contrato del filtro. La bitácora de dispositivo complementa estas pruebas con HTTP real. */
class JwtEscenariosTest {
    private String secret;
    private JwtService jwt;
    private UserDetailsService usuarios;
    private UserDetails usuario;

    @BeforeEach void preparar() {
        byte[] bytes = new byte[48]; new SecureRandom().nextBytes(bytes);
        secret = Base64.getEncoder().encodeToString(bytes);
        jwt = new JwtService(secret, 60);
        usuarios = mock(UserDetailsService.class);
        usuario = User.withUsername("admin").password("hash-de-prueba").authorities("CATALOGO_LEER").build();
        when(usuarios.loadUserByUsername("admin")).thenReturn(usuario);
        SecurityContextHolder.clearContext();
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    private MockHttpServletResponse ejecutar(String encabezado) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/productos");
        if (encabezado != null) request.addHeader("Authorization", encabezado);
        var response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(jwt, usuarios).doFilter(request, response, (req, res) -> {
            if (SecurityContextHolder.getContext().getAuthentication() == null)
                new RestAuthenticationEntryPoint().commence(request, response, new InsufficientAuthenticationException("No autenticado"));
            else response.setStatus(200);
        });
        return response;
    }
    private void error(String header, String message) throws Exception {
        var response = ejecutar(header);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"message\":\"" + message + "\""));
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
    }
    @Test void tokenValido() throws Exception { assertEquals(200, ejecutar("Bearer " + jwt.generarToken(usuario)).getStatus()); }
    @Test void tokenExpirado() throws Exception {
        error("Bearer " + new JwtService(secret, -1).generarToken(usuario), "El token ha expirado. Inicie sesión nuevamente");
    }
    @Test void firmaAlterada() throws Exception {
        String token = jwt.generarToken(usuario); int i = token.lastIndexOf('.') + 1;
        token = token.substring(0,i) + (token.charAt(i)=='A' ? 'B' : 'A') + token.substring(i+1);
        error("Bearer " + token, "El token no es válido");
    }
    @Test void sinCabecera() throws Exception { error(null,"Debe iniciar sesión para acceder a este recurso"); }
    @Test void prefijoMinusculo() throws Exception { error("bearer " + jwt.generarToken(usuario),"Debe iniciar sesión para acceder a este recurso"); }
    @Test void deshabilitadoDespuesDelLogin() throws Exception {
        String token = jwt.generarToken(usuario);
        when(usuarios.loadUserByUsername("admin")).thenReturn(User.withUserDetails(usuario).disabled(true).build());
        error("Bearer " + token,"El usuario está deshabilitado");
    }
}
