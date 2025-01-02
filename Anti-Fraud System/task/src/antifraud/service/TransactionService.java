package antifraud.service;

import antifraud.model.Transaction;
import antifraud.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElse(null);
    }

    public List<Transaction> getAllTransactionsSorted() {
        return transactionRepository.findAllByOrderByIdAsc();
    }

    public List<Transaction> getTransactionsByNumberSorted(String number) {
        return transactionRepository.findByNumberOrderByIdAsc(number);
    }

    public List<Transaction> findByNumberAndDateBetween(String cardNumber, Date afterDate, Date currentDate) {
        return transactionRepository.findByNumberAndDateBetween(cardNumber, afterDate, currentDate);
    }
}
