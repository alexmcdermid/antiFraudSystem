package antifraud.service;

import antifraud.model.StolenCard;
import antifraud.repository.StolenCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StolenCardService {
    private final StolenCardRepository stolenCardRepository;

    @Autowired
    public StolenCardService(StolenCardRepository stolenCardRepository) {
        this.stolenCardRepository = stolenCardRepository;
    }

    public StolenCard saveStolenCard(StolenCard stolenCard) {
        return stolenCardRepository.save(stolenCard);
    }

    public List<StolenCard> getAllStolenCards() {
        return stolenCardRepository.findAll();
    }

    public StolenCard findStolenCardById(Long cardNumber) {
        return stolenCardRepository.findByNumber(cardNumber).orElse(null);
    }

    public void deleteStolenCard(StolenCard stolenCard) {
        stolenCardRepository.delete(stolenCard);
    }

    public boolean stolenCardExist(Long cardNumber) {
        return stolenCardRepository.existsByNumber(cardNumber);
    }
}
