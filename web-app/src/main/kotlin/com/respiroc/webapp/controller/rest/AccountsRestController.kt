package com.respiroc.webapp.controller.rest

import com.respiroc.ledger.application.AccountService
import com.respiroc.ledger.domain.model.Account
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountsRestController(
    private val accountService: AccountService
) {
    @GetMapping("/api/accounts")
    fun allAccounts(): List<Account> {
        return accountService.findAllAccounts().sortedBy { it.noAccountNumber }.toList()
    }
}