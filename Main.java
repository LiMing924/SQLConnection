import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import liming.handle.connection.SQLConnection;

public class Main {
    public static void main(String[] args)
            throws InstantiationException, InvocationTargetException, IllegalAccessException, NoSuchMethodException,
            ClassNotFoundException, IllegalArgumentException, SQLException {
        Try1 try1 = new Try1();
        SQLConnection connection = new SQLConnection(try1);
        Map<String, String> map = connection.getNames();
        for (String key : map.keySet()) {
            System.out.println(key + " " + map.get(key));
        }
        System.out.println("=========");
        Set<String> set = connection.getProtoConnectionName();
        for (String key : set) {
            System.out.println(key);
        }
        System.out.println("=========");

        Set<String> set1 = connection.getMethodName();
        for (String key : set1) {
            System.out.println(key);
        }
        // connection.runConnection("r2");
    }
}
