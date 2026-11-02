package com.peopleflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peopleflow.dto.department.DepartmentRequest;
import com.peopleflow.dto.department.DepartmentResponse;
import com.peopleflow.exception.DuplicateResourceException;
import com.peopleflow.exception.ResourceNotFoundException;
import com.peopleflow.service.DepartmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    @Test
    @DisplayName("POST /api/v1/departments: 201 Created on valid request")
    void createDepartment_Success() throws Exception {
        DepartmentRequest request = new DepartmentRequest("Engineering", "Software team");
        DepartmentResponse response = new DepartmentResponse(1L, "Engineering", "Software team", 0, LocalDateTime.now(), LocalDateTime.now());

        when(departmentService.createDepartment(any(DepartmentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.employeeCount").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/departments: 400 Bad Request when name is blank")
    void createDepartment_BlankName_Returns400() throws Exception {
        DepartmentRequest invalidRequest = new DepartmentRequest("", "Description");

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());
    }

    @Test
    @DisplayName("POST /api/v1/departments: 409 Conflict when name is duplicated")
    void createDepartment_DuplicateName_Returns409() throws Exception {
        DepartmentRequest request = new DepartmentRequest("Engineering", "Software team");
        when(departmentService.createDepartment(any(DepartmentRequest.class)))
                .thenThrow(new DuplicateResourceException("Department with name 'Engineering' already exists"));

        mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Department with name 'Engineering' already exists"));
    }

    @Test
    @DisplayName("GET /api/v1/departments/{id}: 200 OK when department exists")
    void getDepartmentById_Success() throws Exception {
        DepartmentResponse response = new DepartmentResponse(1L, "Engineering", "Software team", 4, LocalDateTime.now(), LocalDateTime.now());
        when(departmentService.getDepartmentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.employeeCount").value(4));
    }

    @Test
    @DisplayName("GET /api/v1/departments/{id}: 404 Not Found when department is missing")
    void getDepartmentById_NotFound_Returns404() throws Exception {
        when(departmentService.getDepartmentById(99L))
                .thenThrow(new ResourceNotFoundException("Department not found with id: 99"));

        mockMvc.perform(get("/api/v1/departments/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Department not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/v1/departments: 200 OK with paginated response")
    void getAllDepartments_Success() throws Exception {
        DepartmentResponse response = new DepartmentResponse(1L, "Engineering", "Software team", 2, LocalDateTime.now(), LocalDateTime.now());
        when(departmentService.getAllDepartments(any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/departments?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Engineering"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/departments/{id}: 200 OK on valid update")
    void updateDepartment_Success() throws Exception {
        DepartmentRequest request = new DepartmentRequest("HR", "People Operations");
        DepartmentResponse response = new DepartmentResponse(1L, "HR", "People Operations", 1, LocalDateTime.now(), LocalDateTime.now());

        when(departmentService.updateDepartment(eq(1L), any(DepartmentRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("HR"));
    }

    @Test
    @DisplayName("DELETE /api/v1/departments/{id}: 204 No Content on successful deletion")
    void deleteDepartment_Success() throws Exception {
        doNothing().when(departmentService).deleteDepartment(1L);

        mockMvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/departments/{id}: 409 Conflict when department has employees")
    void deleteDepartment_WithEmployees_Returns409() throws Exception {
        doThrow(new IllegalStateException("Cannot delete department with id 1 because active employees are assigned to it"))
                .when(departmentService).deleteDepartment(1L);

        mockMvc.perform(delete("/api/v1/departments/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("active employees are assigned")));
    }
}