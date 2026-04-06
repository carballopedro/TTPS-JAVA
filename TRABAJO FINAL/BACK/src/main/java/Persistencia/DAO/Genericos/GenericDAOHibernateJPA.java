package Persistencia.DAO.Genericos;

import Persistencia.ClasesUtilitarias.EMF;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

// DAO genérico que contiene las operaciones básicas de acceso a datos.
// Se usa como base para los DAOs concretos y trabaja con JPA/Hibernate,
// dejando a Spring el manejo de transacciones y del EntityManager.

@Transactional(readOnly = false) // falso x defecto, salvo que se indique lo contrario
public class GenericDAOHibernateJPA<T> implements GenericDAO<T> {

    @PersistenceContext
    private EntityManager entityManager;

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected Class<T> persistentClass;

    public GenericDAOHibernateJPA(Class<T> clase){
        this.persistentClass = clase;
    }

    public Class<T> getPersistentClass() {
        return persistentClass;
    }

    @Override
    public void delete(T entity) {
        EntityManager em = this.getEntityManager();
        em.remove(em.merge(entity));
    }

    @Override
    public void delete(Long id) {
        EntityManager em = this.getEntityManager();
        T managed = em.find(getPersistentClass(), id);
        if (managed != null) {
            em.remove(managed);
        }
    }

    @Transactional(readOnly = true) // solo lectura
    @Override
    public T get(Long id) {
        return this.getEntityManager().find(getPersistentClass(), id);
    }

    // no está bueno concatenar strings (para está consulta no hay riesgo de inyección SQL)
    @Transactional(readOnly = true) // solo lectura
    @Override
    public List<T> getAll(String columnOrder) {
        Query consulta = this.getEntityManager()
                .createQuery("SELECT e FROM " +
                        getPersistentClass().getSimpleName() +
                        " e order by e." + columnOrder);
        List<T> resultado = (List<T>) consulta.getResultList();
        return resultado;
    }

    @Override
    public T persist(T entity) {
        this.getEntityManager().persist(entity);
        return entity;
    }

    @Override
    public T update(T entity) {
        return this.getEntityManager().merge(entity);
    }
}
