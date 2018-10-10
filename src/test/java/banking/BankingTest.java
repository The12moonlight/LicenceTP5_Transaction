package banking;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.hsqldb.cmdline.SqlFile;
import org.hsqldb.cmdline.SqlToolError;


public class BankingTest {
	private static DataSource myDataSource; // La source de données à utiliser
	private static Connection myConnection ;	
	private BankingDAO myDAO;
	
	@Before
	public void setUp() throws SQLException, IOException, SqlToolError {
		// On crée la connection vers la base de test "in memory"
		myDataSource = getDataSource();
		myConnection = myDataSource.getConnection();
		// On initialise la base avec le contenu d'un fichier de test
		String sqlFilePath = BankingTest.class.getResource("testdata.sql").getFile();
		SqlFile sqlFile = new SqlFile(new File(sqlFilePath));
		sqlFile.setConnection(myConnection);
		sqlFile.execute();
		sqlFile.closeReader();	
		// On crée l'objet à tester
		myDAO = new BankingDAO(myDataSource);
	}
	
	@After
	public void tearDown() throws SQLException {
		myConnection.close();		
		myDAO = null; // Pas vraiment utile
	}

	
	@Test
	public void findExistingCustomer() throws SQLException {
		float balance = myDAO.balanceForCustomer(0);
		// attention à la comparaison des nombres à virgule flottante !
		assertEquals("Balance incorrecte !", 100.0f, balance, 0.001f);
	}

	@Test
	public void successfulTransfer() throws Exception {
		float amount = 10.0f;
		int fromCustomer = 0;
		int toCustomer = 1;
		float before0 = myDAO.balanceForCustomer(fromCustomer);
		float before1 = myDAO.balanceForCustomer(toCustomer);
		myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
		// Les balances doivent avoir été mises à jour dans les 2 comptes
		assertEquals("Balance incorrecte !", before0 - amount, myDAO.balanceForCustomer(fromCustomer), 0.001f);
		assertEquals("Balance incorrecte !", before1 + amount, myDAO.balanceForCustomer(toCustomer), 0.001f);				
	}
	
        @Test
        public void debAccountNegat() throws Exception {
            float amount = 150.0f;
            int fromCustomer = 0;
            int toCustomer = 1;
            float before0 = myDAO.balanceForCustomer(fromCustomer);
	    float before1 = myDAO.balanceForCustomer(toCustomer);
            // Le montant est supérieur a la balance du compte 1
            try {
                myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
                // Cette requete ne marche pas donc on va dans le catch
            } catch (Exception e) {
                // On passe par ici, car il y a violation d'integrité
		System.err.println("Erreur balance negatif => constrainte d'intergrite");
                assertEquals("La balance n'a pas changé", before0, myDAO.balanceForCustomer(fromCustomer), 0.001f);
		assertEquals("La balance n'a pas changé !", before1, myDAO.balanceForCustomer(toCustomer), 0.001f);	
            }
        }
            
        @Test
        public void debAccountNotExist() throws Exception {
            float amount = 10.0f;
            int fromCustomer = 2; // Compte inexistant
            int toCustomer = 1; 
            float before = myDAO.balanceForCustomer(1);
            try {
               myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
            } catch (Exception e) {
                // On doit passe par ici, car il y a violation d'integrité
                System.err.println("Erreur compte n'existe pas => constrainte d'intergrite");
                assertEquals("La balance n'a pas changé", before, myDAO.balanceForCustomer(fromCustomer), 0.001f);
            }
        }
        
        @Test
        public void credAccountNotExist() throws Exception {
            float amount = 10.0f;
            int fromCustomer = 0; 
            int toCustomer = 2; // Compte inexistant
            float before = myDAO.balanceForCustomer(0);
            try {
               myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
            } catch (Exception e) {
                // On doit passe par ici, car il y a violation d'integrité
                System.err.println("Erreur compte n'existe pas => constrainte d'intergrite");
                assertEquals("La balance n'a pas changé", before, myDAO.balanceForCustomer(fromCustomer), 0.001f);
            }
        }
        
        @Test
        public void accountNotExist() throws Exception {
            float amount = 10.0f;
            int fromCustomer = 2; // Compte inexistant
            int toCustomer = 3; 
            try {
               myDAO.bankTransferTransaction(fromCustomer, toCustomer, amount);
            } catch (Exception e) {
                // On doit passe par ici, car il y a violation d'integrité
                System.err.println("Erreur compte n'existe pas => constrainte d'intergrite");
            }
        }

	public static DataSource getDataSource() throws SQLException {
		org.hsqldb.jdbc.JDBCDataSource ds = new org.hsqldb.jdbc.JDBCDataSource();
		ds.setDatabase("jdbc:hsqldb:mem:testcase;shutdown=true");
		ds.setUser("sa");
		ds.setPassword("sa");
		return ds;
	}	
}
