package com.peopleflow.service.impl;

import com.peopleflow.dto.department.DepartmentRequest;
import com.peopleflow.dto.department.DepartmentResponse;
import com.peopleflow.entity.Department;
import com.peopleflow.exception.DuplicateResourceException;
import com.peopleflow.exception.ResourceNotFoundException;
import com.peopleflow.repository.DepartmentRepository;
import com.peopleflow.repository.EmployeeRepository;
import com.peopleflow.service.DepartmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for Department business logic.
 *
 * Architecture Notes:
 * - Constructor injection guarantees immutable, mock-friendly dependencies.
 * - @Transactional ensures database atomicity and rollbacks on runtime exceptions.
 * - Prevents deleting departments containing employees to preserve referential integrity.
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository,
                                 EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        String trimmedName = request.getName().trim();
        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Department with name '" + trimmedName + "' already exists");
        }

        Department department = new Department();
        department.setName(trimmedName);
        department.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Department saved = departmentRepository.save(department);
        return mapToResponse(saved, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        long employeeCount = employeeRepository.countByDepartmentId(id);
        return mapToResponse(department, employeeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getAllDepartments(Pageable pageable) {
        return departmentRepository.findAll(pageable)
                .map(dept -> {
                    long count = employeeRepository.countByDepartmentId(dept.getId());
                    return mapToResponse(dept, count);
                });
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        String trimmedName = request.getName().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Another department with name '" + trimmedName + "' already exists");
        }

        department.setName(trimmedName);
        department.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Department updated = departmentRepository.save(department);
        long count = employeeRepository.countByDepartmentId(id);
        return mapToResponse(updated, count);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }

        if (employeeRepository.existsByDepartmentId(id)) {
            throw new IllegalStateException(
                    "Cannot delete department with id " + id + " because active employees are assigned to it");
        }

        departmentRepository.deleteById(id);
    }

    private DepartmentResponse mapToResponse(Department dept, long employeeCount) {
        return new DepartmentResponse(
                dept.getId(),
                dept.getName(),
                dept.getDescription(),
                employeeCount,
                dept.getCreatedAt(),
                dept.getUpdatedAt()
        );
    }
}