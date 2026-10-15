package com.turf.booking.service;

import com.turf.booking.exception.ResourceNotFoundException;
import com.turf.booking.model.Customer;
import com.turf.booking.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer create(Customer customer) {
        customer.setId(null);
        return customerRepository.save(customer);
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }

    public Customer update(Long id, Customer updated) {
        Customer customer = getById(id);
        customer.setName(updated.getName());
        customer.setPhone(updated.getPhone());
        customer.setEmail(updated.getEmail());
        return customerRepository.save(customer);
    }
}
