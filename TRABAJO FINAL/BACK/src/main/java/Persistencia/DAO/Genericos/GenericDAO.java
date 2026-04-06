package Persistencia.DAO.Genericos;

import java.util.List;

// Interfaz genérica de acceso a datos.
// Define las operaciones CRUD básicas que deben implementar todos los DAOs
// para interactuar con la base de datos.

public interface GenericDAO<T> {
    public void delete(T entity);
    public void delete(Long id);
    public T get(Long id);
    public List<T> getAll(String columnOrder);
    public T persist(T entity);
    public T update(T entity);
}
