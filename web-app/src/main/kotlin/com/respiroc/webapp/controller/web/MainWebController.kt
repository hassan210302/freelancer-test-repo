package com.respiroc.webapp.controller.web

import com.respiroc.webapp.controller.BaseController
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class MainWebController() : BaseController() {

    @GetMapping("/")
    fun home(): String {
        return if (isUserLoggedIn()) {
            "redirect:/dashboard"
        } else {
            "redirect:/auth/login"
        }
    }

    @GetMapping("/dashboard")
    fun dashboard(
        model: Model
    ): String {
        addCommonAttributesForCurrentTenant(model, "Dashboard")
        return "dashboard/index"
    }
} 