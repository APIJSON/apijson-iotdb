/*Copyright ©2024 APIJSON(https://github.com/APIJSON)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package apijson.iotdb;

import apijson.JSONResponse;
import apijson.NotNull;
import apijson.RequestMethod;
import apijson.StringUtil;
import apijson.orm.AbstractParser;
import apijson.orm.SQLConfig;
import com.alibaba.fastjson.JSONObject;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.tsfile.read.common.Field;
import org.apache.iotdb.tsfile.read.common.RowRecord;

import java.util.*;

import static apijson.orm.AbstractSQLExecutor.KEY_RAW_LIST;


/**
 * @author Lemon
 * @see DemoSQLExecutor 重写 execute 方法：
 *     \@Override
 *      public JSONObject execute(@NotNull SQLConfig<Long> config, boolean unknownType) throws Exception {
 *          if (config.isIoTDB()) {
 *              return IoTDBUtil.execute(config, null, unknownType);
 *          }
 *
 *          return super.execute(config, unknownType);
 *     }
 *
 *     DemoSQLConfig 重写方法 getSchema, getSQLSchema 方法
 *    \@Override
 *     public String getSchema() {
 * 	       return IoTDBUtil.getSchema(super.getSchema(), DEFAULT_SCHEMA, isIoTDB());
 *     }
 *
 *    \@Override
 *     public String getSQLSchema() {
 * 		   return IoTDBUtil.getSQLSchema(super.getSQLSchema(), isIoTDB());
 *     }
 */
public class IoTDBUtil {
    public static final String TAG = "IoTDBUtil";

    public static String getSchema(String schema, String defaultSchema) {
        return getSchema(schema, defaultSchema, true);
    }
    public static String getSchema(String schema, String defaultSchema, boolean isIoTDB) {
        if (StringUtil.isEmpty(schema) && isIoTDB) {
            schema = defaultSchema;
        }
        return schema;
    }

    public static String getSQLSchema(String schema) {
        return getSQLSchema(schema, true);
    }
    public static String getSQLSchema(String schema, boolean isIoTDB) {
        return schema;
    }

    public static String getTablePath(String path) {
        return getTablePath(path, true);
    }
    public static String getTablePath(String path, boolean isIoTDB) {
        if (isIoTDB) {
            String[] ks = path == null || path.trim().isEmpty() ? null : path.trim().split("\\.");
            int len = ks == null ? 0 : ks.length;
            return len <= 0 ? "**" : (len >= 3 ? path : path + ".**");
        }

        return path;
    }

    public static <T> String getSessionKey(@NotNull SQLConfig<T> config) {
        String uri = config.getDBUri();
        return uri + (uri.contains("?") ? "&" : "?") + "username=" + config.getDBAccount();
    }

    public static final Map<String, Session> CLIENT_MAP = new LinkedHashMap<>();
    public static <T> Session getSession(@NotNull SQLConfig<T> config) throws IoTDBConnectionException {
        return getSession(config, true);
    }
    public static <T> Session getSession(@NotNull SQLConfig<T> config, boolean autoNew) throws IoTDBConnectionException {
        String key = getSessionKey(config);

        Session session = CLIENT_MAP.get(key);
        if (autoNew && session == null) {
            String uri = config.getDBUri();
            int ind = uri.indexOf("://");
            String host = ind < 0 ? uri : uri.substring(ind + 3);
            int ind2 = host.indexOf("?");
            host = ind2 < 0 ? host : host.substring(0, ind2);
            int ind3 = host.indexOf(":");
            String portStr = ind < 0 ? null : host.substring(ind3 + 1);
            host = ind3 < 0 ? host : host.substring(0, ind3);
            int port = portStr == null || portStr.trim().isEmpty() ? 6667 : Integer.valueOf(portStr);

            session = new Session.Builder()
                    .host(host)
                    .port(port)
                    .username(config.getDBAccount())
                    .password(config.getDBPassword())
                    .build();
            session.open();

            Session finalSession = session;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    finalSession.close();
                } catch (IoTDBConnectionException e) {
                    e.printStackTrace(); // throw new RuntimeException(e);
                }
            }));

            CLIENT_MAP.put(key, session);
        }

        return session;
    }

    public static <T> void closeSession(@NotNull SQLConfig<T> config) {
        try {
            Session session = getSession(config, false);
            if (session != null) {
                String key = getSessionKey(config);
                CLIENT_MAP.remove(key);

                try {
                    session.close();
                }
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static <T> void closeAllSession() {
        Collection<Session> cs = CLIENT_MAP.values();
        for (Session c : cs) {
            try {
                c.close();
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }

        CLIENT_MAP.clear();
    }


    public static <T> JSONObject execute(@NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        if (RequestMethod.isQueryMethod(config.getMethod())) {
            List<JSONObject> list = executeQuery(config, sql, unknownType);
            JSONObject result = list == null || list.isEmpty() ? null : list.get(0);
            if (result == null) {
                result = new JSONObject(true);
            }

            if (list != null && list.size() > 1) {
                result.put(KEY_RAW_LIST, list);
            }

            return result;
        }

        return executeUpdate(config, sql);
    }

    public static <T> int execUpdate(SQLConfig<T> config, String sql) throws Exception {
        JSONObject result = executeUpdate(config, sql);
        return result.getIntValue(JSONResponse.KEY_COUNT);
    }

    public static <T> JSONObject executeUpdate(SQLConfig<T> config, String sql) throws Exception {
        return executeUpdate(null, config, sql);
    }
    public static <T> JSONObject executeUpdate(Session session, SQLConfig<T> config, String sql) throws Exception {
        if (session == null) {
            session = getSession(config);
        }

        session.executeNonQueryStatement(sql);

        JSONObject result = AbstractParser.newSuccessResult();

        RequestMethod method = config.getMethod();
        if (method == RequestMethod.POST) {
            List<List<Object>> values = config.getValues();
            result.put(JSONResponse.KEY_COUNT, values == null ? 0 : values.size());
        } else {
            String idKey = config.getIdKey();
            Object id = config.getId();
            Object idIn = config.getIdIn();
            if (id != null) {
                result.put(idKey, id);
            }
            if (idIn != null) {
                result.put(idKey + "[]", idIn);
            }

            if (method == RequestMethod.PUT) {
                Map<String, Object> content = config.getContent();
                result.put(JSONResponse.KEY_COUNT, content == null ? 0 : content.size());
            } else {
                result.put(JSONResponse.KEY_COUNT, id == null && idIn instanceof Collection ? ((Collection<?>) idIn).size() : 1); // FIXME 直接 SQLAuto 传 Flux/InfluxQL INSERT 如何取数量？
            }
        }

        return result;
    }


    public static <T> JSONObject execQuery(@NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        List<JSONObject> list = executeQuery(config, sql, unknownType);
        JSONObject result = list == null || list.isEmpty() ? null : list.get(0);
        if (result == null) {
            result = new JSONObject(true);
        }

        if (list != null && list.size() > 1) {
            result.put(KEY_RAW_LIST, list);
        }

        return result;
    }

    public static <T> List<JSONObject> executeQuery(@NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        return executeQuery(null, config, sql, unknownType);
    }
    public static <T> List<JSONObject> executeQuery(Session session, @NotNull SQLConfig<T> config, String sql, boolean unknownType) throws Exception {
        if (session == null) {
            session = getSession(config);
        }

//        session.setDatabase(config.getSchema());

        SessionDataSet ds = session.executeQueryStatement(sql);
        List<String> ns = ds == null ? null : ds.getColumnNames();
        List<String> nameList = ns == null || ns.isEmpty() ? null : new ArrayList<>(ns.size());

        if (nameList != null) {
            String prefix = config.getSQLSchema() + "." + config.getSQLTable() + ".";

            for (String name : ns) {
                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                }

                nameList.add(name);
            }
        }

        if (nameList == null || nameList.isEmpty()) {
            return null;
        }

        List<JSONObject> resultList = new ArrayList<>(ds.getFetchSize());

        while (ds.hasNext()) {
            RowRecord row = ds.next();
            List<Field> fs = row.getFields();

            JSONObject obj = new JSONObject(true);
            obj.put(nameList.get(0), row.getTimestamp());
            for (int i = 0; i < fs.size(); i++) {
                Field f = fs.get(i);
                Object v = f == null ? null : f.getObjectValue(f.getDataType());
                obj.put(nameList.get(i + 1), v);
            }

            resultList.add(obj);
        }

        return resultList;
    }

}
