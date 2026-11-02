package com.peopleflow.dto.department;

/**
 * Lightweight projection of Department returned inside Employee responses.
 */
public class DepartmentSummaryDto {

    private Long id;
    private String name;

    public DepartmentSummaryDto() {
    }

    public DepartmentSummaryDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}