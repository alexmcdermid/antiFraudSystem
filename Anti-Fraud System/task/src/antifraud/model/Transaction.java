package antifraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long amount;
    private String ip;
    private String number;
    private String region;
    private Date date;
    private String result;
    private String feedback = "";

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

    public String getRegion() {return region;}

    public void setRegion(String region) {this.region = region;}

    public Date getDate() {return date;}

    public void setDate(Date date) {this.date = date;}

    public String getResult() {return result;}

    public void setResult(String result) {this.result = result;}

    public String getFeedback() {return feedback;}

    public void setFeedback(String feedback) {this.feedback = feedback;}
}
