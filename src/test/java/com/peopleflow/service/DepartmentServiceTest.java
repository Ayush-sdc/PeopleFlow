package com.peopleflow.service;

import com.peopleflow.dto.department.DepartmentRequest;
import com.peopleflow.dto.department.DepartmentResponse;
import com.peopleflow.entity.Department;
import com.peopleflow.exception.DuplicateResourceException;
import com.peopleflow.exception.ResourceNotFoundException;
import com.peopleflow.repository.DepartmentRepository;
import com.peopleflow.repository.EmployeeRepository;
import com.peopleflow.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department department;
    private DepartmentRequest request;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Software development team");
        department.setId(1L);
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());

        request = new DepartmentRequest("Engineering", "Software development team");
    }

    @Test
    @DisplayName("Create Department: Success")
    void createDepartment_Success() {
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        DepartmentResponse response = departmentService.createDepartment(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Engineering");
        assertThat(response.getEmployeeCount()).isZero();
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("Create Department: Duplicate Name Throws DuplicateResourceException")
    void createDepartment_DuplicateName_ThrowsException() {
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.createDepartment(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(departmentRepository, never()).save(any(Department.class));
    }

    @Test
    @DisplayName("Find Department By ID: Success")
    void getDepartmentById_Success() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(5L);

        DepartmentResponse response = departmentService.getDepartmentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Engineering");
        assertThat(response.getEmployeeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Find Department By ID: Nonexistent ID Throws ResourceNotFoundException")
    void getDepartmentById_NotFound_ThrowsException() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getDepartmentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Get All Departments: Returns Paginated List")
    void getAllDepartments_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Department> deptPage = new PageImpl<>(List.of(department), pageable, 1);

        when(departmentRepository.findAll(pageable)).thenReturn(deptPage);
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(2L);

        Page<DepartmentResponse> result = departmentService.getAllDepartments(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Engineering");
        assertThat(result.getContent().get(0).getEmployeeCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Update Department: Success")
    void updateDepartment_Success() {
        DepartmentRequest updateRequest = new DepartmentRequest("Platform Engineering", "Updated description");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Platform Engineering", 1L)).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(3L);

        DepartmentResponse response = departmentService.updateDepartment(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(departmentRepository).save(department);
    }

    @Test
    @DisplayName("Update Department: Conflicting Name Throws DuplicateResourceException")
    void updateDepartment_DuplicateName_ThrowsException() {
        DepartmentRequest updateRequest = new DepartmentRequest("Human Resources", "Updated description");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Human Resources", 1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.updateDepartment(1L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Delete Department: Success when no employees assigned")
    void deleteDepartment_Success() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

        departmentService.deleteDepartment(1L);

        verify(departmentRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete Department: Throws Exception when active employees exist")
    void deleteDepartment_WithEmployees_ThrowsException() {
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> departmentService.deleteDepartment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active employees are assigned");

        verify(departmentRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Delete Department: Throws ResourceNotFoundException for nonexistent ID")
    void deleteDepartment_NotFound_ThrowsException() {
        when(departmentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> departmentService.deleteDepartment(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(departmentRepository, never()).deleteById(anyLong());
    }
}