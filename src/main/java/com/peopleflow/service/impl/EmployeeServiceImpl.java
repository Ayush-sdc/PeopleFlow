package com.peopleflow.service.impl;

import com.peopleflow.dto.department.DepartmentSummaryDto;
import com.peopleflow.dto.employee.EmployeeRequest;
import com.peopleflow.dto.employee.EmployeeResponse;
import com.peopleflow.entity.Department;
import com.peopleflow.entity.Employee;
import com.peopleflow.exception.DuplicateResourceException;
import com.peopleflow.exception.ResourceNotFoundException;
import com.peopleflow.repository.DepartmentRepository;
import com.peopleflow.repository.EmployeeRepository;
import com.peopleflow.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Employee business logic.
 *
 * Architecture Notes:
 * - Constructor injection for loose coupling and high testability.
 * - Enforces business validations (email uniqueness across updates, department existence).
 * - Read-only transactions optimize Hibernate session flushing and DB lock overhead.
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new DuplicateResourceException("Employee with email '" + trimmedEmail + "' already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        Employee employee = new Employee();
        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setEmail(trimmedEmail);
        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setSalary(request.getSalary());
        employee.setDepartment(department);

        Employee saved = employeeRepository.save(employee);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        return mapToResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Long departmentId, String search, Pageable pageable) {
        String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        return employeeRepository.searchEmployees(departmentId, normalizedSearch, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        String trimmedEmail = request.getEmail().trim().toLowerCase();
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(trimmedEmail, id)) {
            throw new DuplicateResourceException("Another employee with email '" + trimmedEmail + "' already exists");
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.getDepartmentId()));

        employee.setFirstName(request.getFirstName().trim());
        employee.setLastName(request.getLastName().trim());
        employee.setEmail(trimmedEmail);
        employee.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        employee.setJobTitle(request.getJobTitle().trim());
        employee.setJoiningDate(request.getJoiningDate());
        employee.setSalary(request.getSalary());
        employee.setDepartment(department);

        Employee updated = employeeRepository.save(employee);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }

        // Isolated deletion logic: allows future migration to soft-delete without modifying caller contract
        employeeRepository.deleteById(id);
    }

    private EmployeeResponse mapToResponse(Employee emp) {
        DepartmentSummaryDto deptSummary = null;
        if (emp.getDepartment() != null) {
            deptSummary = new DepartmentSummaryDto(
                    emp.getDepartment().getId(),
                    emp.getDepartment().getName()
            );
        }

        return new EmployeeResponse(
                emp.getId(),
                emp.getFirstName(),
                emp.getLastName(),
                emp.getEmail(),
                emp.getPhone(),
                emp.getJobTitle(),
                emp.getJoiningDate(),
                emp.getSalary(),
                deptSummary,
                emp.getCreatedAt(),
                emp.getUpdatedAt()
        );
    }
}