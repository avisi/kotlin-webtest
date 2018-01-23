/**
 * For licensing, see LICENSE.txt
 * @author Rein Krul
 */
package nl.avisi.kotlinwebtest

interface Credentials

class UsernamePassword(val user: String, val password: String) : Credentials