package liming.handle.connection;

import liming.item.annotate.UtextAnnotate;

@UtextAnnotate("配置jdbc版本")
public enum DriverClassName {
    @UtextAnnotate("连接MySQL 8.x")
    MySql8(0, "com.mysql.cj.jdbc.Driver"),
    @UtextAnnotate("连接MySQL 5.x")
    MySql5(1, "com.mysql.jdbc.Driver");

    // @UtextAnnotate("连接Oracle 11g")
    // Oracle11g("","oracle.jdbc.driver.OracleDriver"),
    // @UtextAnnotate("连接SQL Server 2019")
    // SqlServer2019("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    private int num;
    private String value;

    DriverClassName(int num, String DriverClassName) {
        this.num = num;
        this.value = DriverClassName;
    }

    public String getDriverClassName() {
        return value;
    }

    public static String getURL(SQL sql) {
        switch (sql.Driver().num) {
            case 0:
            case 1:
                return JDBC(sql);
            default:
                throw new RuntimeException();
        }
    }

    private static String JDBC(SQL sql) {
        return "jdbc:mysql://" + sql.hostname() + ":" + sql.port() + "/" + sql.databaseName()
                + "?useUnicode=" + sql.useUnicode() + "&characterEncoding=" + sql.characterEncoding()
                + "&autoReconnectForPools=" + sql.autoReconnectForPools() + "&autoReconnect="
                + sql.autoReconnect() + "&failOverReadOnly=" + sql.failOverReadOnly()
                + "&maxReconnects=" + sql.maxReconnects() + "&initialTimeout=" + sql.initialTimeout()
                + "&connectTimeout=" + sql.connectTimeout() + "&socketTimeout=" + sql.socketTimeout();
        // + "&serverTimezone=" + sql.serverTimezone();
    }
}
