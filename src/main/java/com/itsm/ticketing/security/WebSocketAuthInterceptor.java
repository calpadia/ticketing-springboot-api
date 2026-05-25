package com.itsm.ticketing.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * WebSocket STOMP channel interceptor for JWT authentication.
 * Authenticates users during the CONNECT phase using a JWT token
 * passed via the "Authorization" STOMP header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract JWT token from STOMP "Authorization" header
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);

                try {
                    String userEmail = jwtUtils.extractUsername(jwt);

                    if (userEmail != null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                        if (jwtUtils.isTokenValid(jwt, userDetails)) {
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );
                            accessor.setUser(authToken);
                            log.debug("WebSocket authenticated user: {}", userEmail);
                        }
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication failed: {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket CONNECT without Authorization header");
            }
        }

        return message;
    }
}
