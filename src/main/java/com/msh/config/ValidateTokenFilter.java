package com.msh.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

public class ValidateTokenFilter extends OncePerRequestFilter {
    Locale currentLocale = new Locale("fa");


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("************* getLocalPort : " + request.getLocalPort());
        System.out.println("************* getRemotePort : " + request.getRemotePort());
        System.out.println("************* getLocalAddr : " + request.getLocalAddr());
        System.out.println("************* getRemoteAddr : " + request.getRemoteAddr());
        System.out.println("************* FORWARDED : " + request.getHeader("X-FORWARDED-FOR"));
        filterChain.doFilter(request, response);
    }


}
