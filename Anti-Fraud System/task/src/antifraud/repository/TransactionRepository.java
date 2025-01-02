package antifraud.repository;

import antifraud.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByNumberAndDateBetween(String number, Date startDate, Date endDate);
}
