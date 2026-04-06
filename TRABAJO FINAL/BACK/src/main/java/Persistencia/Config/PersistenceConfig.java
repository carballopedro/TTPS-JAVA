package Persistencia.Config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

// Configuración de persistencia con JPA/Hibernate.
// Define la conexión a la base de datos, el EntityManagerFactory
// y el manejo de transacciones para las entidades del sistema.

@Configuration   // Marks the class as a configuration class for Spring.
@EnableTransactionManagement   //Enables declarative transaction management via Spring's @Transactional annotation.
@ComponentScan(basePackages = {"Persistencia", "Modelo"})
public class PersistenceConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setUsername("root");
        driverManagerDataSource.setPassword("password");
        driverManagerDataSource.setUrl("jdbc:mysql://localhost:3306/trabajofinal");
        driverManagerDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return driverManagerDataSource;
    }


    /**
     * El localContainerEntityManagerFactoryBean es un componente de Spring que facilita la configuración de la
     * fábrica de administradores de entidades de JPA. Este bean permite la integración de JPA con el contenedor
     * de Spring, proporcionando una forma sencilla de gestionar las entidades y las transacciones.
     * @return
     */

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource());
        emf.setPackagesToScan("Persistencia", "Modelo");
        emf.setEntityManagerFactoryInterface(EntityManagerFactory.class);
        JpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(jpaVendorAdapter);
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        emf.setJpaProperties(jpaProperties);
        return emf;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(emf);
        return transactionManager;
    }

    }
