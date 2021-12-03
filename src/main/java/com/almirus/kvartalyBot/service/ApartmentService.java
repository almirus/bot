package com.example.buns.service;

import com.example.buns.dal.entity.Apartment;
import com.example.buns.dal.repository.ApartmentRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

@Service
@Data
public class ApartmentService {
    private final ApartmentRepository apartmentRepository;

    public Apartment getApartment(Integer roomNum) {
        return apartmentRepository.getApartmentById(roomNum);
    }
}
