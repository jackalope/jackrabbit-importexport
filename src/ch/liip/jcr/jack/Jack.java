
package ch.liip.jcr.jack;

import java.util.Properties;
import java.io.*;
import javax.jcr.*;
import org.apache.jackrabbit.core.TransientRepository;
//import ch.liip.jcr.davex.DavexClient;

/**
Import or export the entire repository (system view).

Export and import are done on path specified in src/jcr.properties as repository-base-xpath
Import is done with behavior IMPORT_UUID_COLLISION_THROW, that is if there already is something in the repository and that happens to have the same UUID, the import will fail.
Our primary goal is to replace the complete repository contents.
*/
public class Jack {
    public static void main(String[] args) throws Throwable {
        if (args.length != 2) {
            System.out.println("usage: java -jar impexp.jar (import|export) file.xml");
            System.exit(1);
        }
        Jack j = new Jack();

        try {
            if ("import".equals(args[0])) {
                j.doImport(args[1]);
            } else if ("export".equals(args[0])) {
                j.doExport(args[1]);
            } else {
                System.out.println("Unrecognized command "+args[0]);
            }
        } catch(Throwable t) {
            System.err.println("Operation failed:");
            System.err.println(t.getMessage());
        } finally {
            j.close();
        }
    }

    //protected DavexClient client;
    protected Repository repository;
    protected Session session;
    protected Properties config;
    public Jack() throws Throwable {
        InputStream is = getClass().getClassLoader().getResourceAsStream("jcr.properties");
        if (is==null) throw new Exception("Could not find jcr.properties inside the jar");
        config = new Properties();
        config.load(is);
        is.close();

        //network access (fails because of obscure logging library incompatibilites)
        //client = new DavexClient(prop.getProperty("storage"));
        //repository = client.getRepository();

        //create local repository
        repository = new TransientRepository(config.getProperty("jackrabbit-config"),
                                             config.getProperty("jackrabbit-home"));
        SimpleCredentials cred = new SimpleCredentials(config.getProperty("username"),
                                                       config.getProperty("password").toCharArray());
        session = repository.login(cred, config.getProperty("workspace", "default"));
    }
    public void close() {
        session.logout();
    }
    protected void finalize() {
        this.close();
    }

    public void doExport(String filepath) throws Exception {
        File f = new File(filepath);
        if (f.exists()) {
            throw new IllegalArgumentException("Export file "+filepath+" is existing, can not export");
        }
        try {
            FileOutputStream os = new FileOutputStream(f);
            //export all including binary, recursive
            session.exportSystemView(config.getProperty("repository-base-xpath","/"), os, false, false);
            os.close();
        } catch(Throwable t) {
            throw new Exception("Failed to export repository at " +
                config.getProperty("repository-base-xpath","/") + " to file "+filepath+"\n"+t.toString());
        }
        System.out.println("Exported the repository to "+f);
    }

    public void doImport(String filepath) throws Exception {
        File f = new File(filepath);
        if (! f.exists()) {
            throw new IllegalArgumentException("File "+filepath+" not existing, can not import");
        }
        try {
            FileInputStream data = new FileInputStream(f);
            session.importXML(config.getProperty("repository-base-xpath","/"), data,
                              ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        } catch(Throwable t) {
            throw new Exception("Failed to import repository to "+
                config.getProperty("repository-base-xpath","/") +
                " from file "+filepath+"\n"+t.toString());
        }
        try {
            session.save();
        } catch(Throwable t) {
            throw new Exception("Failed to save the imported repository: "+t.toString());
        }
        System.out.println("Imported the repository from "+f);
    }
}