package com.respiroc.ledger.domain.repository

import com.respiroc.ledger.domain.model.ExpenseCategory
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ExpenseCategoryRepository : CustomJpaRepository<ExpenseCategory, Long> {

    @Query("SELECT ec FROM ExpenseCategory ec WHERE ec.isActive = true ORDER BY ec.name")
    fun findAllActive(): List<ExpenseCategory>

    @Query("SELECT ec FROM ExpenseCategory ec WHERE ec.name = :name")
    fun findByName(name: String): ExpenseCategory?
}