import java.util.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.*;
import java.util.concurrent.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import com.google.gson.annotations.Expose;



/*
 TO DO:
 * FAI PARADIGMA LETTORE-SCRITTORE per vedere il ranking e fare recensioni
 */

public class Hotel {
    
    private String nome;
    //private ElencoCitta cittaenum;
    //DEVE ESSERE THREAD SAFE
    static ArrayList <Hotel> listahotel;

    private String nomecitta;
    private int id;
    private String descrizione;
    private String telefono;    
    private String [] servizi;
    private int rate;
    private ratings ratings;
    

    public transient List <Nupla> GlobalScoreArray;
    
    private transient List <Nupla> posizioneArray, puliziaArray, servizioArray, prezzoArray;
    
    public transient Citta citta;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    /*  tipo utilizzato dal metodo getUsers() per de-serializzare gli utenti in una ConcurrentHashMap*/
    private static final Type hm_type = new TypeToken<ArrayList<Hotel>>(){}.getType();


    
    //per ogni hotel ho diverse liste, ognuna per ogni tipo di servizio.
    //così poi per calcolare il ranking avrò un metodo che fa la media pesata 
    public Hotel (String nome, String nomecitta, 
        int id, String descrizione, String telefono, String [] servizi, int rate, ratings ratings) {
        this.nome = nome;
        this.nomecitta = nomecitta;
        this.citta = Citta.buildcitta(nomecitta);
        this.id = id;
        this.descrizione = descrizione;
        this.telefono = telefono;
        this.servizi = servizi;

        this.GlobalScoreArray = new LinkedList<>();
        this.posizioneArray=new LinkedList<>();
        this.puliziaArray=new LinkedList<>();
        this.servizioArray=new LinkedList<>();
        this.prezzoArray = new LinkedList<>(); 
        
        this.ratings = ratings;  
        this.rate = rate;    
    }

    /**
     * Scorre la lista degli hotel e restituisce l'oggetto di tipo Hotel corrispondente, se esiste
     * @param nome
     * @param citta
     * @return un oggetto di tipo Hotel, corrispondente all'Hotel con nome "nome" e città "citta"
     */
    public static synchronized Hotel buildHotel(String nome, String citta) {
        Hotel hotelnuovo = new Hotel(nome, citta,0,"","", new String [0],0,new ratings(0, 0, 0, 0));
        for (Hotel h : listahotel) {
            if (h.equals(hotelnuovo)) {
                return h; // Restituisce l'istanza uguale trovata
            }
        }
        
        return null;
    }

    /**
     * recupera dal file JSON la lista degli hotel
     * @param filenome
     */
    public static void importadajson(String filenome) {
        
        try {
            
            JsonReader reader = new JsonReader(new FileReader(filenome));
            listahotel = gson.fromJson(reader, hm_type);
            if (listahotel == null) {
                listahotel = new ArrayList <Hotel> (Integer.MAX_VALUE);
            }
            reader.close();
            
        }
        catch(IOException e) {
            System.err.printf("[HOTEL] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
        
        
        for(Hotel h: listahotel) {            
            String nomedicitta = h.getnomecitta();
            //crea un'istanza di Citta corrispondente a nomecitta ed evita che nella lista di Citta vi siano duplicati
            Citta.creacittadajson(nomedicitta);
            h.citta = Citta.buildcitta(nomedicitta);
            h.citta.listaHotel.add(h);

            h.GlobalScoreArray = new LinkedList<>();
            h.GlobalScoreArray.add(new Nupla(null, h.rate));

            h.posizioneArray= new LinkedList<>();
            h.posizioneArray.add(new Nupla(null, h.ratings.posizione));

            h.prezzoArray = new LinkedList<>();
            h.prezzoArray.add(new Nupla(null, h.ratings.prezzo));

            h.puliziaArray = new LinkedList<>();
            h.puliziaArray.add(new Nupla(null, h.ratings.pulizia));

            h.servizioArray = new LinkedList<>();
            h.servizioArray.add(new Nupla(null, h.ratings.servizi));
        }
        //per ogni città ordino gli hotel in base al ranking
        for(Citta c: Citta.listacitta) {
            c.sorthotelincitta();
        }
    }
    
    /**
     * aggiorna il file JSON contenente la lista degli hotel
     * @param nomefile
     */
    public synchronized static void aggiorna_hotel(String nomefile) {
        try {
            JsonWriter jsonWriter = new JsonWriter(new FileWriter(nomefile));

            jsonWriter.setIndent("  ");

            jsonWriter.beginArray(); // Inizia l'array JSON

            for (Hotel h : listahotel) {
                gson.toJson(h, Hotel.class, jsonWriter); // Serializza l'oggetto Hotel nel file JSON
            }

            jsonWriter.endArray(); // Fine dell'array JSON
            jsonWriter.close();
        }
        catch (IOException e) {
            System.err.printf("[HOTEL] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
    }
    
    /**
     * inserisce una recensione
     * @param citta
     * @param GlobalScore
     * @param SingleScores
     * @param Utente
     * @return un array di booleani di dimensione due. Il primo elemento indica se l’inserimento della recensione ha avuto luogo o meno. Il secondo elemento indica se l’inserimento della recensione ha fatto in modo che l’hotel in prima posizione del ranking sia cambiato. In tal caso, sarà aggiornato a riguardo ogni utente attualmente loggato.
     */
    public synchronized boolean [] inserisci_recensione(String citta, 
        int GlobalScore, int [] SingleScores, User Utente) {
        //il metodo è synchronized affinché non vengano inserite più recensioni sullo stesso hotel contemporaneamente
        boolean [] res = new boolean[2];
        res[0] = false;
        res[1] = false;
        Citta citta2 = Citta.buildcitta(citta);
        if(citta2==null) {
            return res;
        }
        if(GlobalScore<0 || GlobalScore>5)
            return res;
        for(int i =0;i<SingleScores.length;i++)
            if(SingleScores[i]<0 || SingleScores[i]>5)
                return res;
        
        //se questo hotel non è in citta restituisco false
        //non è un problema se questa scansione viene fatta da più thread contemporaneamente
        //perché la lista degli hotel di ciascuna citta non viene mai modificata
        if(!citta2.listaHotel.contains(this)) 
            return res;
        
        this.GlobalScoreArray.add(new Nupla( Utente, GlobalScore));
        this.posizioneArray.add(new Nupla(Utente, SingleScores[0]));
        this.puliziaArray.add(new Nupla(Utente, SingleScores[1]));
        this.servizioArray.add(new Nupla(Utente, SingleScores[2]));
        this.prezzoArray.add(new Nupla(Utente, SingleScores[3]));
        
        this.rate = calcola_media(GlobalScoreArray);
        this.ratings.posizione = calcola_media(posizioneArray);
        this.ratings.pulizia = calcola_media(puliziaArray);
        this.ratings.prezzo = calcola_media(prezzoArray);
        this.ratings.servizi = calcola_media(servizioArray);

        
        //dopo aver salvato la recensione, riordino il ranking della citta dell'hotel
        res[0] = true;
        res[1] = this.citta.sorthotelincitta(); 
        return res;
    }

    private synchronized int calcola_media(List <Nupla> listarecensioni) {
        int dimensione = listarecensioni.size();
        if(dimensione==0)
            return 0;
        int somma =0;
        
        for(Nupla nupla: listarecensioni) {
            somma+=nupla.getpunteggio();
        }
        return somma/dimensione;
    }


    public synchronized long calcola_media_date() {
        List <Nupla> listarecensioni = GlobalScoreArray;
        int dimensione = listarecensioni.size();
        if(dimensione==0)
            return 0;
        long somma = 0;
        for(Nupla nupla : listarecensioni) {
            Date data = nupla.getdata();
            somma+=data.getTime();
        }
        return somma/dimensione;
    }


    /**
     * 
     * @return rappresentazione formato stringa dell'hotel
     */
    public synchronized String toString() {
        
        String res;
        res = "nome hotel: " + nome + "\n";
        res += "citta: " + nomecitta + "\n";
        res += ("punti: "+ rate+"\n");
        res += ("punti posizione: "+ ratings.posizione+"\n");
        res += ("punti pulizia: "+ ratings.pulizia+"\n");
        res += ("punti servizio: "+ ratings.servizi+"\n");
        res += ("punti prezzo: "+ ratings.prezzo+"\n");

        int posizioneranking = this.citta.getindice(this);
        res+= ("posizione ranking: "+ posizioneranking + "\n") ;
        res+=("descrizione: " + this.descrizione+"\n");
        res+=("telefono: " + this.telefono+"\n");
        if(this.servizi.length!=0) {
            res+="servizi:";
            for(String servizio : servizi) {
                res+=(" "+ servizio);
            }
            res+="\n";
        }
        res+="\n";
        return res;
    }

    public boolean equals (Hotel h) {
        return this.nome.equals(h.nome) && this.citta.equals(h.citta);
    }

    public boolean equals (Object h) {
        if(h instanceof Hotel) 
            return equals((Hotel) h);
        else return false;
    }
    
    public int getpunti () {
        return this.rate;
    }
    public String getnome () {
        return this.nome;
    }
    public String getnomecitta() {
        return this.nomecitta;
    }

    private static class ratings {
        public int pulizia, posizione, servizi, prezzo;
        public ratings(int pulizia, int posizione, int servizi, int prezzo) {
            this.pulizia = pulizia;
            this.posizione = posizione;
            this.servizi = servizi;
            this.prezzo = prezzo;
        }
    }
    
}



class Nupla {
    private Date data;
    private User utente;
    private double punteggio;

    public Nupla ( User utente, int punteggio) {
        this.data = new Date();
        this.utente = utente;
        this.punteggio = punteggio;
    }

    public double getpunteggio() {
        return this.punteggio;
    }

    public Date getdata() {
        return this.data;
    }
    public User getutente() {
        return this.utente;
    }
}
