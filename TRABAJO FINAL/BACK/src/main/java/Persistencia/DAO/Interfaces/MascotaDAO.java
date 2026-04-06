package Persistencia.DAO.Interfaces;

import Modelo.Enums.EstadoMascota;
import Modelo.Enums.RazaMascota;
import Modelo.Enums.TamanoMascota;
import Modelo.Enums.TipoMascota;
import Modelo.Mascota;
import Persistencia.DAO.Genericos.GenericDAO;

import java.util.List;

public interface MascotaDAO extends GenericDAO<Mascota> {

    public List<Mascota> getByEstado(EstadoMascota estado);
    public List<Mascota> getByTipo (TipoMascota tipo);
    public List<Mascota> getByRaza (RazaMascota raza);
    public List<Mascota> getByTamano (TamanoMascota tamano);
}