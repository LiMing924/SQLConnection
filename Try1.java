import java.sql.Connection;

import liming.handle.connection.SQL;

public class Try1 {
    @SQL(name = "c1", username = "root", password = "962464", databaseName = "server")
    public Connection connection1;

    @SQL(name = "r1", username = "root", password = "962464", databaseName = "server")
    public void run1(Connection connection) {

    }

    @SQL(name = "c2", sqlName = "c1")
    public Connection connection2;

    @SQL(name = "r2", sqlName = "r1")
    public void run2(Connection connection) {

    }

    @SQL(name = "c3", sqlName = "r1")
    public Connection connection3;

    @SQL(name = "r3", sqlName = "r2")
    public void run3(Connection connection) {

    }

    // @SQL(name = "r2", sqlName = "c3")
    // public void run4(Connection connection) {

    // }

    @SQL(name = "r4", sqlName = "r5")
    public void run4(Connection connection) {

    }

    @SQL(name = "r5", sqlName = "r6")
    public void run5(Connection connection) {

    }

    @SQL(name = "r6", sqlName = "r4")
    public void run6(Connection connection) {

    }
}
