package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.Apartment;
import com.almirus.kvartalyBot.dal.repository.ApartmentRepository;
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
