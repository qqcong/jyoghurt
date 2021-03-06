package com.github.jyoghurt.core.dao;


import com.github.jyoghurt.core.domain.BaseEntity;
import com.github.jyoghurt.core.enums.LogSystemType;
import com.github.jyoghurt.core.exception.DaoException;
import com.github.jyoghurt.core.exception.UtilException;
import com.github.jyoghurt.core.handle.OperatorHandle;
import com.github.jyoghurt.core.handle.SQLJoinHandle;
import com.github.jyoghurt.core.utils.JPAUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ClassUtils;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import static com.github.jyoghurt.core.mybatis.SqlBuilder.*;

/**
 * Created by jtwu on 2015/4/21.
 */
public class BaseMapperProvider {

    private List<SQLJoinHandle> sqlJoinHandles;
    private Class<?> entityClass;
    protected String limit;

    private void begin() {
        reset();
        BEGIN();
    }

    private void reset() {
        sqlJoinHandles = new ArrayList<>();
    }

    private String sql() {
        reset();
        return SQL();
    }

    /**
     * 查询所有数据sql
     *
     * @param param 参数
     * @return sql
     * @{@inheritDoc}
     */
    public String findAll(Map<String, Object> param) {
        beginWithClass(param);
        //处理查询列问题 add by limiao 20170508
        Map<String, Object> map = ((Map<String, Object>) param.get(BaseMapper.DATA));
        String selectColumnSql = createSelectColumnSql(map);
        if (MapUtils.isNotEmpty(map) && map.containsKey("distinct")) {
            SELECT_DISTINCT(selectColumnSql);
        } else {
            SELECT(selectColumnSql);
        }
        FROM(getTableNameWithAlias(entityClass));
        createAllWhere(map, false);
        return StringUtils.join(sql(), limit);
    }


    protected void createAllWhere(Map<String, Object> param, boolean usePage) {
        createAllWhere(param, usePage, false);
    }

    private void createAllWhere(Map<String, Object> param, boolean usePage, boolean isCount) {
        if (MapUtils.isEmpty(param)) {
            return;
        }
        try {
            Map<String, OperatorHandle> operatorMap = param.containsKey("operatorHandles") ?
                    (Map) param.get("operatorHandles") : new HashMap();
            createFieldsWhereSql(operatorMap, param);
            parseQueryHandle(param, usePage, isCount);
        } catch (Exception e) {
            throw new DaoException(e);
        }
    }


    private void createFieldsWhereSql(Map<String, OperatorHandle> operatorMap, Map<String, Object> param) {
        createFieldsWhereSql(operatorMap, param, entityClass);
    }

    private void createFieldsWhereSql(Map<String, OperatorHandle> operatorMap, Map<String, Object> param,
                                      Class clazz) {
//        处理非本类字段
        for (String key : operatorMap.keySet()) {
            if (key.contains(".")) {
                createColumnWhereSql(operatorMap, StringUtils.EMPTY, key);
            }
        }
//        处理本类字段
        for (Field field : JPAUtils.getAllFields(clazz)) {
            createFieldWhereSql(operatorMap, field, param);
        }
    }

    private boolean createFieldWhereSql(Map<String, OperatorHandle> operatorMap, Field field, Map<String, Object> param) {
        //验证是否需要拼装该属性
        if (!validateField2WhereSql(operatorMap, field, param)) {
            return false;
        }

        //封装类型递归处理
        if (null != field.getType().getAnnotation(Table.class)) {
            return false;
        }
        if (!operatorMap.containsKey(field.getName())) {
            WHERE(StringUtils.join("t.", getEqualsValue(field.getName(),
                    StringUtils.join(BaseMapper.DATA + ".", field.getName()))));
            return true;
        }
        createColumnWhereSql(operatorMap, "t.", field.getName());
        return true;
    }

    private void createColumnWhereSql(Map<String, OperatorHandle> operatorMap, String tableAlias, String fieldName) {
        /*
         * 将参数变量的"."改成"_"，eg table.id改成table_id,
         * 不改mybatis会将解释成table对象的id属性，而非table.id变量
         */

        for (String key : operatorMap.keySet()) {
            if (key.contains(".")) {
                operatorMap.put(key.replace(".", "_"), operatorMap.get(key));
            }
        }

//        String value = getColumnValues(operatorMap, fieldName);
        String value = ArrayUtils.isEmpty((operatorMap.get(fieldName)).getValues())
                ? "#{" + StringUtils.join(BaseMapper.DATA + "." + fieldName) + "}"
                : "#{" + BaseMapper.DATA + ".operatorHandles." + fieldName.replace(".", "_") + ".values[0]}";

        switch ((operatorMap.get(fieldName)).getOperator()) {
            case EQUAL: {
                WHERE(StringUtils.join(tableAlias, fieldName, " = ", value));
                break;
            }
            case LIKE: {
                WHERE(StringUtils.join(tableAlias, fieldName, " like CONCAT('%',", value, ", '%')"));
                break;
            }
            case LESS_THEN: {
                WHERE(StringUtils.join(tableAlias, fieldName, " < ", value));
                break;
            }
            case MORE_THEN: {
                WHERE(StringUtils.join(tableAlias, fieldName, " > ", value));
                break;
            }
            case LESS_EQUAL: {
                WHERE(StringUtils.join(tableAlias, fieldName, " <= ", value));
                break;
            }
            case MORE_EQUAL: {
                WHERE(StringUtils.join(tableAlias, fieldName, " >= ", value));
                break;
            }
            case NOT_IN: {
                WHERE(StringUtils.join(tableAlias, fieldName, " not in ", getInValuesSql(operatorMap, fieldName)));
                break;
            }
            case IN: {
                WHERE(StringUtils.join(tableAlias, fieldName, " in ", getInValuesSql(operatorMap, fieldName)));
                break;
            }
            case FIND_IN_SET: {
                WHERE("find_in_set(" + value + ",t." + fieldName + ")");
                break;
            }
            /* add by limiao 20160203 ,新增NOT_EQUAL, NOT_LIKE,IS_NULL ,IS_NOT_NULL   */
            case NOT_EQUAL: {
                WHERE(StringUtils.join(tableAlias, fieldName, " != ", value));
                break;
            }
            case NOT_LIKE: {
                WHERE(StringUtils.join(tableAlias, fieldName, " not like CONCAT('%'," + value + ", '%')"));
                break;
            }
            case IS_NULL: {
                WHERE(StringUtils.join(tableAlias, fieldName, " is null "));
                break;
            }
            case IS_NOT_NULL: {
                WHERE(StringUtils.join(tableAlias, fieldName, " is not null "));
                break;
            }
        }
    }

    private String getInValuesSql(Map<String, OperatorHandle> operatorMap, String fieldName) {
        fieldName = StringUtils.replace(fieldName,".","_");
        List inValues = new ArrayList<>();
        for (int i = 0; i < operatorMap.get(fieldName).getValues().length; i++) {
            inValues.add(" #{" + BaseMapper.DATA + ".operatorHandles." + fieldName + ".values[" + i + "]}");
        }

        //add by limiao 20160811 处理拼in的时候，数组为空，不想查询的问题
        return inValues.isEmpty() ? " ( null ) " : StringUtils.join(" (", StringUtils.join(inValues, ","), ")");
    }



    /**
     * 验证该字段是否有需要拼装到sql中
     *
     * @param operatorMap 扩展运算集合
     * @param field       字段
     * @param param       数据集合
     * @return boolean值, 是否需要验证
     * @
     */
    private boolean validateField2WhereSql(Map<String, OperatorHandle> operatorMap, Field field, Map<String, Object>
            param) {
        if (null != field.getAnnotation(Transient.class) || field.getType().isAssignableFrom(Class.class)) {
            return false;
        }
        return param.containsKey(field.getName()) || operatorMap.containsKey(field.getName());
    }


    private String createInOrNotIn(Map<String, OperatorHandle> operatorMap, String fieldName) {
        List inValues = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(operatorMap.get(fieldName).getValues())) {
            for (int i = 0; i < operatorMap.get(fieldName).getValues().length; i++) {
                inValues.add(" #{" + BaseMapper.DATA + ".operatorHandles." + fieldName + ".values[" + i + "]}");
            }
        }

        //add by limiao 20160811 处理拼in的时候，数组为空，不想查询的问题
        return inValues.isEmpty() ? " ( null ) " : StringUtils.join(" (", StringUtils.join(inValues, ","), ")");

    }

    /*
     * 处理查询扩展类
     */
    private void parseQueryHandle(Map<String, Object> param, Boolean usePage, Boolean isCount) {
        if (param.containsKey("orderBy") && !isCount) {
            LinkedHashMap<String, String> orderByMap = (LinkedHashMap<String, String>) param.get("orderBy");
            for (String orderBy : orderByMap.keySet()) {
                if (orderBy.contains(".")) {
                    ORDER_BY(orderBy + " " + orderByMap.get(orderBy));
                    continue;
                }
                ORDER_BY(StringUtils.join("t.", orderBy, " ", orderByMap.get(orderBy)));
            }
        }
        if (param.containsKey("whereSqls")) {
            List<String> whereSqls = (List<String>) param.get("whereSqls");
            for (String whereSql : whereSqls) {
                WHERE(whereSql);
            }
        }
        createLimit(param, usePage);
        if (param.containsKey("sqlJoinHandle")) {
            sqlJoinHandles.addAll((List) param.get("sqlJoinHandle"));
        }
        for (SQLJoinHandle sqlJoinAssistor : sqlJoinHandles) {
            switch (sqlJoinAssistor.getJoinType()) {
                case JOIN: {
                    JOIN(sqlJoinAssistor.getJoinSql());
                    if (StringUtils.isNotEmpty(sqlJoinAssistor.getSelectColumns()) && !isCount) {
                        SELECT(sqlJoinAssistor.getSelectColumns());
                    }
                    break;
                }
                case INNER_JOIN: {
                    INNER_JOIN(sqlJoinAssistor.getJoinSql());
                    if (StringUtils.isNotEmpty(sqlJoinAssistor.getSelectColumns()) && !isCount) {
                        SELECT(sqlJoinAssistor.getSelectColumns());
                    }
                    break;
                }
                case LEFT_OUTER_JOIN: {
                    LEFT_OUTER_JOIN(sqlJoinAssistor.getJoinSql());
                    if (StringUtils.isNotEmpty(sqlJoinAssistor.getSelectColumns()) && !isCount) {
                        SELECT(sqlJoinAssistor.getSelectColumns());
                    }
                    break;
                }
                case RIGHT_OUTER_JOIN: {
                    RIGHT_OUTER_JOIN(sqlJoinAssistor.getJoinSql());
                    if (StringUtils.isNotEmpty(sqlJoinAssistor.getSelectColumns()) && !isCount) {
                        SELECT(sqlJoinAssistor.getSelectColumns());
                    }
                    break;
                }
            }
        }
        if (param.containsKey("groupBy")) {
            GROUP_BY((String) param.get("groupBy"));
        }
    }

    /**
     * 创建limit
     *
     * @param param   参数
     * @param usePage 是否使用分页
     */
    private void createLimit(Map<String, Object> param, Boolean usePage) {
        if (param.containsKey("page") && usePage) {
            limit = StringUtils.join(" limit ", ((Integer) param.get("page") - 1) * (Integer) param.get("rows"), " , ",
                    param.get("rows").toString());
        }
    }

    /**
     * 查询所有数据sql
     *
     * @param param 参数
     * @return sql
     */
    public String selectById(Map<String, Object> param) {
        beginWithClass(param);
        SELECT("*");
        FROM(getTableName(entityClass));
        WHERE(getEqualsValue(JPAUtils.getIdField(entityClass).getName(), BaseMapper.ID));
        return sql();
    }

    /**
     * 获取比较值字符串
     *
     * @param column 列名
     * @param value  值
     * @return 拼接后的字符串
     */
    private String getEqualsValue(String column, String value) {
        return StringUtils.join(column, " = #{", value, "}");
    }

    /*
     * 生成sql开始
     */
    protected void beginWithClass(Map<String, Object> param) {
        if (null == param.get(BaseMapper.ENTITY_CLASS)) {
            throw new DaoException(StringUtils.join("获取实体类型失败 entityClass 为空"));
        }
        begin();
        entityClass = (Class) param.get(BaseMapper.ENTITY_CLASS);
    }


    /**
     * 保存sql
     *
     * @param param 参数
     * @return sql
     * @{@inheritDoc}
     */
    public String save(Map<String, Object> param) {
        entityClass = param.get(BaseMapper.ENTITY).getClass();
        begin();
        String tableName = getTableName(entityClass);
        if (param.containsKey(BaseMapper.TABLE_NAME) && param.get(BaseMapper.TABLE_NAME) != null) {
            tableName = param.get(BaseMapper.TABLE_NAME).toString().trim();
        }
        INSERT_INTO(tableName);
        Field idField = null;
        for (Field field : JPAUtils.getAllFields(entityClass)) {
            if (!ClassUtils.isPrimitiveOrWrapper(field.getClass()) && !Enum.class.isAssignableFrom(LogSystemType.class)) {
                continue;
            }
            field.setAccessible(true);
            //处理主键
            if (null != field.getAnnotation(Id.class) || null != field.getAnnotation(Transient.class)) {
                idField = field;
                continue;
            }
            //add by limiao 20160309 insert 为null的不拼sql
            Object value = JPAUtils.getValue(param.get(BaseMapper.ENTITY), field.getName());
            if (null == value) {
                continue;
            }

            VALUES(field.getName(), StringUtils.join("#{", BaseMapper.ENTITY, ".", field.getName(), "}"));
        }
        if (null == idField) {
            throw new DaoException(StringUtils.join(entityClass.getName(), "实体未配置@Id "));
        }

        setId(param, idField);
        return sql();
    }

    /**
     * 保存sql
     *
     * @param param 参数
     * @return sql
     * @{@inheritDoc}
     */
    public String saveBatch(Map<String, Object> param) {
        if (null == param.get(BaseMapper.ENTITIES)) {
            throw new DaoException("批量插入的数据为null");
        }
        entityClass = ((List) param.get(BaseMapper.ENTITIES)).get(0).getClass();
        begin();
        BATCH_INSERT_INTO(getTableName(entityClass));

        Field idField = null;
        for (int i = 0; i < ((List) param.get(BaseMapper.ENTITIES)).size(); i++) {
            for (Field field : JPAUtils.getAllFields(entityClass)) {

                if (!ClassUtils.isPrimitiveOrWrapper(field.getClass()) && !Enum.class.isAssignableFrom(LogSystemType.class)) {
                    continue;
                }
                field.setAccessible(true);
                //处理主键
                if (null != field.getAnnotation(Id.class) || null != field.getAnnotation(Transient.class)) {
                    idField = field;
                    continue;
                }
                //add by limiao 20160309 insert 为null的不拼sql
                Object value = JPAUtils.getValue(((List) param.get(BaseMapper.ENTITIES)).get(i), field.getName());
                if (null == value) {
                    continue;
                }

                BATCH_VALUES(field.getName(), StringUtils.join("#{", BaseMapper.ENTITIES, "[", i, "]", ".", field.getName(), "}"));
            }
            if (null == idField) {
                throw new DaoException(StringUtils.join(entityClass.getName(), "实体未配置@Id "));
            }
            setIdBatch((BaseEntity) ((List) param.get(BaseMapper.ENTITIES)).get(i), i, idField);
            BATCH_SEGMENTATION();
        }
        return sql();
    }


    public String saveForSelective(Map<String, Object> param) {
        final Object entity = param.get(BaseMapper.ENTITY);
        entityClass = entity.getClass();
        begin();
        INSERT_INTO(getTableName(entityClass));

        Field idField = null;
        for (Field field : JPAUtils.getAllFields(entityClass)) {
            field.setAccessible(true);
            //处理主键
            if (null != field.getAnnotation(Id.class) || null != field.getAnnotation(Transient.class)) {
                idField = field;
                continue;
            }
            try {
                if (field.get(entity) != null) {
                    VALUES(field.getName(), StringUtils.join("#{", BaseMapper.ENTITY, ".", field.getName(), "}"));
                }
            } catch (IllegalAccessException e) {
                throw new UtilException(e);
            }
        }
        if (null == idField) {
            throw new DaoException(StringUtils.join(entityClass.getName(), "实体未配置@Id "));
        }
        setId(param, idField);
        return sql();
    }


    private void setId(Map<String, Object> param, Field idField) {
        if (!idField.getType().isAssignableFrom(String.class)) {
            return;
        }
        try {
            if (StringUtils.isNotEmpty((String) idField.get(param.get(BaseMapper.ENTITY)))) {
                VALUES(idField.getName(), StringUtils.join("#{", BaseMapper.ENTITY, ".", idField.getName(), "}"));
                return;
            }

            String id = UUID.randomUUID().toString().replace("-", "");
            VALUES(idField.getName(), StringUtils.join("'", id, "'"));
            idField.set(param.get(BaseMapper.ENTITY), id);
        } catch (IllegalAccessException e) {
            throw new UtilException(e);
        }
    }

    private void setIdBatch(BaseEntity baseEntity, int i, Field idField) {
        if (!idField.getType().isAssignableFrom(String.class)) {
            return;
        }
        try {
            if (StringUtils.isNotEmpty((String) idField.get(baseEntity))) {
                BATCH_VALUES(idField.getName(), StringUtils.join("#{", BaseMapper.ENTITIES, "[", i, "]", ".", idField.getName(), "}"));
                return;
            }

            String id = UUID.randomUUID().toString().replace("-", "");
            BATCH_VALUES(idField.getName(), StringUtils.join("'", id, "'"));
            idField.set(baseEntity, id);
        } catch (IllegalAccessException e) {
            throw new UtilException(e);
        }
    }

    public String update(Map<String, Object> param) {
        entityClass = param.get(BaseMapper.ENTITY).getClass();
        begin();
        UPDATE(getTableName(entityClass));
        Field idField = null;
        for (Field field : JPAUtils.getAllFields(entityClass)) {
            field.setAccessible(true);
            //非基础类型不处理
            if (field.getType().isAssignableFrom(Collection.class) || null != field.getType().getAnnotation(Table.class)) {
                continue;
            }
            //处理nullable
            Annotation columnAnnotation = field.getAnnotation(Column.class);
            if (null != columnAnnotation && ((Column) columnAnnotation).nullable() == false
                    && null == JPAUtils.getValue(param.get(BaseMapper.ENTITY), field.getName())) {
                continue;
            }
            //处理非主键
            if (null == field.getAnnotation(Id.class)) {
                SET(StringUtils.join(getEqualsValue(field.getName(), StringUtils.join(BaseMapper.ENTITY, ".", field.getName()))));
                continue;
            }

            idField = field;
            WHERE(getEqualsValue(field.getName(), StringUtils.join(BaseMapper.ENTITY, ".", field.getName())));
        }
        if (null == idField) {
            throw new DaoException(StringUtils.join(entityClass.getName(), "实体未配置@Id "));
        }


        return sql();

    }

    /**
     * 获取表名
     *
     * @param entityClass 实体类
     * @return sql
     * @{@inheritDoc}
     */
    private String getTableName(Class entityClass) {
        Annotation table = entityClass.getAnnotation(Table.class);
        if (null == table) {
            throw new DaoException(StringUtils.join("实体未配置Table注解 entityClass =", entityClass.getName()));
        }
        String tableName = ((Table) table).name();
        if (StringUtils.isEmpty(tableName)) {
            throw new DaoException(StringUtils.join("实体的Table注解未配置name属性 entityClass =", entityClass.getName()));
        }
        return tableName;
    }

    private String getTableNameWithAlias(Class entityClass) {
        return StringUtils.join(getTableName(entityClass), " t");
    }

    public String pageData(Map<String, Object> param) {
        beginWithClass(param);
        //处理查询列问题 add by limiao 20170508
        Map<String, Object> map = ((Map<String, Object>) param.get(BaseMapper.DATA));
        String selectColumnSql = createSelectColumnSql(map);
        if (MapUtils.isNotEmpty(map) && map.containsKey("distinct")) {
            SELECT_DISTINCT(selectColumnSql);
        } else {
            SELECT(selectColumnSql);
        }
        FROM(getTableNameWithAlias(entityClass));
        createAllWhere(map, true);
        return StringUtils.join(sql(), limit);
    }

    public String pageTotalRecord(Map<String, Object> param) {
        beginWithClass(param);
        SELECT(createSelectCountColumnSql(param));
        FROM(getTableNameWithAlias(entityClass));
        createAllWhere((Map<String, Object>) param.get(BaseMapper.DATA), false, true);
        return sql();
    }

    public String findListBySql(Map<String, Object> param) {
        createLimit((Map<String, Object>) param.get(BaseMapper.DATA), true);
        return StringUtils.join(param.get(BaseMapper.CUSTOM_SQL), limit);
    }

    public String findListTotalRecordBySql(Map<String, Object> param) {
        return StringUtils.join("select count(*) from (", param.get(BaseMapper.CUSTOM_SQL), ") as countTable");
    }

    public String findUniqueObjectBySql(Map<String, Object> param) {
        return StringUtils.join(param.get(BaseMapper.CUSTOM_SQL), " limit 1");
    }

    public String delete(Map<String, Object> param) {
        beginWithClass(param);
        DELETE_FROM(getTableName(entityClass));
        WHERE(getEqualsValue(JPAUtils.getIdField(entityClass).getName(), BaseMapper.ID));

        return sql();
    }

    public String logicDelete(Map<String, Object> param) {
        entityClass = (Class<?>) param.get(BaseMapper.ENTITY_CLASS);
        begin();
        UPDATE(getTableName(entityClass));
        param.put(BaseEntity.DELETE_FLAG, true);
        SET(getEqualsValue(BaseEntity.DELETE_FLAG, BaseEntity.DELETE_FLAG));
        Field idField = JPAUtils.getIdField(entityClass);
        WHERE(getEqualsValue(idField.getName(), BaseMapper.ID));

        return sql();
    }

    public String logicDeleteByCondition(Map<String, Object> param) {
        beginWithClass(param);
        createAllWhere((Map<String, Object>) param.get(BaseMapper.DATA), false);
        UPDATE(StringUtils.join(getTableName(entityClass), " t"));
        param.put(BaseEntity.DELETE_FLAG, true);
        SET(getEqualsValue(BaseEntity.DELETE_FLAG, BaseEntity.DELETE_FLAG));
        return sql();
    }

    public String updateForSelective(Map<String, Object> param) {
        entityClass = param.get(BaseMapper.ENTITY).getClass();
        begin();
        UPDATE(getTableName(entityClass));
        Field idField = null;
        for (Field field : JPAUtils.getAllFields(entityClass)) {
            field.setAccessible(true);
            //非基础类型不处理
            if (field.getType().isAssignableFrom(Collection.class) || null != field.getType().getAnnotation(Table.class)) {
                continue;
            }
            //处理主键
            if (null != field.getAnnotation(Id.class)) {
                idField = field;
                continue;
            }
            Object value = JPAUtils.getValue(param.get(BaseMapper.ENTITY), field.getName());
            if (null == value) {
                continue;
            }
            //空字符串对应修改时，情况输入框
//                if(StringUtils.EMPTY.equals(value)){
//                    continue;
//                }

            SET(StringUtils.join(getEqualsValue(field.getName(), StringUtils.join(BaseMapper.ENTITY, ".", field.getName()))));
        }
        if (null == idField) {
            throw new DaoException(StringUtils.join(entityClass.getName(), "实体未配置@Id "));
        }
        WHERE(getEqualsValue(idField.getName(), StringUtils.join(BaseMapper.ENTITY, ".", idField.getName())));
        return sql();
    }

    public String deleteByCondition(Map<String, Object> param) {
        beginWithClass(param);
        createAllWhere((Map<String, Object>) param.get(BaseMapper.DATA), false);
        DELETE_FROM(StringUtils.join(getTableName(entityClass), " t"));
        return sql().replaceFirst("DELETE FROM", "DELETE t FROM");
    }

    public String updateBySql(Map<String, Object> param) {
        beginWithClass(param);
        UPDATE(getTableNameWithAlias(entityClass));
        SET(param.get(BaseMapper.CUSTOM_SQL).toString());
        createAllWhere((Map<String, Object>) param.get(BaseMapper.DATA), false);
        return SQL();
    }

    public String updateByCondition(Map<String, Object> param) {
        beginWithClass(param);
        UPDATE(getTableNameWithAlias(entityClass));
        SET("name = #{name}");
        createAllWhere((Map<String, Object>) param.get(BaseMapper.DATA), false);
        return StringUtils.join(sql(), limit);
    }

    //处理查询列问题 add by limiao 20170508
    protected String createSelectColumnSql(Map<String, Object> map) {
        String selectColumnSql = " t.* ";
        if (MapUtils.isNotEmpty(map) && map.containsKey("selectColumnSql") && map.get("selectColumnSql") != null) {
            selectColumnSql = " " + map.get("selectColumnSql").toString() + " ";
        }
        return selectColumnSql;
    }

    protected String createSelectCountColumnSql(Map<String, Object> param) {
        Map<String, Object> map = ((Map<String, Object>) param.get(BaseMapper.DATA));
        String selectColumnSql = "count(distinct t." + JPAUtils.getIdField((Class<?>) param.get(BaseMapper.ENTITY_CLASS)).getName() + ")";
        if (map.containsKey("selectColumnSql") && map.get("selectColumnSql") != null) {
            selectColumnSql = " count(distinct " + map.get("selectColumnSql").toString() + ") ";
        }
        return selectColumnSql;
    }


}
