package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.Debt;
import com.almirus.kvartalyBot.dal.repository.DebtRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Data
public class DebtService {
    private final DebtRepository debtorRepository;

    public List<Debt> getDebtList() {
        return debtorRepository.findAll();
    }

    public Debt save(Debt debt){
        return  debtorRepository.save(debt);
    }
}
