package antifraud.service;

import antifraud.model.IP;
import antifraud.repository.IPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IPService {
    private final IPRepository ipRepository;

    @Autowired
    public IPService(IPRepository ipRepository) {
        this.ipRepository = ipRepository;
    }

    public IP saveIP(IP ip) {
        return ipRepository.save(ip);
    }

    public List<IP> getAllIPs() {
        return ipRepository.findAll();
    }

    public IP findByIp(String ip) {
        return ipRepository.findByIp(ip).orElse(null);
    }

    public void deleteIP(IP ip) {
        ipRepository.delete(ip);
    }

    public boolean isIpTaken(String ip) {
        return ipRepository.existsByIp(ip);
    }
}
