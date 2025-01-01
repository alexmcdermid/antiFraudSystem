package antifraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class IP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ip;

    public IP() {
    }

    public Long getId() {
        return id;
    }

    public String getIp() {return ip;}

    public void setIp(String ip) {this.ip = ip;}
}
