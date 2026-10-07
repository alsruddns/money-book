package com.moneybook.backend.accountmanagement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PasswordChangeRequiredInterceptorTests {
 private final PasswordChangeRequiredInterceptor interceptor=new PasswordChangeRequiredInterceptor();
 @AfterEach void clear(){SecurityContextHolder.clearContext();}

 @Test void temporaryPasswordCannotAccessBusinessApis() throws Exception{
  SecurityContextHolder.getContext().setAuthentication(forced());
  MockHttpServletRequest request=new MockHttpServletRequest("GET","/api/money-books");request.setContextPath("/api");
  MockHttpServletResponse response=new MockHttpServletResponse();
  assertFalse(interceptor.preHandle(request,response,new Object()));assertEquals(403,response.getStatus());
  assertTrue(response.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
 }

 @Test void temporaryPasswordCanChangePasswordAndLogout() throws Exception{
  SecurityContextHolder.getContext().setAuthentication(forced());
  MockHttpServletRequest password=new MockHttpServletRequest("PATCH","/api/account/password");password.setContextPath("/api");
  MockHttpServletRequest logout=new MockHttpServletRequest("POST","/api/auth/logout");logout.setContextPath("/api");
  assertTrue(interceptor.preHandle(password,new MockHttpServletResponse(),new Object()));
  assertTrue(interceptor.preHandle(logout,new MockHttpServletResponse(),new Object()));
 }

 private JwtAuthenticationToken forced(){Jwt jwt=Jwt.withTokenValue("test").header("alg","none").subject("5").build();return new JwtAuthenticationToken(jwt,List.of(new SimpleGrantedAuthority("PASSWORD_CHANGE_REQUIRED")));}
}
