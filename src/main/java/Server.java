import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.*;

import static spark.Spark.*;
import static spark.Spark.halt;

public class Server {
    private String path;
    private Integer port;
    private int pool = 0;
    private Gmailer gmailer;

    public Server(Integer port, String path, int pool, Gmailer gmailer) {
        this.path = path;
        this.port = port;
        this.pool = pool;
        this.gmailer = gmailer;
        this.start();
    }

    public void start() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (LocalDateTime.now().getMinute()==30) {
                    String now = LocalDateTime.now().withMinute(30).toString();
                    try {
                        System.out.println(lottery(now));
                        //System.out.println("Hola");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, 0, 60000);
        Spark.port(port);
        // Operations for the API
        post("v1/lottery/play", this::accept);
        get("v1/lottery/winners", this::getHistoric);
    }

    private String getHistoric(Request req, Response res) {
        List<String> winners = new ArrayList<>();
        String path_win = this.path+"/winners.csv";
        try {
            CSVReader reader = new CSVReader(new FileReader(path_win));
            String[] wins;
            while ((wins = reader.readNext()) != null) {
                for (String win : wins) {
                    if (win.length() > 1) winners.add(win);
                }
            }
            return new Gson().toJson(winners);
        } catch (FileNotFoundException ie){
            halt(500, "File not found");
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private String accept(Request req, Response res) throws IOException, CsvValidationException {
        Share share = new Gson().fromJson(req.body(), Share.class);
        String fileName = share.getDateTime() + ".csv";
        File file = new File(this.path, fileName);
        if (file.createNewFile()){
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(share.getEmail()+"|"+share.getNumber()+",");
            writer.close();
        }
        else {
            String path = this.path+"/"+share.getDateTime()+".csv";
            CSVReader reader = new CSVReader(new FileReader(path));
            String[] shares;
            while ((shares=reader.readNext()) != null) {
                for (String one_share : shares) {
                    System.out.println(share);
                    String[] parts_share = one_share.split("\\|");
                    if (parts_share[0].equals(share.getEmail()) && parts_share[1].equals(share.getNumber()))
                        return "You can't play twice with the same number";
                }
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(share.getEmail() + "|" + share.getNumber() + ",");
            writer.close();
        }
        pool += 100;
        return "Thank you for participating: " + share.toString();
    }

    private String lottery(String now) throws Exception {
        List<String> winners = new ArrayList<>();
        Integer winner_num = new Random().nextInt(256);
        String path_participants = this.path+"/" + now.replace(":","-").substring(0,16) + ".csv";
        try {
            CSVReader reader = new CSVReader(new FileReader(path_participants));
            String[] shares;
            while ((shares = reader.readNext()) != null) {
                for (String share : shares) {
                    String[] parts_share = share.split("\\|");
                    if (parts_share.length == 2) {
                        if (Integer.parseInt(parts_share[1]) == winner_num)
                            winners.add(parts_share[0]);
                    }
                }
            }
            reader.close();
            new File(path_participants).delete();
            if (winners.size() == 0) return "Number: " + winner_num + ". No winners";
            else {
                String subject = "Congratulations!";
                String message = "";
                int ppw = pool / winners.size();
                File file = new File(this.path + "/winners.csv");
                BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
                for (String winner : winners) {
                    writer.write(now.substring(0, 16) + "|" + winner + "|" + ppw + ",");
                    message = "Congratulations " + winner + ". You have won " + ppw + " SEK with number " + winner_num
                    + ". Thank you for participating.\nBest regards,\nLottery365";
                    gmailer.sendMail(winner, subject, message);
                }
                writer.close();
                pool = 0;
                return "The winner number is " + winner_num + ". The winners are" + winners.toString() + ". Each one won: " + ppw;
            }
        } catch (FileNotFoundException ie){
            return "No participants";
        }
    }

    private String toJson(Object o){
        return new Gson().toJson(o);
    }

    public void stop(){Spark.stop();}

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        Gmailer gmailer = new Gmailer();
        List<Integer> servers = new ArrayList<>();
        servers.add(4568);servers.add(4569);
        Server server = new Server(4567, "files", 400, gmailer);
    }
}
