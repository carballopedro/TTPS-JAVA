package Persistencia.DAO.Interfaces;

import Modelo.Usuario;
import Persistencia.DAO.Genericos.GenericDAO;

import java.util.List;

public interface UsuarioDAO extends GenericDAO<Usuario> {

    public Usuario getByEmail(String email);
    public List<Usuario> getHabilitados();
    public List<Usuario> getDeshabilitados();
    public List<Usuario> getTopColaboradores(int limite);

}