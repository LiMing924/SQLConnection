package liming.handle.connection;

import liming.item.annotate.StextAnnotate;
import liming.item.annotate.UtextAnnotate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@StextAnnotate(Private = false, Protected = false)
@UtextAnnotate("数据库连接类")
public class SQLConnection {

	private Map<String, SQLInfo> connections;

	public SQLConnection(Object... connectionObjects) throws InstantiationException {
		for (Object connectionObject : connectionObjects)
			if (connectionObject instanceof Class) {
				throw new InstantiationException("");
			}
		connections = new HashMap<>();
		for (Object connectionObject : connectionObjects) {
			Method[] methods = connectionObject.getClass().getDeclaredMethods();
			for (Method method : methods) {
				if (method.isAnnotationPresent(SQL.class)) {
					SQL sql = method.getAnnotation(SQL.class);
					if (connections.containsKey(sql.name()))
						continue;
					try {
						String url = "jdbc:mysql://" + sql.hostname() + ":" + sql.port() + "/" + sql.databaseName()
								+ "?useUnicode=" + sql.useUnicode() + "&characterEncoding=" + sql.characterEncoding()
								+ "&autoReconnectForPools=" + sql.autoReconnectForPools() + "&autoReconnect="
								+ sql.autoReconnect() + "&failOverReadOnly=" + sql.failOverReadOnly()
								+ "&maxReconnects=" + sql.maxReconnects() + "&initialTimeout=" + sql.initialTimeout()
								+ "&connectTimeout=" + sql.connectTimeout() + "&socketTimeout=" + sql.socketTimeout();
						Class.forName(sql.Driver().getDriverClassName());
						Connection connection = DriverManager.getConnection(url, sql.username(), sql.password());
						if (sql.name().equals(""))
							connections.put(method.getDeclaringClass().getName() + "." + method.getName(),
									new SQLInfo(connectionObject, method, connection));
						else
							connections.put(sql.name(), new SQLInfo(connectionObject, method, connection));
					} catch (SQLException e) {
						e.printStackTrace();
						System.err.println(sql.Driver().getDriverClassName());
						System.err.println(sql.name() + "方法异常 " + e.getMessage());
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	@UtextAnnotate("通过设置的名称调用方法")
	public synchronized Object runConnection(String name)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		if (connections.containsKey(name)) {
			SQLInfo info = connections.get(name);
			return info.method.invoke(info.object, info.connection);
		} else
			throw new NoSuchMethodException("未找到 " + name + " 的方法");
	}

	private static class SQLInfo {
		Object object;
		Method method;
		Connection connection;

		public SQLInfo(Object object, Method method, Connection connection) {
			this.object = object;
			this.method = method;
			this.connection = connection;
		}
	}
}
