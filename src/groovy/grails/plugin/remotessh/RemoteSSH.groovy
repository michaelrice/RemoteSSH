package grails.plugin.remotessh

import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.Session
import ch.ethz.ssh2.StreamGobbler

class RemoteSSH extends RemoteConnection {


    @Override
    String runCommandWithOutput(Connection conn) {
        log.trace("Running command with output.")

        if (!output) {
            output = new StringBuilder()
        }

        try {
            /* Create a session */
            Session sess = conn.openSession()
            log.trace("Opened session.")
            sess.requestPTY("${terminalType.toString()}")
            log.trace("Set terminal type to ${terminalType.toString()}")
            // I think this will only work if there is no password sudo
            // on the box. TODO: test this theory.
            if (sudo == "sudo") {
                sess.execCommand("sudo ${shell}")
                // sess.execCommand("sudo bash")
                log.trace("Opend ${shell} using sudo.")
            }
            else {
                sess.execCommand("${shell}")
                log.trace("Opened ${shell}")
            }
            sleep(10)
            sess.getStdin().write((usercommand + "\n").getBytes())
            log.trace("Executed ${usercommand}")
            sess.getStdin().write("exit\n".getBytes())
            sess.getStdin().write("exit\n".getBytes())
            sleep(10)
            InputStream stdout = new StreamGobbler(sess.getStdout())
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout))
            // output.append("Remote execution of $usercommand returned:<br>")
            if (!output) {
                output = new StringBuilder()
                log.trace("Created new StringBuilder to capture output.")
            }
            // I only want to capture the actual output of the command
            // so we want to ignore the firs line where we run the command
            // then the next to where we type exit\n above.
            // We want to remove 0, 1, 2, -1 from this list. Then add everything
            // else to the stringbuilder.
            List lines = br.readLines()[3..-2]
            lines.each {
                output.append(it + "\n")
            }
            /* Close this session */
            sess.close()
            /* Close the connection */
            conn.close()

        }
        catch (IOException e) {
            output.append(e)
        }

        return output.toString()
    }
}