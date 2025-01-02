package antifraud.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class GlobalLimits {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long allowedLimit;
    private Long manualLimit;

    public Long getId() {
        return id;
    }

    public Long getAllowedLimit() {return allowedLimit;}
    public Long getManualLimit() {return manualLimit;}
    public void setAllowedLimit(Long allowedLimit) {this.allowedLimit = allowedLimit;}
    public void setManualLimit(Long manualLimit) {this.manualLimit = manualLimit;}
}

