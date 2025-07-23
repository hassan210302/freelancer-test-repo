package com.respiroc.customer.domain.repository

import com.respiroc.customer.domain.model.Customer
import com.respiroc.util.repository.CustomJpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface CustomerRepository : CustomJpaRepository<Customer, Long> {

    @Query(
        """
        SELECT c FROM Customer c
        LEFT JOIN FETCH c.company company
        LEFT JOIN FETCH c.person person
    """
    )
    fun findCustomers(): List<Customer>

    @Query(
        """
        SELECT c FROM Customer c
        LEFT JOIN FETCH c.company company
        LEFT JOIN FETCH c.person person
        WHERE (
                (person.name ILIKE '%' || :name || '%') 
                OR 
                (company.name ILIKE '%' || :name || '%')
            )
    """
    )
    fun findCustomersByNameContainingIgnoreCase(
        @Param("name") name: String
    ): List<Customer>

    fun existsCustomersByCompany_NameAndCompany_OrganizationNumber(
        companyName: String,
        companyOrganizationNumber: String,
    ): Boolean

    fun existsCustomersByPerson_Name(personName: String): Boolean
}