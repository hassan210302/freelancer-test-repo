package com.respiroc.user.domain.model

import com.respiroc.tenant.domain.model.Tenant
import jakarta.persistence.*

@Entity
@Table(name = "user_tenants")
open class UserTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    open val id: Long = -1

    @Column(name = "user_id", nullable = false)
    open var userId: Long = -1

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, insertable = false)
    open lateinit var user: User

    @Column(name = "tenant_id", nullable = false, updatable = false)
    open var tenantId: Long = -1

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false, insertable = false)
    open lateinit var tenant: Tenant

    @OneToMany(mappedBy = "userTenant", orphanRemoval = true, fetch = FetchType.LAZY)
    open var roles: MutableSet<UserTenantRole> = HashSet()
}