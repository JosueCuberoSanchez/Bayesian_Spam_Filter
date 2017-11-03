import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListThreadsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Thread;
import org.jsoup.Jsoup;

import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


// ...

public class MyClass {

    private static List<Triad>[] filter;
    private static List<String>[] most_used;
    private static int counter1 = 0;
    double spam_probability = 0.3;
    double spam_threshold = 0.9;
    int training_size = 50;
    // ...

    public MyClass(){
        most_used = new LinkedList[26];
        for(int i = 0; i < most_used.length; i++){
            most_used[i] = new LinkedList<>();
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader("MostUsed.txt"));
            String current;
            while((current = reader.readLine()) != null){
                most_used[(current.charAt(0)) - 97].add(current);
            }

            for(int i = 0; i < most_used.length; i++){
                Iterator it = most_used[i].iterator();
                while(it.hasNext()){
                    System.out.print(it.next() + " ");
                }
                System.out.println();
            }
            filter = new LinkedList[26];
            for(int i = 0; i < filter.length; i++){
                filter[i] = new LinkedList<>();
            }
        }
        catch(IOException e){
            System.out.println("No se pudo abrir el archivo de palabras mas comunes.");
        }
    } //end of constructor

    /**
     * List all Threads of the user's mailbox matching the query.
     *
     * @param service Authorized Gmail API instance.
     * @param userId  User's email address. The special value "me"
     *                can be used to indicate the authenticated user.
     * @param query   String used to filter the Threads listed.
     * @throws IOException
     */
    public static void listThreadsMatchingQuery(Gmail service, String userId,
                                                String query) throws IOException {
        ListThreadsResponse response = service.users().threads().list(userId).setQ(query).execute();
        List<Thread> threads = new ArrayList<Thread>();
        while (response.getThreads() != null) {
            threads.addAll(response.getThreads());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().threads().list(userId).setQ(query).setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        for (Thread thread : threads) {
            System.out.println(thread.toPrettyString());
        }
    }

    /**
     * List all Threads of the user's mailbox with labelIds applied.
     *
     * @param service  Authorized Gmail API instance.
     * @param userId   User's email address. The special value "me"
     *                 can be used to indicate the authenticated user.
     * @param labelIds String used to filter the Threads listed.
     * @throws IOException
     */
    public static void listThreadsWithLabels(Gmail service, String userId,
                                             List<String> labelIds,List<String> ID) throws IOException {
        ListThreadsResponse response = service.users().threads().list(userId).setLabelIds(labelIds).execute();
        List<Thread> threads = new ArrayList<Thread>();
        while (response.getThreads() != null) {
            threads.addAll(response.getThreads());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().threads().list(userId).setLabelIds(labelIds)
                        .setPageToken(pageToken).execute();
            } else {
                break;
            }
        }

        for (Thread thread : threads) {
            ID.add(thread.getId());
        }
    }

    // ...
    public static String getMessage(Gmail service, String userId, String messageId)
            throws IOException, MessagingException {
        Message message = service.users().messages().get(userId, messageId).setFormat("full").execute();
        String msj = StringUtils.newStringUtf8(Base64.decodeBase64(message.getPayload().getParts().get(1).getBody().toPrettyString()));
        msj = Jsoup.parse(msj).text().toLowerCase();

        return msj;
    }

    public static Message getFullMessage(Gmail service, String userId, String messageId, List<Triad>[] filterData)//int[] frecuencies,String[] words,int[] probabilites)
            throws IOException, MessagingException {
        Message message = service.users().messages().get(userId, messageId).setFormat("full").execute();
        System.out.println(message.getSnippet());
        String msj = StringUtils.newStringUtf8(Base64.decodeBase64(message.getPayload().getParts().get(1).getBody().toPrettyString()));
        msj = Jsoup.parse(msj).text().toLowerCase();
        System.out.println(msj);

        Boolean already_considered = false;
        Boolean is_in_most_used;

        String[] divide = msj.split("\\P{Alpha}+");
        int times;
        Triad aux;
        for (String actual_word : divide) {
            is_in_most_used = false;
            Iterator it = most_used[actual_word.charAt(0) - 97].iterator();
            while (it.hasNext() && !is_in_most_used) {
                if (actual_word.equals(it.next().toString())) {
                    is_in_most_used = true;
                }
            }

            if (!is_in_most_used) {
                if (!filterData[actual_word.charAt(0) - 97].isEmpty()) {
                    times = 0;
                    Iterator words_it = filterData[actual_word.charAt(0) - 97].iterator();
                    while (words_it.hasNext()) {
                        aux = (Triad) words_it.next();
                        System.out.println("WORD: " + aux.getWord());
                        if (actual_word.equalsIgnoreCase(aux.getWord())) {
                            times++;
                            aux.setFrecuencies(times);
                            System.out.println("NO VACIO");
                        }
                    }

                    if (times == 0) {
                        Triad triad = new Triad(actual_word, 1, 0);
                        System.out.println("==0");
                        filterData[actual_word.charAt(0) - 97].add(triad);
                    }
                } else {
                    Triad triad = new Triad(actual_word, 1, 0);
                    filterData[actual_word.charAt(0) - 97].add(triad);
                    System.out.println("==else");
                }
            }
        }
        return message;
    }


    public double Bayes(int total_inbox,int total_spam, List<Triad>[] spam, List<Triad>[] inbox){
        double result = 0;
        Triad aux;
        float pw=0;
        float qw=0;
        for(int i=0;i<26;i++) {
            Iterator it = spam[i].iterator();
            while(it.hasNext()){
                aux = (Triad) it.next();
                pw = aux.getFrecuency()/total_spam;
                if(pw > 1){
                    pw = 1;
                }
                aux.setProbability(pw);
            }

            it = inbox[i].iterator();
            while (it.hasNext()){
                aux = (Triad) it.next();
                qw = aux.getFrecuency()/total_inbox;
                if(qw > 1){
                    qw = 1;
                }
                aux.setProbability(qw);
            }
        }
        return result;
    }

  public double calculate_probability(List<Triad>[] spam, List<Triad>[] inbox, String msj, double spam_probability){
      double message_probability = 0;
      double numerator = 0;
      double denominator = 0;
      String[] divide = msj.split("\\P{Alpha}+");
      for(int i = 0; i < divide.length; i++){
          Triad in_spam = null;
          Triad in_inbox = null;
          boolean end = false;
          Iterator it;
          it = spam[divide[i].charAt(0) - 97].iterator();
          while(it.hasNext() && !end){
              in_spam = (Triad) it.next();
              if(in_spam.getWord().equalsIgnoreCase(divide[i])){
                  end = true;
              }
          }
          if(end){
              end = false;
              it = inbox[divide[i].charAt(0) - 97].iterator();
              while(it.hasNext() && !end){
                  in_inbox = (Triad) it.next();
                  if(in_inbox.getWord().equalsIgnoreCase(divide[i])){
                      end = true;
                  }
              }

              if(end){
                  numerator += (in_spam.getProbability() * spam_probability);
                  denominator += ((in_spam.getProbability() * spam_probability)
                                    + (in_inbox.getProbability() * (1 - spam_probability)));
              }
          }
      }
      message_probability = (numerator / denominator);
      return message_probability;
  }

}  // end of class

