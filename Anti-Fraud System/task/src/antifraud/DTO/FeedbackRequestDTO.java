package antifraud.DTO;

public class FeedbackRequestDTO {
    private Long transactionId;
    private String feedback;

    public FeedbackRequestDTO(Long transactionId, String feedback) {
        this.transactionId = transactionId;
        this.feedback = feedback;
    }

    public Long getTransactionId() {return transactionId;}
    public String getFeedback() {return feedback;}
    public void setTransactionId(Long transactionId) {this.transactionId = transactionId;}
    public void setFeedback(String feedback) {this.feedback = feedback;}
}
