import com.squarepeace.nnppss.Frame;
import com.squarepeace.nnppss.Utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test; // Import the necessary package for the @Test annotation
import static org.junit.Assert.assertTrue; // Import the necessary package for the assertTrue method


public class UtilitiesTests {


    //test for createConfigFile method in Utilities class
    @Test
    public void testCreateConfigFile() {
        Utilities utilities = new Utilities();
        utilities.createConfigFile();
        File file = new File("config.properties");
        assertTrue(file.exists());
    }

    //test for getPSVitaUrl and getPSPUrl methods in Utilities class
    @Test
    public void testGetPSVitaUrlAndPSPUrl() {
        Utilities utilities = new Utilities();
        utilities.createConfigFile();
        String pspUrl = utilities.getPSPUrl();
        String psvitaUrl = utilities.getPSVitaUrl();
        String psxUrl = utilities.getPsxUrl();
        assertTrue(!pspUrl.isEmpty());
        assertTrue(!psvitaUrl.isEmpty());
        assertTrue(!psxUrl.isEmpty());
    }

    //test for getProperty method in Utilities class
    @Test
    public void testGetProperty() {
        Utilities utilities = new Utilities();
        utilities.createConfigFile();
        String pspUrl = utilities.getProperty("psp.url");
        String psvitaUrl = utilities.getProperty("psvita.url");
        String psxUrl = utilities.getProperty("psx.url");
        assertTrue(pspUrl.endsWith(".tsv"));
        assertTrue(psvitaUrl.endsWith(".tsv"));
        assertTrue(psxUrl.endsWith(".tsv"));
    }    

    //test for readTSV method in Utilities class
    @Test
    public void testReadTSV() throws FileNotFoundException, IOException {
        Utilities utilities = new Utilities();
        assertTrue(utilities.readTSV(utilities.TSV_PSP).getRowCount() > 0);
        assertTrue(utilities.readTSV(utilities.TSV_VITA).getRowCount() > 0);
        assertTrue(utilities.readTSV(utilities.TSV_PSX).getRowCount() > 0);
    }

    //test exist db files (db/PSP_GAMES and db/PSV_GAMES) on directory
    @Test
    public void testExistDBFiles() {
        Utilities utilities = new Utilities();
        File pspFile = new File(utilities.TSV_PSP);
        File psvitaFile = new File(utilities.TSV_VITA);
        File psxFile = new File(utilities.TSV_PSX);
        assertTrue(pspFile.exists());
        assertTrue(psvitaFile.exists());
        assertTrue(psxFile.exists());
    }
    
    //test for moveFile method in Utilities class
    @Test
    public void testMoveFile() throws IOException {
        Utilities utilities = new Utilities();
        File file = new File("db/PSP_GAMES.tsv");
        utilities.moveFile(file.getPath(), "db/PSP_GAMES_COPY.tsv");
        File fileCopy = new File("db/PSP_GAMES_COPY.tsv");
        assertTrue(fileCopy.exists());
        //back to original name
        utilities.moveFile(fileCopy.getPath(), "db/PSP_GAMES.tsv");
    }

    //test for convertFileSize method in Utilities class
    @Test
    public void testConvertFileSize() {
        Utilities utilities = new Utilities();
        String fileSize = utilities.convertFileSize(1024);
        assertTrue(fileSize.equals("0.0 MiB"));
    }

    //test for isCommandInstalled method in Utilities class
    @Test
    public void testIsCommandInstalled() {
        Utilities utilities = new Utilities();
        assertTrue(utilities.isCommandInstalled("java"));
    }
}
