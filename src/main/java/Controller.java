import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import javax.mail.MessagingException;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Created by Josue Cubero on 11/12/2016.
 */
public class Controller {
    public static List<String> IdSpam = new LinkedList<>();
    public static List<String> IdInbox = new LinkedList<>();
    private static List<Triad>[] filterSpam;
    private static List<Triad>[] filterInbox;
    private static List<String>[] most_used;
    MyClass nueva;
    Quickstart gmail;
    Gmail service;


    boolean trained = false;
    private double spam_probability = 0.3;
    private double spam_threshold = 0.9;
    private int training_size = 50;
    private int total_words = 0;
    public int total_inbox = 0;
    public int total_spam = 0;

    public Controller(){
        most_used = new LinkedList[26];
        for(int i = 0; i < most_used.length; i++){
            most_used[i] = new LinkedList<>();
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader("MostUsed.txt"));
            String current;
            while ((current = reader.readLine()) != null) {
                most_used[(current.charAt(0)) - 97].add(current);
            }

            for (int i = 0; i < most_used.length; i++) {
                Iterator it = most_used[i].iterator();
                while (it.hasNext()) {
                    System.out.print(it.next() + " ");
                }
                System.out.println();
            }
            filterSpam = new LinkedList[26];
            filterInbox = new LinkedList[26];
            for (int i = 0; i < filterSpam.length; i++) {
                filterSpam[i] = new LinkedList<>();
                filterInbox[i] = new LinkedList<>();
            }
        }
        catch (IOException e) {
            System.out.println("No se pudo abrir el archivo de palabras mas comunes.");
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader("SpamData.txt"));
            BufferedReader reader2 = new BufferedReader(new FileReader("InboxData.txt"));
            String current;
            while ((current = reader.readLine()) != null) {
                String[] line = current.split(",");
                Triad triad = new Triad(line[0], Float.parseFloat(line[1]), 0);
                filterSpam[line[0].charAt(0) - 97].add(triad);
            }

            while ((current = reader2.readLine()) != null) {
                String[] line = current.split(",");
                Triad triad = new Triad(line[0], Float.parseFloat(line[1]), 0);
                filterInbox[line[0].charAt(0) - 97].add(triad);
            }

            for (int i = 0; i < filterSpam.length; i++) {
                Iterator it = filterSpam[i].iterator();
                while (it.hasNext()) {
                    String test[] = ((Triad) it.next()).get_triad_data();
                    System.out.println(test[0] + "," + test[1] + "," + "en datos.");
                }
            }

            trained = true;
        }
        catch(IOException e){}
    }

    public void main_menu(){
        Boolean cont = true;
        while(cont){
            System.out.println("Filtro anti spam, ingrese 1 si desea autenticarse o 2 si desea salir de la aplicacion.");
            Scanner option = new Scanner(System.in);
            int n = option.nextInt();
            if(n == 1){
                try {
                    gmail = new Quickstart();
                    service = gmail.getGmailService();
                    sub_menu();
                    service = null;
                    gmail = null;

                }
                catch(IOException e){

                }
                //cont = false;
            } else if(n == 2) {
                cont = false;
            } else {
                System.out.println("Ingrese un numero valido.");
            }
        }
    }

    public void sub_menu(){
        Boolean cont = true;
        while(cont) {
            System.out.println("Ingrese 1 para configurar\nIngrese 2 para entrenar\nIngrese 3 para mostrar datos\n" +
                    "Ingrese 4 obtener un correo nuevo\nIngrese un 5 para desloguearse\nIngrese un 6 para salir");
            Scanner option = new Scanner(System.in);
            int n = option.nextInt();
            switch (n) {
                case 1:
                    System.out.println("1");
                    configuration_menu();
                    break;
                case 2:
                    System.out.println("2");
                    train_filter();
                    break;
                case 3:
                    System.out.println("3");
                    //mostrar datos
                    break;
                case 4:
                    System.out.println("4");
                    classify_mail();
                    //correo nuevo
                    break;
                case 5:
                    System.out.println("5");
                    delete_credentials();
                    //desloguear
                    return;
                default:
                    System.out.println("default");
            }
        }
    }

    public void configuration_menu(){
        int temporal;
        Boolean cont = true;
        System.out.println("Ingrese 1 si desea poner la probabilidad de spam o dos si desea la cantidad por defecto(0.3)");
        Scanner option = new Scanner(System.in);
        temporal = option.nextInt();
        if(temporal == 1) {
            spam_probability = option.nextDouble();
            System.out.println("Probabilidad de spam cambiada a: " + String.valueOf(spam_probability));
        } else {
            System.out.println("Probabilidad de spam dejada como 0.3(defalut)");
        }
        System.out.println("Ingrese 1 si desea poner la el spam threshold o dos si desea la cantidad por defecto(0.9)");
        //option = new Scanner(System.in);
        temporal = option.nextInt();
        if(temporal == 1) {
            spam_threshold = option.nextDouble();
            System.out.println("Spam threshold cambiada a: " + String.valueOf(spam_threshold));
        } else {
            System.out.println("Spam threshold dejada como 0.9(defalut)");
        }
        while(cont) {
            System.out.println("Ingrese el tamano del conjunto de entrenamiento");
            option = new Scanner(System.in);
            temporal = option.nextInt();
            if(temporal >= 50) {
                training_size = temporal;
                System.out.println("Tamano del conjunto de entrenamiento puesto en: " + training_size);
                cont = false;
            } else {
                System.out.println("Ingrese un numero mayor a 50");
            }
        }
    }

    public void train_filter() {
        // Build a new authorized API client service.
        try {
            //service = Quickstart.getGmailService();

            // Print the labels in the user's account.
            String user = "me";

            List<String> Spam = new LinkedList<>();
            Spam.add("SPAM");
            List<String> Inbox = new LinkedList<>();
            Inbox.add("UNREAD");
            nueva = new MyClass();
            int[] frecuencies = new int[99999];
            String[] words = new String[99999];
            int[] probabilities = new int[99999];
            nueva.listThreadsWithLabels(service, user, Spam, IdSpam);
            nueva.listThreadsWithLabels(service, user, Inbox, IdInbox);
            Iterator it = IdSpam.iterator();
            Message mensaje;
            try {
                while (it.hasNext()) {
                    mensaje = nueva.getFullMessage(service, user, it.next().toString(), filterSpam);
                    total_spam++;
                }
                it = IdInbox.iterator();
                while (it.hasNext() && total_inbox < 10) {
                    mensaje = nueva.getFullMessage(service, user, it.next().toString(), filterInbox);
                    total_inbox++;
                }
                nueva.Bayes(total_inbox, total_spam, filterSpam, filterInbox);
                Triad aux;
                PrintWriter b_writer = new PrintWriter("SpamData.txt");
                PrintWriter b_writer2 = new PrintWriter("InboxData.txt");
                for(int i = 0; i < 26; i++) {
                    Iterator it2 = filterSpam[i].iterator();
                    Iterator it3 = filterInbox[i].iterator();
                    while(it2.hasNext()) {
                        aux = (Triad) it2.next();
                        b_writer.println(aux.getWord() + "," + aux.getFrecuency() + "," + aux.getProbability());
                    }
                    while(it3.hasNext()){
                        aux = (Triad) it3.next();
                        b_writer2.println(aux.getWord() + "," + aux.getFrecuency() + "," + aux.getProbability());
                    }
                }
                b_writer.close();
                b_writer2.close();
            }
            catch (MessagingException e){

            }
        } catch (IOException e) {

        }
    }

    public void classify_mail(){
        List<String> Unread = new LinkedList<>();
        Unread.add("UNREAD");
        Iterator it = IdInbox.iterator();
        String mensaje;
        Double message_probability;
        try{
            String user = "me";
            try{
                int total = 0;
                while (it.hasNext() && total < 10) {
                    mensaje = nueva.getMessage(service, user, it.next().toString());
                    message_probability = nueva.calculate_probability(filterSpam, filterInbox, mensaje, spam_probability);
                    System.out.println("Probabilidad de ser SPAM: " + message_probability);
                    if(message_probability > spam_threshold){
                        System.out.println("Este mensaje es SPAM");
                    }else{
                        System.out.println("Este mensaje no es SPAM");
                    }
                    total++;
                }
            }
            catch(MessagingException e){

            }
        }
        catch(IOException e){

        }
    }

    public void delete_credentials(){
        Path file_location = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".credentials/gmail-java-quickstart", "StoredCredential");
        System.out.println(file_location.getFileName());
        try{
            Files.deleteIfExists(file_location);
        }
        catch (IOException e){

        }
    }
}
