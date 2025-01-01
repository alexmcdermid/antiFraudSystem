package antifraud.DTO;

public class IPDTO {

    private Long id;
    private String ip;

    public IPDTO(Long id, String ip) {
        this.id = id;
        this.ip = ip;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

}
