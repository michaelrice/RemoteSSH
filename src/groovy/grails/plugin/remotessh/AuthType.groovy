package grails.plugin.remotessh

/**
 * Created with IntelliJ IDEA.
 * User: Michael Rice
 * Twitter: @errr_
 * Website: http://www.errr-online.com/
 * Github: https://github.com/michaelrice
 * Date: 5/10/2014
 * Time: 12:14 AM
 * Licenses: MIT http://opensource.org/licenses/MIT
 */
public enum AuthType {
    PASSWORD('password'),
    PUBLIC_KEY('publickey'),
    KEYBOARD_INTERACTIVE('keyboard-interactive')

    String value

    private AuthType(String value) {
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