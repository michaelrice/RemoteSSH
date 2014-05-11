package grails.plugin.remotessh

import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.InteractiveCallback
import org.apache.log4j.Logger

/**
 * Created with IntelliJ IDEA.
 * User: Michael Rice
 * Twitter: @errr_
 * Website: http://www.errr-online.com/
 * Github: https://github.com/michaelrice
 * Date: 5/9/2014
 * Time: 6:18 PM
 * Licenses: MIT http://opensource.org/licenses/MIT
 */
abstract class RemoteConnection {

    /**
     * Host name of the system to connect to.
     */
    public String host

    /**
     * User to connect with.
     */
    public String user

    /**
     * Password of the user to connect with.
     */
    public String userpass

    /**
     * Remote port sshd server is listening for connections on.
     */
    public int port

    /**
     * Command the user should run on the remote server.
     */
    public String usercommand

    /**
     * Shell on the remote server to use.
     */
    public String shell

    /**
     *
     */
    public String file

    /**
     * Remote directory on the host.
     */
    public String remotedir

    /**
     * Output from the command.
     */
    public StringBuilder output

    /**
     * Local directory to place file from remote server.
     */
    public String localdir

    /**
     * Terminal type to request from the server.
     */
    public TerminalType terminalType = TerminalType.VT220

    /**
     * If you want to run the command using sudo set
     * this string to "sudo"
     */
    public String sudo

    /**
     * SshConfig holding the users options.
     */
    public SshConfig sshConfigObject

    /**
     * Authentication type to use when connecting.
     */
    public AuthType authType

    /**
     * ssh key file used to connect to the remote host.
     */
    public String sshUserKeyFile

    /**
     * ssh key file password
     */
    public String sshUserKeyFilePassword

    /**
     * Filter for output.
     */
    public String filter

    /**
     * Logger
     */
    Logger log = Logger.getLogger(RemoteConnection)

    /**
     *
     * @param config
     * @return
     */
    public String Result(SshConfig config) {
        sshConfigObject = config
        return runCommandWithOutput(getConnection())
    }

    /**
     *
     * @param closure
     * @return
     * @throws IllegalArgumentException
     */
    public String Result(Closure closure) throws IllegalArgumentException {
        run closure
        return runCommandWithOutput(getConnection())
    }

    abstract String runCommandWithOutput(Connection connection)

    /**
     * Return a connection object to the caller.
     *
     * @return
     */
    private Connection getConnection() {

        String sshuser = sshConfigObject?.getConfig("USER")?.toString()
        String sshpass = sshConfigObject?.getConfig("PASS")?.toString()
        String sshkey = sshConfigObject?.getConfig("KEY")?.toString()
        String sshkeypass = sshConfigObject?.getConfig("KEYPASS")?.toString()
        String sshauthtype = sshConfigObject?.getConfig("AUTH_TYPE")?.toString()

        int sshport
        // This will throw a NFE if PORT is empty in the config
        // because you can not cast an empty string to an int.
        try {
            sshport = sshConfigObject?.getConfig("PORT").toString() as int
            log.trace("SSH port set in config: ${sshport}")
        }
        catch (NumberFormatException ignore) {
            sshport = null
            log.trace("SSH port not set in config.")
        }
        // if port was set use its value, else use the config, else default to 22
        port = port ?: sshport ?: 22
        String username = user ?: sshuser
        String password = userpass ?: sshpass
        // if authType set use it, else use config, else use kb-i
        // useful where use PasswordAuthentication no (which is default on many Linux systems)
        String authMethod = authType?.toString() ?: sshauthtype  ?: AuthType.KEYBOARD_INTERACTIVE.toString()
        authType = authType ?: AuthType.valueOf(authMethod)
        File keyfile
        sshkey = sshUserKeyFile ?: sshkey
        if (!sshkey.empty) {
            keyfile = new File(sshkey)
            log.trace("Loaded sshkey from file: ${sshkey}")
        }
        String keyfilePass = sshUserKeyFilePassword ?: sshkeypass

        // Now we are ready to try to create the connection to the server.
        Connection conn
        try {
            conn = new Connection(host, port)
            log.trace("Opening connection to ${host}.")
            // Now connect
            conn.connect()
            // Authenticate
            boolean isAuthenticated = false
            if (authType) {
                log.trace("User requested authType. Attempting to auth using ${authMethod}")
                if (authType.equals(AuthType.PUBLIC_KEY)) {
                    log.trace("Attempting a public key auth.")
                    conn = publicKeyAuth(keyfile, keyfilePass, conn, username)
                    isAuthenticated = conn.isAuthenticationComplete()
                }
                else if (authType.equals(AuthType.PASSWORD)) {
                    log.trace("Attempting a password authentication.")
                    isAuthenticated = passwordAuth(conn, username, password)
                }
                else if (authType.equals(AuthType.KEYBOARD_INTERACTIVE)) {
                    isAuthenticated = keyboardInteractiveAuth(conn, username, password)
                }
                else {
                    log.debug("Unsupported AuthType requested.")
                }
            }

            // If our requested auth style attempt worked return the connection.
            if (isAuthenticated) {
                log.trace("Successfully created auth session using ${authMethod}.")
                return conn
            }

            // if there is no password set, but there is a keyfile set try pub key auth
            if (!password && keyfile) {
                isAuthenticated = conn.authenticateWithPublicKey(username, keyfile, keyfilePass)
            }
            else {
                // Need to check if password auth is supported.
                List thing = conn.getRemainingAuthMethods(username)

                isAuthenticated = conn.authenticateWithPassword(username, password)
            }
            if (!isAuthenticated) {
                log.trace("Authentication failed.")
                throw new IOException("Authentication failed.")
            }
        }
        catch (IOException ioe) {

        }
    }

    private boolean keyboardInteractiveAuth(Connection connection, String username, String password) {
        if (connection.authenticateWithKeyboardInteractive(username, new InteractiveLogic("", password))) {
            return true
        }
        return false
    }

    private boolean passwordAuth(Connection connection, String username, String password) {
        return false
    }

    /**
     * Runs a passed closure to implement builder-style operation.
     *
     * @param closure
     */
    private void run(Closure closure) {
        closure.delegate = this
        closure.resolveStrategy = Closure.OWNER_FIRST
        closure.call()
    }

    /**
     * Attempt to auth the connection using the public key method.
     * Returns the connection back to the caller.
     *
     * @param keyfile
     * @param keyfilePass
     * @param conn
     * @param username
     * @return
     */
    private Connection publicKeyAuth(File keyfile, String keyfilePass, Connection conn, String username) {
        log.info("Public Key Auth in progress.")
        try {
            if (keyfile.canRead()) {
                if (conn.isAuthMethodAvailable(username, AuthType.PUBLIC_KEY.toString())) {
                    log.trace("Attempting to auth using keyfile.")
                    if (conn.authenticateWithPublicKey(username, keyfile, keyfilePass)) {
                        return conn
                    }
                }
                log.debug("Server did not report supporting pub key auth.")
            }
        }
        catch (Exception e) {
            log.error("Failed to auth using pub key auth. Stacktrace from underlying problem to follow.", e)
        }
        return conn
    }

    /**
     * The logic that one has to implement if "keyboard-interactive" autentication shall be
     * supported.
     *
     */
    class InteractiveLogic implements InteractiveCallback {
        int promptCount = 0
        String lastError
        private String password
        private Logger log = Logger.getLogger(InteractiveLogic)

        public InteractiveLogic(String lastError, String passwd) {
            log.trace("Created new InteractiveLogic with error: ${lastError}")
            this.lastError = lastError
            this.password = passwd
        }

        /**
         * The callback may be invoked several times
         * depending on how many questions-sets the
         * server sends.
         *
         */
        public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws IOException {
            log.trace("attempting to reply to challenge.")
            log.trace("Name: ${name}")
            log.trace("Instruction: ${instruction}")
            log.trace("Number of Promots: ${numPrompts}")
            log.trace("Prompt: ${prompt}")
            log.trace("Echo: ${echo}")
            List result = []
            for (int i = 0; i < numPrompts; i++) {
                // Often, servers just send empty strings for "name" and "instruction"
                List content = [lastError, name, instruction, prompt[i]]
                log.trace("Prompt: ${prompt[i]}")
                if (prompt[i].equalsIgnoreCase("password:")) {
                    log.trace("The server is asking us for a password.")
                    result[i] = password
                }
                if (lastError != null) {
                    // show lastError only once
                    lastError = null
                }
                log.trace("Content: ${content} Echo:  ${!echo[i]}")
                //result[i] = esd.answer;
                promptCount++
            }
            return result
        }

        /**
         * We maintain a prompt counter - this enables the detection of situations where the ssh
         * server is signaling "authentication failed" even though it did not send a single prompt.
         */
        public int getPromptCount() {
            return promptCount
        }
    }
}