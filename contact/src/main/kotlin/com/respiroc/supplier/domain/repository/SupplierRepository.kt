package com.respiroc.supplier.domain.repository

import com.respiroc.supplier.domain.model.Supplier
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SupplierRepository : CustomJpaRepository<Supplier, Long> {

    @Query(
        """
        SELECT s FROM Supplier s
        LEFT JOIN FETCH s.company company
        LEFT JOIN FETCH s.person person
        """
    )
    fun findSuppliers(): List<Supplier>

    @Query(
        """
        SELECT s FROM Supplier s
        LEFT JOIN FETCH s.company company
        LEFT JOIN FETCH s.person person
        WHERE (
                (person.name ILIKE '%' || :name || '%') 
                OR 
                (company.name ILIKE '%' || :name || '%')
            )
    """
    )
    fun findSuppliersByNameContainingIgnoreCase(
        @Param("name") name: String
    ): List<Supplier>

    fun existsSuppliersByCompany_NameAndCompany_OrganizationNumber(companyName: String, companyOrganizationNumber: String): Boolean

    fun existsSuppliersByPerson_Name(personName: String): Boolean
}