package com.tarbi.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tarbi.service.TokenBlacklistService;
import com.tarbi.util.CustomUserDetailsService;
import com.tarbi.util.JwtUtil;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	private TokenBlacklistService tokenBlacklistService;

	@Autowired
	private CustomUserDetailsService userDetailsService;
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		final String authHeader=request.getHeader("Authorization");
		String username=null;
		String jwt=null;
		
		if(authHeader!=null &&authHeader.startsWith("Bearer ")) {
			jwt=authHeader.substring(7);
			
			if(tokenBlacklistService.isTokenBlacklisted(jwt)) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.getWriter().write("Token is blacklisted");
				return;
			}
			
			try {
				username=jwtUtil.extractUsername(jwt);
			}
			catch(ExpiredJwtException e) {
				logger.warn("JWT expired: "+e.getMessage());
				 response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	             response.getWriter().write("Token has expired");
	             return;
				
			}
			catch(Exception e) {
				logger.error("Error parsing JWT: "+e.getMessage());
				 response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	             response.getWriter().write("Invalid token");
	             return;
			}
		}
	
	
	if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null) {
		if(jwtUtil.validateToken(jwt, username)) {
			
			UserDetails userDetails=userDetailsService.loadUserByUsername(username);
			
			UsernamePasswordAuthenticationToken authToken=
					new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
			
			authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			
			SecurityContextHolder.getContext().setAuthentication(authToken);

			
		}
	}
	
	filterChain.doFilter(request, response);
	
	
	
	
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
	    return request.getServletPath().equals("/auth/login");
	}
	
	
	
	
	

}
