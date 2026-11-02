package com.peopleflow.repository;

import com.peopleflow.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByDepartmentId(Long departmentId);

    long countByDepartmentId(Long departmentId);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.department " +
           "WHERE (:departmentId IS NULL OR e.department.id = :departmentId) " +
           "AND (:search IS NULL OR " +
           "     LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Employee> searchEmployees(@Param("departmentId") Long departmentId,
                                  @Param("search") String search,
                                  Pageable pageable);
}