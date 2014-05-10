package grails.plugin.remotessh

/**
 * Created with IntelliJ IDEA.
 * User: Michael Rice
 * Twitter: @errr_
 * Website: http://www.errr-online.com/
 * Github: https://github.com/michaelrice
 * Date: 5/9/2014
 * Time: 8:20 PM
 * Licenses: MIT http://opensource.org/licenses/MIT
 */
public enum TerminalType {

    XTERM('xterm'),
    XTERM_BASIC('xterm-basic'),
    LINUX('linux'),
    VT100('vt100'),
    VT220('vt220')

    String value

    private TerminalType(String value) {
        this.value = value
    }

    /**
     * String representation of a TerminalType.
     * @return
     */
    public String toString(){
        return value
    }
}