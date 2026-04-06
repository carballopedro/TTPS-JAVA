package Persistencia.ClasesUtilitarias;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;

// Clase utilitaria que se usaba para crear y mantener una única instancia
// de EntityManagerFactory cuando la persistencia se manejaba manualmente con JPA.

// ACTUALMENTE NO SE USA.
// Fue reemplazada por la configuración de persistencia de Spring,
// que gestiona el EntityManagerFactory y las transacciones automáticamente.

public class EMF
{
    private static EntityManagerFactory em = null;
    static {
        try {
            em = Persistence.createEntityManagerFactory("unlp");
        } catch (PersistenceException e) {
            System.err.println("Error al crear EntityManagerFactory: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public static EntityManagerFactory getEMF(){
        return em;
    }

    private EMF(){}
}
