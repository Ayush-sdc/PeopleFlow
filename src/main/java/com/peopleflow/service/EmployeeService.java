package com.peopleflow.service;

import com.peopleflow.dto.employee.EmployeeRequest;
import com.peopleflow.dto.employee.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeRequest request);

    EmployeeResponse getEmployeeById(Long id);

    Page<EmployeeResponse> getAllEmployees(Long departmentId, String search, Pageable pageable);

    EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

    void deleteEmployee(Long id);
}