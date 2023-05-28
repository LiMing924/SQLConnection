package liming.handle.connection;

import liming.item.annotate.StextAnnotate;
import liming.item.annotate.UtextAnnotate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@StextAnnotate(Private = false, Protected = false)
@UtextAnnotate("数据库连接类")
public class SQLConnection {
	@UtextAnnotate("方法别名与对应方法信息")
	private Map<String, SQLInfo> connectionInfos;
	@UtextAnnotate("原始别名name与连接Connection,原始别名与连接映射表")
	private Map<String, Connection> connections;
	@UtextAnnotate("别名与继承别名,别名与原始别名映射表")
	private Map<String, String> mappingName;

	public Set<String> getProtoConnectionName() {// 原始别名
		return connections.keySet();
	}

	public Map<String, String> getNames() {// 别名映射表
		return mappingName;
	}

	public Set<String> getMethodName() {// 方法别名
		return connectionInfos.keySet();
	}

	public SQLConnection(Object... connectionObjects) throws InstantiationException, ClassNotFoundException,
			SQLException, IllegalArgumentException, IllegalAccessException {
		{// 判断传入对象是否都为非Class对象
			int num = 1;
			for (Object connectionObject : connectionObjects) {
				if (connectionObject instanceof Class) {
					throw new InstantiationException("传入的第 " + num + " 个参数异常，非实例化对象");
				}
				num++;
			}
		}

		// 初始化容器
		connectionInfos = new HashMap<>();// 存放方法别名与对应方法体
		connections = new HashMap<>();// 存放原始别名及对应连接
		mappingName = new HashMap<>();// 存放别名与原始连接的映射

		// 设置临时容器 用于存放非原始别名且所继承的别名暂未加载的方法或成员
		Map<String, ConnectionFieldInfo> tempField = new HashMap<>(); // 当前别名 -- 继承别名 变量
		Map<String, NamePath> tempName = new HashMap<>();// nameCope 缓存
		Map<String, NamePath> tempNameAll = new HashMap<>();

		// 循环遍历，将原始别名加在到缓存。在循环完后所有的原始别名将存放在connections中，会加载部分非原始别名到codeCope中
		for (Object connectionObject : connectionObjects) {
			Class<?> clazz = connectionObject.getClass();
			Field[] fields = clazz.getDeclaredFields();
			for (Field field : fields) {// 处理成员变量
				if (!field.isAnnotationPresent(SQL.class))
					continue;
				SQL sql = field.getAnnotation(SQL.class);
				String name = sql.name();// 获取别名
				if (name.equals("")) {// 矫正别名
					name = clazz.getName() + "." + field.getName();
				}
				// 别名去重
				if (tempNameAll.containsKey(name)) {
					throw new RuntimeException("\n\r\tSQLConnection: 在 " + clazz.getName() + "." + field.getName()
							+ " 成员变量中设置的@SQL注解name字段出现重复,已记录的位置为 " + tempNameAll.get(name).path);
				}

				String sqlName = sql.sqlName();// 获取继承别名
				String path = "变量:" + clazz.getName() + "." + field.getName();// 当前全限定名
				if (sqlName.equals("")) {// 判断是否为原始别名
					Connection connection = getConnection(sql);
					connections.put(name, connection);
					field.set(connectionObject, connection);// 给当前变量赋值
					tempNameAll.put(name, new NamePath(path, name));
				} else {
					Connection connection = getConnection(sqlName);
					if (connection != null) {
						mappingName.put(name, getConnectionName(sqlName));
						field.set(connectionObject, connection);
					} else {
						// 原始连接是不存在 放入缓冲
						tempField.put(name, new ConnectionFieldInfo(connectionObject, field));
						tempName.put(name, new NamePath(path, sqlName));
					}
					tempNameAll.put(name, new NamePath(path, sqlName));
				}
			}
			Method[] methods = clazz.getDeclaredMethods();
			for (Method method : methods) {// 设置成员方法
				if (!method.isAnnotationPresent(SQL.class))
					continue;
				SQL sql = method.getAnnotation(SQL.class);
				String name = sql.name();
				if (name.equals(""))
					name = clazz.getName() + "." + method.getName();

				// 别名去重
				if (tempNameAll.containsKey(name)) {
					throw new RuntimeException("\n\r\tSQLConnection: 在 " + clazz.getName() + "." + method.getName()
							+ " 成员方法中设置的@SQL注解name字段出现重复,已记录的位置为 " + tempNameAll.get(name).path);
				}

				String sqlName = sql.sqlName();
				String path = "方法:" + clazz.getName() + "." + method.getName();// 当前全限定名
				if (sqlName.equals("")) {// 设置原始别名
					Connection connection = getConnection(sql);// 获取连接
					connections.put(name, connection);// 注册信息
					connectionInfos.put(name, new SQLInfo(connectionObject, method));// 添加到方法区缓冲
					tempNameAll.put(name, new NamePath(path, name));// 注册防重
				} else {
					Connection connection = getConnection(sqlName);
					connectionInfos.put(name, new SQLInfo(connectionObject, method));

					if (connection != null)
						mappingName.put(name, getConnectionName(sqlName));

					else {
						// 原始连接是不存在 放入缓冲
						tempName.put(name,
								new NamePath(clazz.getName() + "." + method.getName(), sqlName));
					}
					tempNameAll.put(name, new NamePath(path, sqlName));
				}
			}
		} // 对未处理的部分进行处理
		for (String name : tempName.keySet()) {// 别名
			String sqlname = findName(name, new ArrayList<>(), tempName);// 找到原始别名
			mappingName.put(name, sqlname);
			if (!connectionInfos.containsKey(name)) {
				ConnectionFieldInfo info = tempField.get(name);
				info.field.set(info.object, getConnection(sqlname));
			}
		}
	}

	private static Connection getConnection(SQL sql) throws ClassNotFoundException, SQLException {
		String url = "jdbc:mysql://" + sql.hostname() + ":" + sql.port() + "/" + sql.databaseName()
				+ "?useUnicode=" + sql.useUnicode() + "&characterEncoding=" + sql.characterEncoding()
				+ "&autoReconnectForPools=" + sql.autoReconnectForPools() + "&autoReconnect="
				+ sql.autoReconnect() + "&failOverReadOnly=" + sql.failOverReadOnly()
				+ "&maxReconnects=" + sql.maxReconnects() + "&initialTimeout=" + sql.initialTimeout()
				+ "&connectTimeout=" + sql.connectTimeout() + "&socketTimeout=" + sql.socketTimeout();
		Class.forName(sql.Driver().getDriverClassName());
		return DriverManager.getConnection(url, sql.username(), sql.password());
	}

	private String findName(String name, List<String> list, Map<String, NamePath> temp) {
		if (connections.containsKey(name))// 如果为原始别名则返回这个名字
			return name;
		if (mappingName.containsKey(name)) {// 如果为原始别名则返回这个名字
			return mappingName.get(name);
		}
		if (list.contains(name)) {// 如果成环则抛出异常
			list.add(name);
			throw new RuntimeException("name= " + name + " 在@SQL注解中存在依赖循环 " + list);
		}
		if (temp.containsKey(name)) {
			list.add(name);
			return findName(temp.get(name).sqlName, list, temp);
		}
		throw new RuntimeException("name= " + name + " 在@SQL注解中存在依赖缺失");
	}

	@UtextAnnotate("通过别名获取连接")
	public synchronized Connection getConnection(String name) {
		name = getConnectionName(name);
		return name == null ? null : connections.get(name);
	}

	@UtextAnnotate("获取当前别名的原始别名")
	private synchronized String getConnectionName(String name) {
		if (connections.containsKey(name)) {// 判断是否为原始别名
			return name;
		} else if (mappingName.containsKey(name)) {// 判断是否在别名映射表中
			return mappingName.get(name);
		}
		return null;
	}

	@UtextAnnotate("通过设置的名称调用方法")
	public synchronized Object runConnection(String name)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		if (connectionInfos.containsKey(name)) {
			SQLInfo info = connectionInfos.get(name);
			return info.method.invoke(info.object, getConnection(name));
		} else
			throw new NoSuchMethodException("未找到 " + name + " 的方法");
	}

	private static class NamePath {
		String path;
		String sqlName;

		public NamePath(String path, String sqlName) {
			this.path = path;
			this.sqlName = sqlName;
		}

	}

	private static class SQLInfo {
		Object object;// 执行对象
		Method method;// 方法体

		public SQLInfo(Object object, Method method) {
			this.object = object;
			this.method = method;
		}
	}

	private static class ConnectionFieldInfo {
		Object object;
		Field field;

		public ConnectionFieldInfo(Object object, Field field) {
			this.object = object;
			this.field = field;
		}
	}
}
