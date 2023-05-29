package liming.handle.connection;

import liming.item.annotate.RtextAnnotate;
import liming.item.annotate.StextAnnotate;
import liming.item.annotate.UtextAnnotate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

@StextAnnotate(Private = false, Protected = false)
@UtextAnnotate("数据库连接类,通过该类的构造方法可以给使用SQL注解的地方赋值")
public class SQLConnection {

	@UtextAnnotate("方法别名与对应方法信息")
	private Map<String, SQLInfo> connectionInfos;

	@UtextAnnotate("原始别名name与连接Connection,原始别名与连接映射表")
	private Map<String, Connection> connections;

	@UtextAnnotate("别名与继承别名,别名与原始别名映射表")
	private Map<String, String> mappingName;

	@UtextAnnotate("传入使用SQL注解的对象")
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
		Map<String, NamePath> tempNameAll = new HashMap<>();// 映射总关系

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
						// 原始别名不存在 放入缓冲
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
				connectionInfos.put(name, new SQLInfo(connectionObject, method));// 添加到方法区缓冲
				if (sqlName.equals("")) {// 判断继承别名
					Connection connection = getConnection(sql);// 获取连接
					connections.put(name, connection);// 注册信息
					tempNameAll.put(name, new NamePath(path, name));// 注册防重
				} else {
					Connection connection = getConnection(sqlName);
					if (connection != null)
						mappingName.put(name, getConnectionName(sqlName));
					else {
						// 原始连接是不存在 放入缓冲
						tempName.put(name,
								new NamePath(path, sqlName));
					}
					tempNameAll.put(name, new NamePath(path, sqlName));
				}
			}
		}
		// 对未处理的部分进行处理
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
		String url;
		if (sql.otherUrl().equals("")) {
			url = DriverClassName.getURL(sql);
			Class.forName(sql.Driver().getDriverClassName());
		} else {
			url = sql.otherUrl();
			Class.forName(sql.otherDriver());
		}

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
			NamePath path = temp.get(name);
			path.sqlName = findName(path.sqlName, list, temp);
			return temp.put(name, path).sqlName;
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

	@UtextAnnotate("通过设置的name调用方法")
	@RtextAnnotate("方法处理结果")
	public synchronized Object runConnection(String name, Object... values)
			throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		if (connectionInfos.containsKey(name)) {
			SQLInfo info = connectionInfos.get(name);
			Class<?>[] classes = info.method.getParameterTypes();
			if (classes.length != values.length + 1) {
				throw new IllegalArgumentException(
						"参数类型缺失，传入参数个数：" + values.length + ",所需参数个数：" + (classes.length - 1));
			}
			for (int i = 0; i < classes.length - 1; i++) {
				Object value = values[i];
				Class<?> clazz = classes[i + 1];
				if (clazz.isPrimitive()) { // 判断是否为基本类型
					if (clazz == int.class && value instanceof Integer) {
						values[i] = ((Integer) value).intValue();
					} else if (clazz == long.class && value instanceof Long) {
						values[i] = ((Long) value).longValue();
					} else if (clazz == float.class && value instanceof Float) {
						values[i] = ((Float) value).floatValue();
					} else if (clazz == double.class && value instanceof Double) {
						values[i] = ((Double) value).doubleValue();
					} else if (clazz == boolean.class && value instanceof Boolean) {
						values[i] = ((Boolean) value).booleanValue();
					} else if (clazz == byte.class && value instanceof Byte) {
						values[i] = ((Byte) value).byteValue();
					} else if (clazz == short.class && value instanceof Short) {
						values[i] = ((Short) value).shortValue();
					} else if (clazz == char.class && value instanceof Character) {
						values[i] = ((Character) value).charValue();
					} else {
						throw new IllegalArgumentException(
								"参数类型不匹配，所需参数为=[ " + getParameterTypes(info.method) + " ],传入参数为=[ "
										+ getTypeNames(values) + " ]");
					}
				} else { // 不是基本类型
					if (!clazz.isInstance(value)) {
						throw new IllegalArgumentException(
								"参数类型不匹配，所需参数为=[ " + getParameterTypes(info.method) + " ],传入参数为=["
										+ getTypeNames(values) + "]");
					}
				}
			}
			if (values == null || values.length == 0)
				return info.method.invoke(info.object, getConnection(name));
			else
				switch (values.length) {
					case 1:
						return info.method.invoke(info.object, getConnection(name), values[0]);
					case 2:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1]);
					case 3:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2]);
					case 4:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2],
								values[3]);
					case 5:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2],
								values[3], values[4]);
					case 6:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2],
								values[3], values[4], values[5]);
					case 7:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2],
								values[3], values[4], values[5], values[6]);
					case 8:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2],
								values[3], values[4], values[5], values[6], values[7]);
					case 9:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2],
								values[3], values[4], values[5], values[6], values[7], values[8]);
					case 10:
						return info.method.invoke(info.object, getConnection(name), values[0], values[1], values[2],
								values[3], values[4], values[5], values[6], values[7], values[8], values[9]);
					default:
						throw new RuntimeException("参数过长");
				}
		} else
			throw new NoSuchMethodException("未找到 " + name + " 的方法");
	}

	private static String getParameterTypes(Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < parameterTypes.length; i++) {
			sb.append(parameterTypes[i].getSimpleName());
			if (i != parameterTypes.length - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	private static String getTypeNames(Object... values) {
		if (values == null)
			return "无";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			sb.append(values[i].getClass().getSimpleName());
			if (i != values.length - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public static List<JSONObject> resultSetToList(ResultSet resultSet) throws SQLException {
		List<JSONObject> list = new ArrayList<>();
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		while (resultSet.next()) {
			JSONObject json = new JSONObject();
			for (int i = 1; i <= columnCount; i++) {
				String columnName = metaData.getColumnName(i);
				Object value = resultSet.getObject(i);
				json.put(columnName, value);
			}
			list.add(json);
		}
		return list;
	}

	@UtextAnnotate("")
	public Set<String> getProtoConnectionName() {// 原始别名
		return connections.keySet();
	}

	public Map<String, String> getNames() {// 别名映射表
		return mappingName;
	}

	public Set<String> getMethodName() {// 方法别名
		return connectionInfos.keySet();
	}

	public void clear() throws SQLException {
		for (String name : connections.keySet()) {
			Connection connection = connections.get(name);
			connection.close();
		}
		connectionInfos.clear();
		connections.clear();
		mappingName.clear();
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
