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

    @Override
    public String toString(){
        return email+"|"+dateTime.toString().replace(":","-").substring(0,16)+"|"+number;
    }

    public String getEmail() {
        return email;
    }

    public String getDateTime() {
        return dateTime.toString().substring(0, 16).replace(":","-");
    }

    public String getNumber() {
        return String.valueOf(number);
    }
}
