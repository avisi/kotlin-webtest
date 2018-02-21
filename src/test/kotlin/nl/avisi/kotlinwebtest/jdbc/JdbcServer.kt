package nl.avisi.kotlinwebtest.jdbc

import org.apache.commons.io.IOUtils
import org.h2.tools.DeleteDbFiles
import org.h2.tools.Server
import java.sql.DriverManager


class JdbcServer : AutoCloseable {
    private val initScript = IOUtils.toString(javaClass.classLoader.getResourceAsStream("jdbc/create.sql"))
    private val JDBC_DRIVER = "org.h2.Driver"
    private val DB_URL = "jdbc:h2:tcp://localhost/~/database"

    companion object {
        private lateinit var server: Server
    }

    init {
        DeleteDbFiles.execute("~", "database", true)
        server = Server.createTcpServer().start()
        Class.forName(JDBC_DRIVER)
        DriverManager.getConnection(DB_URL).use {
            it.createStatement().use {
                it.execute(initScript)
            }
        }
    }

    override fun close() {
        server.stop()
        DeleteDbFiles.execute("~", "database", true)
    }
}