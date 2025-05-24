package com.jangid.forging_process_management_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
@WebFilter("/*")
public class SimpleCORSFilter implements Filter {

  @Value("${frontend.url}")
  private String frontendUrl;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Get the origin from the request
    String origin = httpRequest.getHeader("Origin");
    
    // Check if the origin is allowed
    if (origin != null && (origin.equals(frontendUrl) || 
                          origin.equals("http://www.fopmas.com") || 
                          origin.equals("http://fopmas.com") ||
                          origin.equals("http://91.108.105.97") ||
                          origin.equals("http://91.108.105.97:80"))) {
      httpResponse.setHeader("Access-Control-Allow-Origin", origin);
    }
    
    httpResponse.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
    httpResponse.setHeader("Access-Control-Max-Age", "3600");
    httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
    httpResponse.setHeader("Access-Control-Expose-Headers", "Authorization");
    httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
    
    if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
      httpResponse.setStatus(HttpServletResponse.SC_OK);
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void init(FilterConfig filterConfig) {
  }

  @Override
  public void destroy() {
  }
}
