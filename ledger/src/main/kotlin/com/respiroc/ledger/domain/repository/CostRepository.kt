package com.respiroc.ledger.domain.repository

import com.respiroc.ledger.domain.model.Cost
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CostRepository : CustomJpaRepository<Cost, Long> 