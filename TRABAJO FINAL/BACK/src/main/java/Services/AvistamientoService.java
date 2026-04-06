package Services;

import Modelo.Avistamiento;
import Modelo.Publicacion;
import Persistencia.DAO.Interfaces.AvistamientoDAO;
import Persistencia.DAO.Interfaces.PublicacionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AvistamientoService {

    private final AvistamientoDAO avistamientoDAO;
    private final PublicacionDAO publicacionDAO;

    @Autowired
    public AvistamientoService(AvistamientoDAO avistamientoDAO,
                               PublicacionDAO publicacionDAO) {
        this.avistamientoDAO = avistamientoDAO;
        this.publicacionDAO = publicacionDAO;
    }

    public List<Avistamiento> listarTodos() {
        return avistamientoDAO.getAll("id");
    }

    public Avistamiento crear(Avistamiento av) {

        // Validación básica
        if (av == null ||
                av.getPublicacion() == null ||
                av.getPublicacion().getId() == null) {
            return null; // el controller se encarga de devolver 400
        }

        // Verifico si existe la publicación
        Publicacion pub = publicacionDAO.get(av.getPublicacion().getId());
        if (pub == null) {
            return null; // el controller se encarga de devolver 404
        }

        // reemplazo la publicación por la versión real de BD
        av.setPublicacion(pub);

        avistamientoDAO.persist(av);

        return av;
    }
}