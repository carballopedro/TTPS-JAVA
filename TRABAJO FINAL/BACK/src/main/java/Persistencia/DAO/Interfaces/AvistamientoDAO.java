package Persistencia.DAO.Interfaces;

import Modelo.Avistamiento;
import Modelo.Publicacion;
import Persistencia.DAO.Genericos.GenericDAO;

import java.util.List;

public interface AvistamientoDAO extends GenericDAO<Avistamiento> {

    public List<Avistamiento> getByLatitudYLongitud(Double latitud, Double longitud);
    public List<Avistamiento> getByPublicacion(Publicacion publicacion);
}