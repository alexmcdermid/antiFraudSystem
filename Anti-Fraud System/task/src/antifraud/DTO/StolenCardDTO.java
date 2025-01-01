package antifraud.DTO;

public class StolenCardDTO {
    private Long id;
    private String number;

    public StolenCardDTO(Long id, Long number) {
        this.id = id;
        this.number = Long.toString(number);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = Long.toString(number);
        
    }
}
