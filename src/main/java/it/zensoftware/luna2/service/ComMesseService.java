package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.ComMessaDAO;
import it.zensoftware.luna2.model.Commessa;
import it.zensoftware.luna2.model.Preventivo;
import it.zensoftware.luna2.model.Fattura;
import java.util.Calendar;

/**
 * Servizio per la gestione delle Commesse
 */
public class ComMesseService {

    private ComMessaDAO comMessaDAO;

    public ComMesseService(ComMessaDAO comMessaDAO) {
        this.comMessaDAO = comMessaDAO;
    }

    /**
     * Crea una nuova commessa dal preventivo
     */
    public Commessa creaCommessaDaPreventivo(Preventivo preventivo) {
        if (preventivo == null) {
            throw new IllegalArgumentException("Preventivo non valido");
        }

        Integer anno = Calendar.getInstance().get(Calendar.YEAR);
        String numero = comMessaDAO.getNextNumero(anno);

        Commessa commessa = new Commessa();
        commessa.setNumero(numero);
        commessa.setAnno(anno);
        commessa.setPreventivo(preventivo);
        commessa.setDescrizione("Commessa da " + preventivo.getNumero());
        commessa.setImponibile(preventivo.getImponibile());
        commessa.setIva(preventivo.getIva());
        commessa.setTotale(preventivo.getTotale());
        commessa.setStato(Commessa.StatoCommessa.APERTA);
        commessa.setPercentualeCompletamento(0);

        comMessaDAO.save(commessa);
        return commessa;
    }

    /**
     * Completa una commessa e prepara per la chiusura
     */
    public void completaCommessa(Commessa commessa) {
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non valida");
        }

        commessa.setStato(Commessa.StatoCommessa.COMPLETATA);
        commessa.setPercentualeCompletamento(100);
        commessa.setDataFineEffettiva(new java.util.Date());
        comMessaDAO.update(commessa);
    }

    /**
     * Chiude una commessa associandola alla fattura
     */
    public void chiudiCommessa(Commessa commessa, Fattura fattura) {
        if (commessa == null || fattura == null) {
            throw new IllegalArgumentException("Commessa o Fattura non valida");
        }

        commessa.setFattura(fattura);
        commessa.setStato(Commessa.StatoCommessa.CHIUSA);
        commessa.setDataChiusura(new java.util.Date());
        comMessaDAO.update(commessa);
    }

    /**
     * Annulla una commessa
     */
    public void annullaCommessa(Commessa commessa) {
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non valida");
        }

        commessa.setStato(Commessa.StatoCommessa.ANNULLATA);
        comMessaDAO.update(commessa);
    }

    /**
     * Sospende una commessa
     */
    public void sospendiCommessa(Commessa commessa) {
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non valida");
        }

        commessa.setStato(Commessa.StatoCommessa.SOSPESA);
        comMessaDAO.update(commessa);
    }

    /**
     * Riprende una commessa sospesa
     */
    public void riprediCommessa(Commessa commessa) {
        if (commessa == null) {
            throw new IllegalArgumentException("Commessa non valida");
        }

        if (!Commessa.StatoCommessa.SOSPESA.equals(commessa.getStato())) {
            throw new IllegalStateException("Commessa non sospesa");
        }

        commessa.setStato(Commessa.StatoCommessa.IN_LAVORAZIONE);
        comMessaDAO.update(commessa);
    }

    /**
     * Aggiorna il percentuale di completamento
     */
    public void aggiornaPercentualeCompletamento(Commessa commessa, Integer percentuale) {
        if (commessa == null || percentuale < 0 || percentuale > 100) {
            throw new IllegalArgumentException("Dati non validi");
        }

        commessa.setPercentualeCompletamento(percentuale);
        if (percentuale < 100 && !Commessa.StatoCommessa.IN_LAVORAZIONE.equals(commessa.getStato())) {
            commessa.setStato(Commessa.StatoCommessa.IN_LAVORAZIONE);
        }
        comMessaDAO.update(commessa);
    }
}
