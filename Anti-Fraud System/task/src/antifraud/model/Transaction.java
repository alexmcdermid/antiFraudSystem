package antifraud.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long amount;
    private String ip;
    private String number;
    private String info;

    public Long getId() {
        return id;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getIp() {return ip;}

    public void setIp(String ip) {this.ip = ip;}

    public String getNumber() {return number;}

    public void setNumber(String number) {this.number = number;}

    public String getInfo() {return info;}

    public void setInfo(String info) {this.info = info;}
}
