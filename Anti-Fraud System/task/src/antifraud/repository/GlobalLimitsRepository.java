package antifraud.repository;

import antifraud.model.GlobalLimits;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalLimitsRepository extends JpaRepository<GlobalLimits, Long> {
}