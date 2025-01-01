package antifraud.repository;

import antifraud.model.IP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IPRepository extends JpaRepository<IP, Long> {
    Optional<IP> findByIp(String ip);
    boolean existsByIp(String ip);
}
