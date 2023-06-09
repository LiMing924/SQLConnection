package liming.handle.connection;

import liming.item.annotate.UtextAnnotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@UtextAnnotate("若注解在方法上，尽可能避免使用 ... 方法传入可变参数，以免不必要的错误")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface SQL {
    @UtextAnnotate("使用其他已经配置的sql别名")
    String sqlName() default "";

    @UtextAnnotate("别名,未配置则默认使用类全限定名加方法名")
    String name() default "";

    @UtextAnnotate("数据库用户名（用于连接数据库）")
    String username() default "root";

    @UtextAnnotate("用户密码（用于连接数据库）")
    String password() default "";

    @UtextAnnotate("ip")
    String hostname() default "localhost";

    @UtextAnnotate("端口号")
    int port() default 3306;

    @UtextAnnotate("数据库名称")
    String databaseName() default "";

    @UtextAnnotate("连接配置")
    DriverClassName Driver() default DriverClassName.MySql8;

    @UtextAnnotate("编码")
    String characterEncoding() default "UTF-8";

    @UtextAnnotate("是否使用Unicode字符集，如果参数characterEncoding设置为gb2312或gbk，本参数值必须设置为true")
    boolean useUnicode() default false;

    @UtextAnnotate("当数据库连接异常中断时，是否自动重新连接？")
    boolean autoReconnect() default false;

    @UtextAnnotate("是否使用针对数据库连接池的重连策略")
    boolean autoReconnectForPools() default false;

    @UtextAnnotate("自动重连成功后，连接是否设置为只读？")
    boolean failOverReadOnly() default true;

    @UtextAnnotate("autoReconnect设置为true时，重试连接的次数")
    int maxReconnects() default 3;

    @UtextAnnotate("autoReconnect设置为true时，两次重连之间的时间间隔，单位：秒")
    int initialTimeout() default 2;

    @UtextAnnotate("和数据库服务器建立socket连接时的超时，单位：毫秒。 0表示永不超时，适用于JDK 1.4及更高版本")
    long connectTimeout() default 0;

    @UtextAnnotate("socket操作（读写）超时，单位：毫秒。 0表示永不超时")
    long socketTimeout() default 0;

    @UtextAnnotate("设置时区")
    String serverTimezone() default "UTC";

    @UtextAnnotate("当使用JDBC连接其他数据库时所用的URL")
    String otherUrl() default "";

    @UtextAnnotate("当使用JDBC连接其他数据库时所用的驱动程序")
    String otherDriver() default "";
}
