import java.util.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.CommunicationException;
import java.security.spec.InvalidParameterSpecException;
import java.time.Duration;


public class HOTELIERServerMain {
    public static final String configFile = "server.properties";
    public static int port;
    public static int maxDelay; 
    public static int periodicDelay;
    public static String filehotel;
    private static ScheduledExecutorService scheduler;
    private static int coreCount = Runtime.getRuntime().availableProcessors();
    public static final ExecutorService pool = new ThreadPoolExecutor(
        coreCount, coreCount*5, 1000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue <Runnable>(5)
        );
    public static ServerSocket serverSocket;
    private static int udpPort;
    private static String udpAddress;
    private static String fileutenti;

    private static AtomicInteger lettoriinattesa = new AtomicInteger(0);
    private static AtomicInteger scrittoriinattesa = new AtomicInteger(0);
    private static AtomicInteger lettoriattivi = new AtomicInteger(0);
    private static AtomicInteger scrittoriattivi = new AtomicInteger(0);
    

    //variabile di condizione su cui i thread si sospenderanno/attiveranno per interagire con gli hotel
    public static Object cv = new Object();
    public static class Aggiornatore implements Runnable {
        @Override
        public void run() {
            try {
                User.aggiorna_utenti(fileutenti);
                acquisisciscrittura();
                Hotel.aggiorna_hotel(filehotel);
                rilasciascrittura();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
        }
    }
    
    public static void acquisiscilettura() {
        lettoriinattesa.incrementAndGet();
        while(scrittoriattivi.get()>0)
            try {
                cv.wait();
            }
            catch(InterruptedException e) {
                System.err.printf("[SERVER] Errore: %s\n", e.getMessage()); 
                e.printStackTrace();
            }
        lettoriinattesa.decrementAndGet();
        lettoriattivi.incrementAndGet();
    }

    public static void rilascialettura() {
        synchronized(cv) {
            lettoriattivi.decrementAndGet();
            cv.notifyAll();
        }
        
    }

    public static void acquisisciscrittura() {
        scrittoriinattesa.incrementAndGet();
        try {
            while(scrittoriattivi.get()>0 || lettoriattivi.get()>0)
                cv.wait();
        } catch (Exception e) {
            System.err.printf("[SERVER] Errore: %s\n", e.getMessage()); 
            e.printStackTrace();
        }
        
        scrittoriinattesa.decrementAndGet();
        scrittoriattivi.incrementAndGet();
    }

    public static void rilasciascrittura() {
        synchronized(cv) {
            scrittoriattivi.decrementAndGet();
            cv.notifyAll();
        }
        
    }

    public static void main(String [] args) {
        try {
            readConfig();
            serverSocket = new ServerSocket(port);
            Runtime.getRuntime().addShutdownHook(
                new ServerTerminationHandler(maxDelay, pool, serverSocket, fileutenti, filehotel)
            );
            
            
            //recupero le informazioni dai file json
            Hotel.importadajson(filehotel);
            User.recupera_utenti(fileutenti);
            
            scheduler = Executors.newSingleThreadScheduledExecutor();
            Runnable aggiornatore = new Aggiornatore();
            int initialDelay = 0;
            int periodicDelay = (int) Duration.ofMinutes(1).toMinutes();
            
            //lancio il thread che aggiornerà i file JSON ogni minuto
            scheduler.scheduleWithFixedDelay(aggiornatore, initialDelay, periodicDelay, TimeUnit.SECONDS);
            
            while (true) {
                Socket socket = null;
            // Accetto le richieste provenienti dai client.
                try {
                    socket = serverSocket.accept();
                }
                catch (SocketException e) {
                    break;
                }
                //lancio il thread che si occupa di gestire la connessione con il client
                pool.execute(new Worker(socket,udpAddress,udpPort));
            }
        } 

        catch (Exception e) {
            System.err.printf("[SERVER]: %s\n",e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
    }
    /**
    * Metodo che legge il file di configurazione del server.
    * @throws FileNotFoundException se il file non esiste
    * @throws IOException se si verifica un errore durante la lettura
    */
    public static void readConfig() throws FileNotFoundException, IOException {
        InputStream input = HOTELIERServerMain.class.getResourceAsStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        maxDelay = Integer.parseInt(prop.getProperty("maxDelay"));
        filehotel = prop.getProperty("hoteljson");
        fileutenti = prop.getProperty("usersFile");
        udpPort = Integer.parseInt(prop.getProperty("udpPort"));
        udpAddress = prop.getProperty("udpAddress");
        periodicDelay = Integer.parseInt(prop.getProperty("periodicDelay"));
        input.close();
    }

    public static class Worker implements Runnable {
        private User utente;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String udpAddress;
        private int udpPort;
        
        

        public Worker(Socket socket, String udpAddress,int udpPort ) {
            this.socket = socket;
            this.utente = null;
            this.udpAddress = udpAddress;
            this.udpPort = udpPort;
            
        }

        /**
         * @param a, risultato booleano di un metodo di User o di Hotel
         * @return 1 se a vale true, 0 altrimenti
         */
        private int convertires(boolean a) {
            return a ? 1 : 0;
        }

        

       
        /**
         * spedisce al client una stringa di dimensione notevole.
         * Per far ciò prima spedisce la dimensione della stringa e poi la stringa stessa
         * @param descrizionehotel, la descrizione in formato stringa dell'hotel
         */
        private void sendtoClient(String descrizionehotel) {
            
            int dimensione = descrizionehotel.length();
            String [] array = descrizionehotel.split("\n");
            out.println(dimensione);
            for(String stringa: array) {
                out.println(stringa);
            }
        }

        /**
         * crea una MulticastSocket e spedisce message via UDP
         * @param message
         * @param ipAddress
         * @param port
         * @throws IOException
         */
        public static synchronized void inviaMessaggioUDP(String message, String ipAddress, int port) throws IOException {
            MulticastSocket ms = new MulticastSocket(port);
            InetAddress group = InetAddress.getByName(ipAddress);
            byte[] msg = message.getBytes();
            DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
            ms.send(packet);
            ms.close();
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                int comandoricevuto;
                boolean res;
                String username, password = "";
                String nomehotel, citta;
                String hoteltoString;
                Citta citta2;
                Hotel hotel;
                while (true) {
                    comandoricevuto = Integer.parseInt(in.readLine());
                    if(comandoricevuto==-1)
                        break;
                    switch (comandoricevuto) {
                        case 1: //il client vuole registrarsi
                            username = in.readLine();
                            password = in.readLine();
                            res = User.register(username, password);
                            User.aggiorna_utenti(fileutenti);
                            out.println(convertires(res));
                            break;
                    
                        case 2: //il client vuole effettuare il login
                            username = in.readLine();
                            password = in.readLine();
                            utente = User.login(username, password);
                            if(utente==null)
                                res = false;
                            else 
                                res =true;
                            out.println(convertires(res));
                            break;
                        
                        case 3: //il client vuole effettuare il logout
                            username = in.readLine();
                            res = User.logout(username);
                            out.println(convertires(res));
                            break;

                        case 4: //il client vuole ricevere la descrizione di un hotel
                            nomehotel = in.readLine();
                            citta = in.readLine();
                            hotel = Hotel.buildHotel(nomehotel, citta);
                            if(hotel!=null) {
                                acquisiscilettura();
                                sendtoClient(hotel.toString());
                                rilascialettura();
                            }
                                
                            else
                                sendtoClient("HOTEL NON TROVATO");
            
                            break;

                        case 5: //il client vuole ricevere la descrizione di tutti gli hotel di una città
                            citta = in.readLine();
                            citta2 = Citta.buildcitta(citta);
                            if(citta2!=null) {
                                acquisiscilettura();
                                hoteltoString = citta2.hotelincittatoString();
                                sendtoClient(hoteltoString);
                                rilascialettura();
                            }
                            else
                                sendtoClient("CITTÀ NON TROVATA");
                            break;

                        case 6: //il client vuole inserire una recensione
                            nomehotel = in.readLine();   
                            citta = in.readLine();

                            hotel = Hotel.buildHotel(nomehotel,citta);
                            if(hotel == null) {
                                out.println(convertires(false));
                                break;
                            }
                            else
                                out.println(convertires(true));
                                


                            int GlobalScore = Integer.parseInt(in.readLine());

                            int puntiPosizione = Integer.parseInt(in.readLine());
                            int puntiPulizia = Integer.parseInt(in.readLine());
                            int puntiServizio = Integer.parseInt(in.readLine());
                            int puntiPrezzo = Integer.parseInt(in.readLine());

                            int [] punti = {puntiPosizione,puntiPulizia,puntiServizio,puntiPrezzo};
                            
                           
                            
                            acquisisciscrittura();
                            boolean [] resRecensione = hotel.inserisci_recensione(citta,GlobalScore,punti,utente);
                            res = resRecensione[0];
                            utente.inserisci_recensione();
                            rilasciascrittura();
                            if(resRecensione[1])
                                inviaMessaggioUDP(nomehotel + " a " + citta + " in testa", udpAddress,udpPort);
                            Hotel.aggiorna_hotel(filehotel);
                            out.println(convertires(res));
                            
                            break;

                        case 7: //il client vuole visionare il proprio distintivo
                            int livello = utente.getlivello();
                            out.println(livello);
                            break;

                        default:
                            break;
                    }
                }


                in.close(); out.close(); socket.close();
            }
            catch (Exception e) {
                System.err.printf("[WORKER] Errore: %s\n", e.getMessage()); 
                e.printStackTrace();
            }
        }
    }
}


