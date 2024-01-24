import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.security.*;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.*;
import java.lang.reflect.Type;

//nel server andrò ad istanziare un User per ogni connessione
public class User {
    private static List<User> listautenti = new ArrayList<>();
    private static long boundseme = 1000000000;
    private static final Type hm_type = new TypeToken<ArrayList<User>>(){}.getType();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private String username;
    private int livello=0;
    private int numero_recensioni_fatte=0;
    private Couple coppia;
    
    public User(String username) {
        this.username = username;
        this.livello = 1;
        this.numero_recensioni_fatte = 0;
        this.coppia = new Couple("", "");
    }
    /**
     * funzione usata per aggiornare il file JSON con nuovi utenti registrati
     * @param nomefile
     * @throws IOException
     */
    public static void aggiorna_utenti(String nomefile) throws IOException {

        
        JsonWriter jsonWriter = new JsonWriter(new FileWriter(nomefile, false));
        jsonWriter.setIndent("  ");
        synchronized(listautenti) {
            gson.toJson(listautenti, hm_type, jsonWriter);
        }
        jsonWriter.flush();
        jsonWriter.close();
       


    }
        
    
    /**
     * funzione usata per  recuperare gli utenti già registrati in precedenza
     * @param nomefile, il nome del file JSON da cui recuperare l'elenco degli utenti già registrati
     * @throws IOException
     */
    public static void recupera_utenti(String nomefile) throws IOException {
        
        File file = new File(nomefile);
        if(!file.exists() || file.length()==0) {
            listautenti = new ArrayList<>();
            return;
        }
        JsonReader reader = new JsonReader(new FileReader(nomefile));
        listautenti = gson.fromJson(reader, hm_type);
        reader.close();

        if(listautenti == null)
            listautenti = new ArrayList<>();
    }
    /**
     * Questo metodo è fondamentale per il log in e la registrazione se si vuole evitare di salvare in memoria la password.
     * @param stringa
     * @return il risultato di SHA-256 applicato alla password
     */
    private static String hash_one_way(String stringa) {
        StringBuilder hexString = new StringBuilder();;
        try {
            // Ottieni un'istanza di MessageDigest per l'algoritmo SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Ottenere l'array di byte della stringa
            byte[] stringBytes = stringa.getBytes();
            
            // Aggiorna il digest con i byte della stringa
            byte[] hashBytes = digest.digest(stringBytes);
            
            // Converti l'array di byte in una rappresentazione esadecimale
            hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Throwable e) {
            System.err.printf("[USER]: %s\n",e.getMessage());
            e.printStackTrace();
        }
        return hexString.toString();
    }

    /**
     * @param username
     * @param password
     * @return true se la registrazione è andata a buon fine, false altrimenti.
     */
    public static boolean register(String username, String password) {
        User utente = new User(username);
        if(listautenti.contains(utente))
            return false;
        if(password.length()==0)
            return false;
        long semelong = ThreadLocalRandom.current().nextLong(boundseme);
        String seme = Long.toString(semelong);
        String res = hash_one_way(password+seme);

        Couple coppia = new Couple(seme, res);
        
        utente.coppia = coppia;

        listautenti.add(utente);

        return true;

    }

    /**
     * @param username
     * @param password
     * @return true un'istanza di User corrispondente a username se esiste, null altrimenti
     */
    public static User login(String username, String password) {

        User utente = new User(username);
        boolean trovato = false;
        synchronized(listautenti) {
            if(listautenti.isEmpty())
                return null;
            for(User u : listautenti) {
                if(utente.equals(u)) {
                    utente = u;
                    trovato = true;
                }
                    
            }
            if(!trovato)
                return null;
        }
        Couple coppia = utente.getcoppia();
        String seme = coppia.getseme();
        String rescoppia = coppia.getres();
        if(hash_one_way(password+seme).equals(rescoppia)) {
            return utente;
        }
        return null;
        
    }

    /**
     * @param username
     * @return true se il logout è andato a buon fine, false altrimenti.
     */
    public static boolean logout(String username) {
        
        User utente = new User(username);
        synchronized(listautenti) {
            for (User u: listautenti) {
                if(u.equals(utente))
                    return true;
            }
            return false;
        }
    }

    

    
    /**
     * fa in modo ogni 5 recensioni fatte, l'utente aumenta di livello.
     */
    public void inserisci_recensione() {
        
        this.incrementanumrecensioni();
        //ho già raggiunto il livello massimo
        if(this.getlivello()==5) 
            return;
        if(this.getnumrecensioni()%5 !=0)
            return; 
        this.incrementalivello();;
        return;
        
    }

    public boolean equals(Object o) {
        if(o instanceof User) 
            return equals((User) o);
        else return false;
    }

    public boolean equals (User u) {
        return this.username.equals(u.getusername());
    }

    public Couple getcoppia() {
        String seme = this.coppia.getseme();
        String res = this.coppia.getres();
        return new Couple(seme,res);
    }
    public String getusername() {
        return this.username;
    }
    /**
     * @return un intero da 1 a 5 che corrisponde al distintivo dell'utente.
     */
    public int getlivello() {
        return this.livello;
    }
    public int getnumrecensioni() {
        return this.numero_recensioni_fatte;
    }
    public void incrementanumrecensioni() {
        this.numero_recensioni_fatte++;
    }
    public void incrementalivello() {
        this.livello++;
    }
}




class Couple {
    private String seme, res;
    
    public Couple(String seme, String res) {
        this.seme = seme;
        this.res = res;
    }
    public String getseme() {
        return this.seme;
    }
    public String getres() {
        return this.res;
    }
}

