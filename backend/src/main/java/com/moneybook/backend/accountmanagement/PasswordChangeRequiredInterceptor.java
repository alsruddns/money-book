package com.moneybook.backend.accountmanagement;

import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.nio.charset.StandardCharsets;

/** Blocks all protected application routes while a temporary password must be changed. */
@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {
 @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler)throws java.io.IOException {
  Authentication a=SecurityContextHolder.getContext().getAuthentication();
  if(!(a instanceof JwtAuthenticationToken)||a.getAuthorities().stream().noneMatch(x->x.getAuthority().equals("PASSWORD_CHANGE_REQUIRED"))||allowed(request))return true;
  response.setStatus(403);response.setHeader("Cache-Control","no-store");response.setCharacterEncoding(StandardCharsets.UTF_8.name());response.setContentType("application/json");
  response.getWriter().write("{\"code\":\"PASSWORD_CHANGE_REQUIRED\",\"message\":\"계속하려면 먼저 임시 비밀번호를 변경해야 합니다.\"}");return false;
 }
 private boolean allowed(HttpServletRequest r){String path=r.getRequestURI().substring(r.getContextPath().length());String method=r.getMethod();
  return ("PATCH".equals(method)&&"/account/password".equals(path))
   ||("GET".equals(method)&&("/account/me".equals(path)||"/auth/me".equals(path)))
   ||("POST".equals(method)&&("/auth/logout".equals(path)||"/account/sessions/logout-all".equals(path)));
 }
}
