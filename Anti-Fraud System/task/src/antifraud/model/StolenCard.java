package antifraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class StolenCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long number;

    public StolenCard() {}

    public Long getId() {
        return id;
    }

    public Long getNumber() {return number;}

    public void setNumber(Long cardNumber) {this.number = cardNumber;}
}
