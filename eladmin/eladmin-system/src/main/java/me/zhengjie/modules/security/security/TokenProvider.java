/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.modules.security.security;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zhengjie.modules.security.config.SecurityProperties;
import me.zhengjie.modules.security.service.dto.JwtUserDto;
import me.zhengjie.utils.RedisUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author /
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenProvider implements InitializingBean {

    private Key signingKey;
    private JwtParser jwtParser;
    private final RedisUtils redisUtils;
    private final SecurityProperties properties;
    public static final String AUTHORITIES_UUID_KEY = "uid";
    public static final String AUTHORITIES_UID_KEY = "userId";

    @Override
    public void afterPropertiesSet() {
        // Decode Base64 key and create signing key
        byte[] keyBytes = Decoders.BASE64.decode(properties.getBase64Secret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        // Initialize JwtParser
        jwtParser = Jwts.parserBuilder()
                .setSigningKey(signingKey) // Use pre-generated signing key
                .build();
    }

    /**
     * Create token set to never expire,
     * Token expiration is maintained in Redis
     * @param user /
     * @return /
     */
    public String createToken(JwtUserDto user) {
        // Set parameters
        Map<String, Object> claims = new HashMap<>(6);
        // Set user ID
        claims.put(AUTHORITIES_UID_KEY, user.getUser().getId());
        // Set UUID to ensure each token is unique
        claims.put(AUTHORITIES_UUID_KEY, IdUtil.simpleUUID());
        // Call Jwts.builder() directly to create new instance
        return Jwts.builder()
                // Set custom claims
                .setClaims(claims)
                // Set subject
                .setSubject(user.getUsername())
                // Sign with pre-generated signing key and algorithm
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Get authentication information based on token
     *
     * @param token /
     * @return /
     */
    Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        User principal = new User(claims.getSubject(), "******", new ArrayList<>());
        return new UsernamePasswordAuthenticationToken(principal, token, new ArrayList<>());
    }

    public Claims getClaims(String token) {
        return jwtParser
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * @param token Token to check
     */
    public void checkRenewal(String token) {
        // Check whether to renew token, calculate expiration time
        String loginKey = loginKey(token);
        long time = redisUtils.getExpire(loginKey) * 1000;
        Date expireDate = DateUtil.offset(new Date(), DateField.MILLISECOND, (int) time);
        // Calculate difference between current time and expiration time
        long differ = expireDate.getTime() - System.currentTimeMillis();
        // Renew if within renewal check window
        if (differ <= properties.getDetect()) {
            long renew = time + properties.getRenew();
            redisUtils.expire(loginKey, renew, TimeUnit.MILLISECONDS);
        }
    }

    public String getToken(HttpServletRequest request) {
        final String requestHeader = request.getHeader(properties.getHeader());
        if (requestHeader != null && requestHeader.startsWith(properties.getTokenStartWith())) {
            return requestHeader.substring(7);
        }
        return null;
    }

    /**
     * Get login user Redis key
     * @param token /
     * @return key
     */
    public String loginKey(String token) {
        Claims claims = getClaims(token);
        return properties.getOnlineKey() + claims.getSubject() + ":" + getId(token);
    }

    /**
     * Get session ID
     * @param token /
     * @return /
     */
    public String getId(String token) {
        Claims claims = getClaims(token);
        return claims.get(AUTHORITIES_UUID_KEY, String.class);
    }
}
