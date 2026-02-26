package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.NoleggioNBTDAO;
import it.zensoftware.luna2.model.NoleggioNBT;

import java.util.Date;
import java.util.List;

public class NoleggioNBTService {

    private final NoleggioNBTDAO nbtDAO;

    public NoleggioNBTService() {
        this.nbtDAO = new NoleggioNBTDAO();
    }

    public List<NoleggioNBT> listAll() {
        return nbtDAO.findAll();
    }

    public NoleggioNBT save(NoleggioNBT nbt) {
        if (nbt.getId() == null) {
            nbt.setDataRichiesta(new Date());
            nbtDAO.save(nbt);
        } else {
            nbtDAO.update(nbt);
        }
        return nbt;
    }
}
