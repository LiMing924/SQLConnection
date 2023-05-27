package liming.handle.connection;

import liming.item.annotate.UtextAnnotate;

@UtextAnnotate("配置jdbc版本")
public enum DriverClassName {
    @UtextAnnotate("连接MySQL 8.x")
    MySql8("com.mysql.cj.jdbc.Driver"),
    @UtextAnnotate("连接MySQL 5.x")
    MySql5("com.mysql.jdbc.Driver"),
    @UtextAnnotate("连接Oracle 11g")
    Oracle11g("oracle.jdbc.driver.OracleDriver"),
    @UtextAnnotate("连接SQL Server 2019")
    SqlServer2019("com.microsoft.sqlserver.jdbc.SQLServerDriver");

    private String value;

    DriverClassName(String DriverClassName) {
        this.value = DriverClassName;
    }

    public String getDriverClassName() {
        return value;
    }
}
