package com.peopleflow.service;

import com.peopleflow.dto.employee.EmployeeRequest;
import com.peopleflow.dto.employee.EmployeeResponse;
import com.peopleflow.entity.Department;
import com.peopleflow.entity.Employee;
import com.peopleflow.exception.DuplicateResourceException;
import com.peopleflow.exception.ResourceNotFoundException;
import com.peopleflow.repository.DepartmentRepository;
import com.peopleflow.repository.EmployeeRepository;
import com.peopleflow.service.impl.EmployeeServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Department department;
    private Employee employee;
    private EmployeeRequest request;

    @BeforeEach
    void setUp() {
        department = new Department("Engineering", "Tech department");
        department.setId(1L);

        employee = new Employee(
                "Rahul",
                "Sharma",
                "rahul@example.com",
                "9876543210",
                "Software Engineer",
                LocalDate.of(2026, 11, 1),
                new BigDecimal("65000.00"),
                department
        );
        employee.setId(10L);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());

        request = new EmployeeRequest(
                "Rahul",
                "Sharma",
                "rahul@example.com",
                "9876543210",
                "Software Engineer",
                LocalDate.of(2026, 11, 1),
                new BigDecimal("65000.00"),
                1L
        );
    }

    @Test
    @DisplayName("Create Employee: Success")
    void createEmployee_Success() {
        when(employeeRepository.existsByEmailIgnoreCase("rahul@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse response = employeeService.createEmployee(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("rahul@example.com");
        assertThat(response.getDepartment().getName()).isEqualTo("Engineering");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Create Employee: Duplicate Email Throws DuplicateResourceException")
    void createEmployee_DuplicateEmail_ThrowsException() {
        when(employeeRepository.existsByEmailIgnoreCase("rahul@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Create Employee: Invalid Department Throws ResourceNotFoundException")
    void createEmployee_InvalidDepartment_ThrowsException() {
        when(employeeRepository.existsByEmailIgnoreCase("rahul@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Department not found");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    @DisplayName("Find Employee By ID: Success")
    void getEmployeeById_Success() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getFirstName()).isEqualTo("Rahul");
    }

    @Test
    @DisplayName("Find Employee By ID: Nonexistent ID Throws ResourceNotFoundException")
    void getEmployeeById_NotFound_ThrowsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");
    }

    @Test
    @DisplayName("Get All Employees: Filtered & Paginated")
    void getAllEmployees_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Employee> empPage = new PageImpl<>(List.of(employee), pageable, 1);

        when(employeeRepository.searchEmployees(1L, "rahul", pageable)).thenReturn(empPage);

        Page<EmployeeResponse> result = employeeService.getAllEmployees(1L, "rahul", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("rahul@example.com");
    }

    @Test
    @DisplayName("Update Employee: Success")
    void updateEmployee_Success() {
        EmployeeRequest updateRequest = new EmployeeRequest(
                "Rahul", "Verma", "rahul.verma@example.com", "9876543210",
                "Senior Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("85000.00"), 1L);

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("rahul.verma@example.com", 10L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse response = employeeService.updateEmployee(10L, updateRequest);

        assertThat(response).isNotNull();
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("Update Employee: Conflicting Email Throws DuplicateResourceException")
    void updateEmployee_DuplicateEmail_ThrowsException() {
        EmployeeRequest updateRequest = new EmployeeRequest(
                "Rahul", "Verma", "other@example.com", "9876543210",
                "Senior Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("85000.00"), 1L);

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("other@example.com", 10L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(10L, updateRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Delete Employee: Success")
    void deleteEmployee_Success() {
        when(employeeRepository.existsById(10L)).thenReturn(true);

        employeeService.deleteEmployee(10L);

        verify(employeeRepository).deleteById(10L);
    }

    @Test
    @DisplayName("Delete Employee: Nonexistent ID Throws ResourceNotFoundException")
    void deleteEmployee_NotFound_ThrowsException() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(employeeRepository, never()).deleteById(anyLong());
    }
}