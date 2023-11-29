import java.time.LocalDateTime;

public class Share {
    private String email;
    private LocalDateTime dateTime;
    private int number;

    public Share(String email, LocalDateTime dateTime,int number){
        this.email = email;
        this.dateTime = dateTime;
        this.number = number;
    }
}
