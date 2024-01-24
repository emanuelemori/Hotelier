import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.beans.IntrospectionException;
import java.io.*;
import java.net.*;
import javax.naming.CommunicationException;
//import uni.LABORATORIOIII.progetto.Server.HOTELIERServerMain;


/*TODO:
 * gestire inizializzazione connessione
 * definire le funzioni
 * vedi se lanciare un'eccezione quando scegli di fare un'op che non puoi fare
 * ELIMINA GLI IF(STATO) OP ILLEGALE, inutili pk tanto chiami la fne check
*/
public class HOTELIERCustomerClientMain {
    public static final String configFile = "client.properties";
    // Nome host e porta del server.
    public static String hostname, udpAddress; 
    public static int udpPort;
    public static int port; 
    // Socket e relativi stream di input/output.
    private static Scanner scanner = new Scanner(System.in);
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    //variabile booleana che tiene conto se l'utente è loggato o no. true se loggato, false altrimenti
    private static volatile boolean stato =false; 
    //variabile booleana che tiene conto se l'operazione che l'utente vuole effettuare è consentita
    private static boolean operazioneconsentita;
    //variabile aleatoria usata per non ricevere più input da terminale in caso l'utente voglia terminare la sessione
    private static boolean chiudiscanner = false;


    /**
     * @param stato, variabile booleana che vale true se l'utente è loggato, false altrimenti
     * @param comando, intero che specifica il tipo di comando
     * @return true se l'operazione è consentita, false altrimenti
     */
    private static boolean check(boolean stato,int comando) {
        if((comando<-1)|| comando>=8) {
            System.out.println("Operazione illegale");
            return false;
        }
            
        //se non sei loggato non puoi digitare i seguenti comandi
        if(!stato) {
            if(comando==3 || (comando>=5 && comando<=7)) {
                System.out.println("Operazione illegale");
                return false;
            }
        }
        //se sei loggato non puoi digitare i seguenti comandi
        else {
            if(comando>=1 && comando <=2) {
                System.out.println("Operazione illegale");
                return false;
            }
        }
        return true;
    }

    /** 
     * @param username 
     * @param password
     * stampa su schermo se l'operazione è andata a buon fine o no
    */
    public static void register(String username, String password) {
        if(password.length()==0) {
            System.out.println("Registrazione non riuscita");
            return;
        }

        try {
            //comunico al server che il client vuole registrarsi
            out.println(1);
            //spedisco il nome utente
            out.println(username);
            //spedisco la password
            out.println(password);

            String reply = in.readLine();
            String[] parts = reply.split(",");
            int res = Integer.parseInt(parts[0]);
            if(res==1)
                System.out.println("Utente registrato correttamente");
            else
                System.out.println("Registrazione non riuscita");
        }
        catch(IOException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
    }

    /** 
     * stampa su schermo se l'operazione è andata a buon fine o no.
     * restituisce un booleano perché il suo risultato sarà assegnato alla variabile stato.
     * stato vale true se l'utente è loggato, false altrimenti.
     * @param username 
     * @param password
     * @return true se il login è avvenuto con successo, false altrimenti
    */
    public static boolean login(String username, String password) {
        int res =0;
        try {
            out.println(2);
            //spedisco il nome utente
            out.println(username);
            //spedisco la password
            out.println(password);

            String reply = in.readLine();
            String[] parts = reply.split(",");
            res = Integer.parseInt(parts[0]);
            
        }
        catch(IOException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
        if(res==1) {
            System.out.println("Accesso riuscito");
            return true;
        }
        else {
            System.out.println("Accesso non riuscito");
            return false;
        }
    }

    /** 
     * stampa su schermo se l'operazione è andata a buon fine o no.
     * restituisce un booleano perché il suo risultato sarà assegnato alla variabile stato.
     * stato vale true se l'utente è loggato, false altrimenti.
     * @param username 
     * @return false se il logout è avvenuto con successo, true altrimenti.
     * 
    */
    public static boolean logout(String username) {
        int res=0;
        try {
            out.println(3);
            //spedisco il nome utente
            out.println(username);
            
            String reply = in.readLine();
            String[] parts = reply.split(",");
            res = Integer.parseInt(parts[0]);    
        }
        catch(IOException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
        if(res==1) {
            
            System.out.println("Log-out riuscito");
            return false;
        }    
        else {
            System.out.println("Log-out non riuscito");
            return true;
        }
    }

    /**
     * Si limita a spedire al server il nome dell'hotel e la rispettiva città.
     * Si assicura di ricevere la completa descrizione dell'hotel richiesta e la stampa su terminale.
     * @param nomeHotel
     * @param città
     */
    public static void searchHotel(String nomeHotel, String città) {
        out.println(4);
        //spedisco il nomeHotel
        out.println(nomeHotel);
        //spedisco la città
        out.println(città);
        
        leggihotel();
    }

    /**
     * Spedisce al server il nome della città di cui l'utente vuole visionare il ranking degli hotel.
     * Stampa su terminale quanto ricevuto.
     * @param città
     */
    public static void serchAllHotels(String città) {
        out.println(5);
        
        //spedisco la città
        out.println(città);

        leggihotel();
    }    

    /**
     * Si occupa di leggere da socket la descrizione di uno o pià hotel
     */
    private static void leggihotel() {
        try {
            int dimensione = Integer.parseInt(in.readLine());
            StringBuilder res = new StringBuilder();
            int letti = 0;
            String line;
            
            while(letti<dimensione-2) {
                line = in.readLine();
                if(line==null)
                    break;
                res.append(line);
                res.append("\n");
                letti+=line.length()+1;
                
            }
            
            System.out.println(res);
            
        } catch( IOException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
        
    }
    /**
     * spedisce al server nomehotel e citta per verificare se l'hotel che si vuole recensire esiste
     * @param nomehotel
     * @param nomeCittà
     * @return 1 se l'hotel che si vuole recensire esiste, 0 altrimenti
     */
    public static int tryInsertReview(String nomehotel, String nomeCittà) {
        int res=0;
        try {
            out.println(6);
            //spedisco il nomeHotel
            out.println(nomehotel);
            //spedisco la città
            out.println(nomeCittà);
            String reply = in.readLine();
            String[] parts = reply.split(",");
            res = Integer.parseInt(parts[0]);
            if(res==0)
                System.out.println("Hotel o Città non trovati");
            return res;

        }
        catch(IOException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
        return res;
    }

    /**
     * Comunica al server il punteggio da attribuire all'hotel comunicato con tryInsertReview
     * Stampa su terminale se la registrazione della recensione è avvenuta con successo o no.
     * @param GlobalScore
     * @param SingleScores
     */
    
     public static void insertReview(int GlobalScore, int [] SingleScores ) {
        try {
            
            out.println(GlobalScore);
            for(int score: SingleScores) 
                out.println(score);

            String reply = in.readLine();
            String[] parts = reply.split(",");
            int res = Integer.parseInt(parts[0]);
            if(res==1)
                System.out.println("Recensione registrata con successo");
            else
                System.out.println("Recensione non registrata");
        }
        catch(IOException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
    }

    /**
     * Comunica al server che l'utente vuole visualizzare il proprio distintivo.
     * Riceve il tipo di distintivo sottoforma di intero, lo decodifica e stampa il tipo di distintivo su terminale.
     * @throws CommunicationException
     */
    public static void showMyBadges() throws CommunicationException {
        try {
            out.println(7);
            String reply = in.readLine();
            String[] parts = reply.split(",");
            int livello = Integer.parseInt(parts[0]);

            switch(livello) {
                case 1:
                    System.out.println("Recensore");
                    break;
                case 2:
                    System.out.println("Recensore esperto");
                    break;
                case 3:
                    System.out.println("Contributore");
                    break;
                case 4:
                    System.out.println("Contributore esperto");
                    break;
                case 5:
                    System.out.println("Contributore super");
                    break;
                default:
                    throw new CommunicationException();
            }
        }
        catch(IOException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
    }

    /**
     * comunica al server che l'utente vuole terminare la connessione.
     */
    public static void goodbye() {
        out.println(-1);
    }
    
    

    public static class RicevitoreUDP implements Runnable{

        @Override
        public void run() {
            try {
                
                
                byte[] buffer = new byte[1024];
                /* Inet address con indirizzo di multicast */
                InetAddress ia = InetAddress.getByName(udpAddress);
                /* creazione del multicast socket */
                MulticastSocket ms = new MulticastSocket(udpPort);
            
                ms.joinGroup(ia);
                while (stato && !Thread.currentThread().isInterrupted()) {
                    
                    /* preparazione del datagram packet per la ricezione del messaggio */
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    //ms.setSoTimeout(1000);
                    try {
                        ms.setSoTimeout(1000);
                        ms.receive(packet);
                        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                        
                        System.out.println(msg);
                    }
                    catch (Throwable e) {
                        if(e instanceof SocketTimeoutException)
                            continue;
                        else if(e instanceof InterruptedException) {
                            ms.leaveGroup(ia);
                            ms.close();
                            stato = false;
                        }
                            
                        else 
                            e.printStackTrace();
                    }
                    
                }
                ms.leaveGroup(ia);
                ms.close();
            }
            catch (IOException e){
                System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
                e.printStackTrace();
            }
        }
    }

    public static void main(String [] args ) {
        try {
            if(args.length!=0)
                throw new IllegalArgumentException("uso file: java HOTELIERCustomerClientMain.java");
        }
        catch(IllegalArgumentException e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
        System.out.println("------------------HOTELIER------------------");
        try {
            readConfig();
            
            socket = new Socket(hostname, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            Runtime.getRuntime().addShutdownHook(
                //definisco le azioni da compiere in caso di hook
                new Thread(() -> {
                    System.out.println("[CLIENT] Avvio terminazione...");
                    System.out.println("[CLIENT] Se l'attesa continua, premere invio");
                    try {
                        chiudiscanner = true;
                        scanner.close();
                        in.close();
                        out.close();
                        socket.close();
                        stato = false;
                    }
                    catch(IOException e) {
                        System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
                        e.printStackTrace();
                    }
                    System.out.println("[CLIENT] Terminato");
                }
            ));
            

            Thread RicevitoreUDP = null;
            
            //continua varrà false quando il client vorrà chiudere la sessione
            boolean continua = true;
            while(continua) {
                if(stato) 
                    System.out.println("\ndigita\n3 per fare il log out\n"+
                "4 per cercare un hotel di una città\n"+
                "5 per cercare tutti gli hotel di una città\n"+
                "6 per inserire una recensione\n"+
                "7 per vedere il tuo distintivo\n"+
                "-1 per uscire\n");
                else 
                    System.out.println("\ndigita\n1 per registrarti\n"+
                "2 per fare il log in\n"+
                "4 per cercare un hotel di una città\n"+
                "-1 per uscire\n");
                int comando;
                try {
                    //se lo scanner è ancora aperto
                    //cioè l'utente non ha ancora premuto ^C
                    if(!chiudiscanner)
                        comando = Integer.parseInt(scanner.nextLine());
                    //se l'utente ha premuto -1
                    else {
                        comando = -1;
                        continua = false;
                    }
                }
                catch(NumberFormatException e) {
                    comando = 0;
                } 

                operazioneconsentita = check(stato, comando);
                if(!operazioneconsentita) 
                    continue;
                String nomeutente, password, città, nomehotel;
                switch (comando) {
                    case 1: //registrazione
                        System.out.println("scegli un nome utente:");
                        nomeutente = scanner.nextLine();
                        System.out.println("scegli una password:");
                        password = scanner.nextLine();
                        register(nomeutente, password);
                            
                        break;
                    case 2: //login
                        System.out.println("nome utente:");
                        nomeutente = scanner.nextLine();
                        System.out.println("password:");
                        password = scanner.nextLine();
                        stato = login(nomeutente, password);
                        if(stato) {
                            RicevitoreUDP = new Thread(new RicevitoreUDP());
                            RicevitoreUDP.start();
                        }
                        break;
                    case 3: //logout
                        System.out.println("nome utente:");
                        nomeutente = scanner.nextLine();
                        stato = logout(nomeutente);
                        //se ho effettuato il logout, allora smetto di essere in attesa
                        if(!stato) {
                            RicevitoreUDP.interrupt();
                            RicevitoreUDP.join();
                        }
                        break;
                    case 4: //cercare un hotel di una città
                        System.out.println("nome hotel:");
                        nomehotel = scanner.nextLine();    
                        System.out.println("città:");
                        città = scanner.nextLine();
                        searchHotel(nomehotel, città);
                        break;
                    case 5: //cercare tutti gli hotel di una città
                        System.out.println("città:");
                        città = scanner.nextLine();
                        serchAllHotels(città);
                        break;
                    case 6: //recensione
                        System.out.println("nome hotel:");
                        nomehotel = scanner.nextLine();    
                        System.out.println("città:");
                        città = scanner.nextLine();
                        if(tryInsertReview(nomehotel, città)==0)
                            break;

                        System.out.println("Punteggio globale:");
                        int GlobalScore = Integer.parseInt(scanner.nextLine());
                        //Posizione, Pulizia, Servizio, Prezzo
                        System.out.println("Punteggio posizione:");
                        int puntiPosizione = Integer.parseInt(scanner.nextLine());
                        System.out.println("Punteggio pulizia:");
                        int puntiPulizia = Integer.parseInt(scanner.nextLine());
                        System.out.println("Punteggio servizio:");
                        int puntiServizio = Integer.parseInt(scanner.nextLine());
                        System.out.println("Punteggio prezzo:");
                        int puntiPrezzo = Integer.parseInt(scanner.nextLine());
                        int [] SingleScores = {puntiPosizione, puntiPulizia, puntiServizio, puntiPrezzo};
                        insertReview(GlobalScore, SingleScores);
                        break;
                    case 7: //distintivo
                        showMyBadges();
                        break;
                    case -1: //exit
                        goodbye();
                        continua= false;
                        if(stato) {
                            RicevitoreUDP.interrupt();
                            RicevitoreUDP.join();
                        }
                        
                        break;
                    case 0:
                        break;
                    default:
                        break;
                }
            }
            in.close();
            out.close();
            scanner.close();
            socket.close();
            System.out.println("------------------ARRIVEDERCI------------------");
        }
        catch(Throwable e) {
            System.err.printf("[CLIENT] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
    }

    /**
    * Metodo che legge il file di configurazione del client.
    * @throws FileNotFoundException se il file non esiste
    * @throws IOException se si verifica un errore durante la lettura
    */
    public static void readConfig() throws FileNotFoundException, IOException {
        InputStream input = HOTELIERCustomerClientMain.class.getResourceAsStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        udpPort = Integer.parseInt(prop.getProperty("udpPort"));
        udpAddress = prop.getProperty("udpAddress");
        input.close();
    }
}


