package com.peopleflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peopleflow.dto.department.DepartmentSummaryDto;
import com.peopleflow.dto.employee.EmployeeRequest;
import com.peopleflow.dto.employee.EmployeeResponse;
import com.peopleflow.exception.DuplicateResourceException;
import com.peopleflow.exception.ResourceNotFoundException;
import com.peopleflow.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private EmployeeResponse buildEmployeeResponse() {
        return new EmployeeResponse(
                1L,
                "Rahul",
                "Sharma",
                "rahul@example.com",
                "9876543210",
                "Software Engineer",
                LocalDate.of(2026, 11, 1),
                new BigDecimal("65000.00"),
                new DepartmentSummaryDto(1L, "Engineering"),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/employees: 201 Created on valid employee payload")
    void createEmployee_Success() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "Rahul", "Sharma", "rahul@example.com", "9876543210",
                "Software Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("65000.00"), 1L);

        when(employeeService.createEmployee(any(EmployeeRequest.class))).thenReturn(buildEmployeeResponse());

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Rahul"))
                .andExpect(jsonPath("$.email").value("rahul@example.com"))
                .andExpect(jsonPath("$.department.name").value("Engineering"));
    }

    @Test
    @DisplayName("POST /api/v1/employees: 400 Bad Request on missing mandatory fields")
    void createEmployee_MissingFields_Returns400() throws Exception {
        EmployeeRequest invalidRequest = new EmployeeRequest(); // all fields null

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.firstName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/employees: 400 Bad Request on invalid email format")
    void createEmployee_InvalidEmail_Returns400() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "Rahul", "Sharma", "not-a-valid-email", "9876543210",
                "Software Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("65000.00"), 1L);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/employees: 400 Bad Request on negative salary")
    void createEmployee_NegativeSalary_Returns400() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "Rahul", "Sharma", "rahul@example.com", "9876543210",
                "Software Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("-500.00"), 1L);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.salary").exists());
    }

    @Test
    @DisplayName("POST /api/v1/employees: 409 Conflict on duplicate email")
    void createEmployee_DuplicateEmail_Returns409() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "Rahul", "Sharma", "rahul@example.com", "9876543210",
                "Software Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("65000.00"), 1L);

        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenThrow(new DuplicateResourceException("Employee with email 'rahul@example.com' already exists"));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee with email 'rahul@example.com' already exists"));
    }

    @Test
    @DisplayName("POST /api/v1/employees: 404 Not Found on invalid department ID")
    void createEmployee_InvalidDepartment_Returns404() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "Rahul", "Sharma", "rahul@example.com", "9876543210",
                "Software Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("65000.00"), 99L);

        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenThrow(new ResourceNotFoundException("Department not found with id: 99"));

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id}: 200 OK when employee exists")
    void getEmployeeById_Success() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(buildEmployeeResponse());

        mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Rahul"))
                .andExpect(jsonPath("$.email").value("rahul@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/employees/{id}: 404 Not Found when employee does not exist")
    void getEmployeeById_NotFound_Returns404() throws Exception {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new ResourceNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/v1/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/v1/employees: 200 OK with paginated, filtered result")
    void getAllEmployees_Success() throws Exception {
        when(employeeService.getAllEmployees(eq(1L), eq("rahul"), any()))
                .thenReturn(new PageImpl<>(List.of(buildEmployeeResponse())));

        mockMvc.perform(get("/api/v1/employees?departmentId=1&search=rahul&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Rahul"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/employees/{id}: 200 OK on successful update")
    void updateEmployee_Success() throws Exception {
        EmployeeRequest request = new EmployeeRequest(
                "Rahul", "Sharma", "rahul@example.com", "9876543210",
                "Senior Software Engineer", LocalDate.of(2026, 11, 1), new BigDecimal("75000.00"), 1L);

        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class))).thenReturn(buildEmployeeResponse());

        mockMvc.perform(put("/api/v1/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Rahul"));
    }

    @Test
    @DisplayName("DELETE /api/v1/employees/{id}: 204 No Content on successful deletion")
    void deleteEmployee_Success() throws Exception {
        doNothing().when(employeeService).deleteEmployee(1L);

        mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/employees/{id}: 404 Not Found for nonexistent employee")
    void deleteEmployee_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/api/v1/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }
}