package com.respiroc.webapp.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.respiroc.user.application.UserService
import com.respiroc.util.context.SpringUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import java.time.Duration
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
class WebSecurityConfig(
    @param:Value("\${jwt.secret}") private val secret: String
) {

    companion object {
        const val JWT_COOKIE_NAME = "token"
    }

    private val publicPaths = arrayOf(
        "/",
        "/assets/**",
        "/favicon.ico",
        "/auth/login",
        "/auth/signup",
        "/htmx/auth/login",
        "/htmx/auth/signup",
        "/error/**",
        "/actuator/**",
        "/api/voucher-reception"
    )

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        userService: UserService
    ): SecurityFilterChain {
        return http
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(*publicPaths).permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .csrf { it.disable() }
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { jwt ->
                        jwt
                            .jwtAuthenticationConverter(UserJwtAuthenticationConverter(userService))
                            .decoder(jwtDecoder())
                    }
                    .bearerTokenResolver(JwtCookieBearerTokenResolver())
                    .authenticationEntryPoint(JwtAuthenticationEntryPoint())
            }
            .build()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey()).build()

        // Set custom JWT token validator
        val withClockSkew: OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(
            JwtTimestampValidator(Duration.ofSeconds(0))
        )

        jwtDecoder.setJwtValidator(withClockSkew)

        return jwtDecoder
    }

    @Bean
    fun jwtEncoder(): JwtEncoder =
        NimbusJwtEncoder(ImmutableSecret(secretKey()))

    private fun secretKey(): SecretKey {
        val decoded = Base64.getDecoder().decode(secret)
        return SecretKeySpec(decoded, "HmacSHA256")
    }

    // --------------------------------
    // Inner classes
    // --------------------------------

    internal class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {

        override fun commence(
            request: HttpServletRequest,
            response: HttpServletResponse,
            authException: AuthenticationException
        ) {
            val expiredCookie = Cookie(JWT_COOKIE_NAME, null)
            expiredCookie.maxAge = 0
            expiredCookie.path = "/"
            expiredCookie.isHttpOnly = true
            response.addCookie(expiredCookie)

            response.sendRedirect("/auth/login")
        }
    }

    internal class UserJwtAuthenticationConverter(
        private val userService: UserService
    ) : Converter<Jwt, AbstractAuthenticationToken> {

        override fun convert(source: Jwt): AbstractAuthenticationToken? {
            val userId = source.subject.toLong()
            val tenantId = source.getClaim<Long>("tenantId")
            val ctx = userService.findByIdAndTenantId(userId, tenantId) ?: return null
            val principal = SpringUser(ctx)
            return UsernamePasswordAuthenticationToken(principal, source.tokenValue, principal.authorities)
        }
    }

    internal class JwtCookieBearerTokenResolver : BearerTokenResolver {

        override fun resolve(request: HttpServletRequest?): String? {
            val cookies = request?.cookies ?: return null
            return cookies.find { it.name == JWT_COOKIE_NAME }?.value
        }
    }
}