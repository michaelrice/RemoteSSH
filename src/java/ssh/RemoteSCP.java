package ssh;

import grails.plugin.remotessh.SshConfig;

import java.io.File;
import java.io.IOException;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import org.apache.log4j.Logger;

/**
 * Copies a file to remote server.
 */
public class RemoteSCP {
    /**
     * Remote host to connect to
     */
    String host = "";
    /**
     * Remote user to connect as
     */
    String user = "";
    /**
     * Remote port to connect to
     */
    Integer port = 0;
    /**
     * Remote users password
     */
    String userpass = "";
    /**
     * File to copy to remote server
     */
    String file = "";
    /**
     * Directory on remote server to place file
     */
    String remotedir = "";
    /**
     * Command to be run by user
     */
    String usercommand = "";
    /**
     * Output from the command
     */
    String output = "";
    /**
     * Logger
     */
    private Logger log = Logger.getLogger(getClass().getName());
    /**
     * Constructor
     *
     * @param host
     * @param file
     * @param remotedir
     */
    public RemoteSCP(String host, String file, String remotedir) {
        this.host = host;
        this.file = file;
        this.remotedir = remotedir;
    }

    /**
     * Constructor
     *
     * @param host
     * @param user
     * @param file
     * @param remotedir
     */
    public RemoteSCP(String host, String user, String file, String remotedir) {
        this.host = host;
        this.user = user;
        this.file = file;
        this.remotedir = remotedir;
    }

    /**
     * Constructor
     *
     * @param host
     * @param user
     * @param userpass
     * @param file
     * @param remotedir
     */
    public RemoteSCP(String host, String user, String userpass, String file, String remotedir) {
        this.host = host;
        this.user = user;
        this.userpass = userpass;
        this.file = file;
        this.remotedir = remotedir;
    }

    /**
     * Constructor
     *
     * @param host
     * @param file
     * @param remotedir
     * @param port
     */
    public RemoteSCP(String host, String file, String remotedir, Integer port) {
        this.host = host;
        this.file = file;
        this.remotedir = remotedir;
        this.port = port;
    }

    /**
     * Constructor
     *
     * @param host
     * @param user
     * @param file
     * @param remotedir
     * @param port
     */
    public RemoteSCP(String host, String user, String file, String remotedir, Integer port) {
        this.host = host;
        this.user = user;
        this.file = file;
        this.remotedir = remotedir;
        this.port = port;
    }

    /**
     * Constructor
     *
     * @param host
     * @param user
     * @param userpass
     * @param file
     * @param remotedir
     * @param port
     */
    public RemoteSCP(String host, String user, String userpass, String file, String remotedir, Integer port) {
        this.host = host;
        this.user = user;
        this.userpass = userpass;
        this.file = file;
        this.remotedir = remotedir;
        this.port = port;
    }

    /**
     * TODO
     *
     * @param ac
     * @return
     * @throws IOException
     */
    public String Result(SshConfig ac) throws IOException {
        log.trace("Pulling connection info from configuration file.");
        Object sshuser = ac.getConfig("USER");
        Object sshpass = ac.getConfig("PASS");
        Object sshkey = ac.getConfig("KEY");
        Object sshkeypass = ac.getConfig("KEYPASS");
        Object sshport = ac.getConfig("PORT");
        if (user.isEmpty()) {
            user = sshuser.toString();
            log.trace("Setting user to default from config file: " + user);
        }
        if (userpass.isEmpty()) {
            userpass = sshpass.toString();
            log.trace("Setting password to default from config file.");
        }
        if (port == 0) {
            String sps = sshport.toString();
            if (sps.matches("[0-9]+")) {
                port = Integer.parseInt(sps);
            }
        }
        String hostname = host;
        String username = user;
        File keyfile = new File(sshkey.toString());
        String keyfilePass = sshkeypass.toString();
        if (port == 0) {
            port = 22;
        }
        Connection conn = new Connection(hostname, port);
        // Now connect
        conn.connect();
        // Authenticate
        boolean isAuthenticated = false;
        if (userpass.isEmpty()) {
            isAuthenticated = conn.authenticateWithPublicKey(username, keyfile, keyfilePass);
        }
        else {
            isAuthenticated = conn.authenticateWithPassword(username, userpass);
        }

        if (!isAuthenticated) {
            throw new IOException("Authentication failed.");
        }

        /* Create a session */
        SCPClient scp = conn.createSCPClient();
        scp.put(file, remotedir);
        conn.close();

        output = "File " + file + " should now be copied to " + host + ":"
                + remotedir + "<br>";

        return output;
    }
}
