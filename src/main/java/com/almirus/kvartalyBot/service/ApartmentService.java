package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.Apartment;
import com.almirus.kvartalyBot.dal.repository.ApartmentRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Data
public class ApartmentService {
    private final ApartmentRepository apartmentRepository;

    public Apartment getApartment(Integer roomNum) {
        return apartmentRepository.getApartmentById(roomNum);
    }
    public Apartment getApartmentDDU(Integer roomNum) {
        return apartmentRepository.getApartmentByDduNum(roomNum);
    }
    public List<Apartment> getFloorApartments(Integer floor, Integer entrance) {
        return apartmentRepository.getFloorApartments(floor, entrance);
    }

    public List<Apartment> getEntranceApartments(Integer entrance) {
        return apartmentRepository.getEntranceApartments(entrance);
    }

    public List<Integer> getEntranceFloors(Integer entrance){
        return apartmentRepository.getEntranceFloors(entrance);
    }
}
