package com.example.Software.project.Service.Counts;


import java.time.LocalDate;
// import java.time.Month;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.Software.project.Repo.Customer.CustomerRepo;
import com.example.Software.project.Repo.Employee.EmployeesRepository;
import com.example.Software.project.Repo.Item.ItemRepo;
import com.example.Software.project.Repo.Jobs.JobsRepository;
import com.example.Software.project.Repo.Units.UnitsRepository;
import com.example.Software.project.Repo.Vehicles.VehiclesRepository;
import com.example.Software.project.Repo.Visits.VisitsRepository;

// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

@Service
public class CountsService {

    @Autowired
    private UnitsRepository unitsRepository;

    @Autowired
    private JobsRepository jobsRepository;

    @Autowired
    private VehiclesRepository vehiclesRepository;

    @Autowired
    private ItemRepo itemsRepository;

    @Autowired
    private CustomerRepo customersRepository;

    @Autowired
    private EmployeesRepository employeesRepository;

    @Autowired
    private VisitsRepository visitsRepository;

    public long getUnitsCount() {
        return unitsRepository.count();
    }

      public long getVehiclesCount() {
        return vehiclesRepository.count();
    }

    public long getItemsCount() {
        return itemsRepository.count();
    }

    public long getCustomersCount() {
        return customersRepository.count();
    }

    public long getEmployeesCount() {
        return employeesRepository.count();
    }

//calculates the start and end dates of the current month and get the count

    public long getCurrentMonthVisitsCount() {
        LocalDate currentDate = LocalDate.now();
        LocalDate startOfMonth = currentDate.withDayOfMonth(1); // First day of the current month
        LocalDate endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth()); // Last day of the current month
        return visitsRepository.countByDateBetween(startOfMonth, endOfMonth);
    }

    public long getCurrentMonthJobsCount() {
        LocalDate currentDate = LocalDate.now();
        LocalDate startOfMonth = currentDate.withDayOfMonth(1); // First day of the current month
        LocalDate endOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth()); // Last day of the current month
        return jobsRepository.countByDateBetween(startOfMonth, endOfMonth);
    }
    
    
}