package Persistencia.DAO.Interfaces;

import Modelo.Avistamiento;
import Modelo.Publicacion;
import Persistencia.DAO.Genericos.GenericDAO;

import java.util.List;

public interface PublicacionDAO extends GenericDAO<Publicacion> {

    public List<Publicacion> getByLatitudYLongitud(Double latitud, Double longitud);
    public List<Publicacion> getActivas();
    public List<Publicacion> getInactivas();
}