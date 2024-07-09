
//add new job
package com.example.Software.project.Entity.JobAllocationModule;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.springframework.data.mongodb.core.mapping.Document;

@Entity
@Document(collection = "FormData")
public class TeamMemberEntity {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeName;


    private String employeePhone;



    private String employeeDesignation;
    private String employeeEmail;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private com.example.project_backend.Entity.Customer customer;

    public void setEmployeeName(String employeeName) {
    }

    public void setEmployeePhone(String employeePhone) {
    }

    public void setEmployeeDesignation(String employeeDesignation) {
    }

    public void setEmployeeEmail(String employeeEmail) {
    }

    public void setCustomer(com.example.project_backend.Entity.Customer customer) {
    }

    // Getters and Setters
    // ...
}