package com.respiroc.webapp.config

import com.respiroc.user.application.UserService
import com.respiroc.webapp.service.JwtService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import jakarta.servlet.http.Cookie
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User


@Component
class GoogleOAuth2SuccessHandler(
    private val userService: UserService,
    private val jwtService: JwtService
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User
        val email = oauth2User.attributes["email"] as? String
            ?: throw IllegalStateException("Email not found in Google OAuth2 response")

        val user = userService.loginOrSignupOAuth2(email)
        val token = jwtService.generateToken(subject = user.id.toString(), tenantId = user.tenantId)

        val jwtCookie = Cookie("token", token)
        jwtCookie.isHttpOnly = true
        jwtCookie.secure = false
        jwtCookie.path = "/"
        jwtCookie.maxAge = 24 * 60 * 60
        response.addCookie(jwtCookie)
        response.sendRedirect("/")

    }
}
