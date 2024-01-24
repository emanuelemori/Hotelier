import java.util.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.*;
public class Citta {

    //VEDI SE FARE LE LISTE THREAD SAFE O NO
    private String nomecitta;
    public LinkedList <Hotel> listaHotel;

    public static ArrayList <Citta> listacitta = new ArrayList<>(20);


    public Citta (String nomecitta) {
        this.nomecitta = nomecitta;
        this.listaHotel = new LinkedList <> ();
    }

    
    /**
     * crea un oggetto di tipo città e lo inserisce in listacitta se una città con quel nome non è già presente nella lista
     * @param nomecitta
     */
    public static void creacittadajson(String nomecitta) {
        Citta citta = new Citta(nomecitta);
        for(Citta c : Citta.listacitta) {
            if(c.equals(citta))
                return;
        }
        listacitta.add(citta);
    }
    
    /**
     * scorre la lista delle Città e restituisce un oggetto di tipo Città corrispondente, se esiste
     * @param nomecitta
     * @return un oggetto di tipo Città corrispondente a nomecitta, null se tale Città non è presente nella lista
     */
    public static Citta buildcitta(String nomecitta) {
        for(Citta c: Citta.listacitta) {
            //System.out.println("c.nome: "+ c.nomecitta+ ", nomecitta: "+ nomecitta+"\n");
            if(c.getnomecitta().equals(nomecitta))
                return c;
            
            if(c.equals(new Citta(nomecitta)))
                return c;
            
        }
        return null;
    }


    static class LocalComparator implements Comparator<Hotel> {
        
        @Override
        /**
         * A parità di punteggio viene confrontata la quantità di recensioni che i due hotel hanno, e a pari quantità viene confrontata l’attualità delle recensioni.
         * @param h1
         * @param h2
         * return un intero positivo se h1 è in posizione superiore a h2 nel ranking, 0 se ai due spetta la stessa posizione, un intero negativo altrimenti 
         */
        public int compare (Hotel h1, Hotel h2) {
         
            //confronto la qualità
            double mediarankingh1 = h1.getpunti();
            double mediarankingh2 = h2.getpunti();

            if(mediarankingh1==mediarankingh2) {
                //confronto la quantità
                if(h1.GlobalScoreArray.size()==h2.GlobalScoreArray.size()) {
                    //deve dare -1 se h1 < h2, cioè se h1 è più vecchio di h2
                    //cioè se la media delle date di h1 è minore delle date di h2
                    return (int) (h1.calcola_media_date() - h2.calcola_media_date());
                }
                else 
                    return (int) (h2.getpunti() - h1.getpunti());
            }
            else 
                return (int) (mediarankingh2-mediarankingh1);
        }
    }

    public boolean equals (Citta c) {
        if(c==null)
            return false;
        return this.nomecitta.equals(c.getnomecitta());
    }

    public boolean equals (Object c) {
        if(c instanceof Citta) 
            return equals((Citta) c);
        else return false;
    }

    /**
     * ordina il ranking degli hotel di una città in base alle loro recensioni e valuta se l'hotel in testa è cambiato
     * @return true se l'hotel in testa è cambiato, false altrimenti
     */
    public synchronized boolean sorthotelincitta() {
        Hotel head = listaHotel.getFirst();
        Collections.sort(listaHotel, new LocalComparator());
        Hotel newhead = listaHotel.getFirst();
        if(!head.equals(newhead)) 
            return true;
        return false;
    }

    public synchronized int getindice(Hotel hotel) {
        int i = 1;
        for(Hotel h: listaHotel) {
            if(h.equals(hotel))
                return i;
            i++;
        }
        return -1;
    }

    /**
     * 
     * @return rappresentazione in formato stringa della lista degli hotel di una città
     */
    public String hotelincittatoString () {
        StringBuilder res= new StringBuilder();
        for( Hotel hotel : listaHotel) {
            res.append(hotel.toString());
            res.append("\n");
        }
        
        if(res.isEmpty())
            return "NON CI SONO HOTEL IN "+this.getnomecitta();
        return res.toString();
    }

    public String getnomecitta() {
        return this.nomecitta;
    }
}
