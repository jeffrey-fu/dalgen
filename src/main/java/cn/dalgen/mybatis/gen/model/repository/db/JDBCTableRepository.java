package cn.dalgen.mybatis.gen.model.repository.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cn.dalgen.mybatis.gen.enums.TypeMapEnum;
import cn.dalgen.mybatis.gen.model.config.CfColumn;
import cn.dalgen.mybatis.gen.model.config.CfTable;
import cn.dalgen.mybatis.gen.model.dbtable.Column;
import cn.dalgen.mybatis.gen.model.dbtable.NormalIndex;
import cn.dalgen.mybatis.gen.model.dbtable.PrimaryKeys;
import cn.dalgen.mybatis.gen.model.dbtable.Table;
import cn.dalgen.mybatis.gen.model.dbtable.UniqueIndex;
import cn.dalgen.mybatis.gen.model.repository.db.database.DataBaseInfoHandler;
import cn.dalgen.mybatis.gen.utils.CamelCaseUtils;
import cn.dalgen.mybatis.gen.utils.ConfigUtil;
import cn.dalgen.mybatis.gen.utils.ResultSetUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ibatis.type.JdbcType;

/**
 * Created by bangis.wangdf on 15/12/5. Desc
 */
public class JDBCTableRepository {

    private static Map<String, String> tableRemarkMap = Maps.newConcurrentMap();

    /**
     * Gain table table.
     *
     * @param connection the connection
     * @param tableName  the table name
     * @param cfTable    the cf table
     * @return the table
     * @throws SQLException the sql exception
     */
    public Table gainTable(Connection connection, String tableName, CfTable cfTable)
        throws SQLException {
        String physicalName = cfTable == null ? tableName : cfTable.getPhysicalName();
        String logicName = tableName;
        for (String splitTableSuffix : ConfigUtil.getConfig().getSplitTableSuffixs()) {
            if (StringUtils.endsWithIgnoreCase(tableName, splitTableSuffix)) {
                logicName = StringUtils.replace(logicName, splitTableSuffix, "");
                break;
            }
        }

        List<CfColumn> cfColumns = cfTable == null ? null : cfTable.getColumns();

        //??????table
        Table table = new Table();
        table.setSqlName(logicName);

        String _pre = "";
        for (String pre : ConfigUtil.getCurrentDb().getTablePrefixs().keySet()) {
            //????????????
            if (StringUtils.startsWith(logicName, StringUtils.upperCase(pre)) && pre.length() > _pre.length()) {
                _pre = pre;

            }
        }

        if (StringUtils.isNotBlank(_pre)) {
            //?????? or ??????
            String toTableName = ConfigUtil.getCurrentDb().getTablePrefixs().get(_pre)
                + StringUtils.substring(logicName, _pre.length());
            table.setJavaName(CamelCaseUtils.toCapitalizeCamelCase(toTableName));
        }

        if (StringUtils.isBlank(table.getJavaName())) {
            table.setJavaName(CamelCaseUtils.toCapitalizeCamelCase(logicName));
        }
        table.setPhysicalName(physicalName);

        String tableRemark = queryTableRemark(connection, tableName, logicName);

        table.setRemark(tableRemark);

        //????????????
        fillColumns(physicalName, connection, table, cfColumns, cfTable);

        //??????,????????????,??????
        fillPrimaryUniqueIndexKeys(connection, physicalName, table);

        //?????????????????????
        if (ConfigUtil.getConfig().getDeleteColumn() != null) {
            for (Column column : table.getColumnList()) {
                if (StringUtils.equalsIgnoreCase(column.getSqlName(), ConfigUtil.getConfig()
                    .getDeleteColumn().getName())) {
                    table.setNeadSoftDelete(true);
                    break;
                }
            }
        }

        return table;
    }

    /**
     * ?????????????????????
     *
     * @param connection
     * @param tableName
     * @param logicName
     * @return
     * @throws SQLException
     */
    private String queryTableRemark(Connection connection, String tableName, String logicName)
        throws SQLException {
        if (MapUtils.isEmpty(tableRemarkMap)) {
            tableRemarkMap = DataBaseInfoHandler.getDataBaseInfoService().getAllTableRemark(
                connection);
        }
        final String remark = tableRemarkMap.get(StringUtils.upperCase(tableName));
        return StringUtils.isBlank(remark) ? logicName : remark;
    }

    /**
     * ??????,????????????,??????. ??????????????????,??????????????????????????? ???????????????????????????
     *
     * @param connection the connection
     * @param tableName  the table name
     * @param table      the table
     * @throws SQLException the sql exception
     */
    private void fillPrimaryUniqueIndexKeys(Connection connection, String tableName, Table table)
        throws SQLException {

        //???????????????
        List<String> pkNameList = Lists.newArrayList();

        //?????????????????????
        List<String> uniqueNameList = Lists.newArrayList();

        fillPrimaryKeys(connection, tableName, table, pkNameList);

        //???????????? ???????????? select update

        fillUniqueIndex(connection, tableName, table, pkNameList, uniqueNameList);

        //???????????? ???????????? select
        fillNormalIndex(connection, tableName, table, pkNameList, uniqueNameList);
    }

    /**
     * ???????????? ???????????? select
     *
     * @param connection
     * @param tableName
     * @param table
     * @param pkNameList
     * @param uniqueNameList
     * @throws SQLException
     */
    private void fillNormalIndex(Connection connection, String tableName, Table table,
                                 List<String> pkNameList, List<String> uniqueNameList)
        throws SQLException {

        List<String> indexNameList = Lists.newArrayList();
        Map<String, NormalIndex> normalIndexMap = Maps.newHashMap();

        Map<String, String> dbNormalIndexMap = DataBaseInfoHandler.getDataBaseInfoService().getNormalIndexs(connection,
            tableName);
        if (MapUtils.isNotEmpty(dbNormalIndexMap)) {
            for (Entry<String, String> idxEntry : dbNormalIndexMap.entrySet()) {
                String indexName = idxEntry.getValue();
                if (pkNameList.contains(indexName) || uniqueNameList.contains(indexName)) {
                    continue;
                }
                if (!indexNameList.contains(indexName)) {
                    indexNameList.add(indexName);
                }
                NormalIndex normalIndex = normalIndexMap.get(indexName);
                if (normalIndex == null) {
                    normalIndex = new NormalIndex();
                    normalIndexMap.put(indexName, normalIndex);
                    table.addNormalIndex(normalIndex);
                }
                normalIndex.setIdxName(indexName);
                normalIndex.addColumn(table.getColumnByName(idxEntry.getKey()));
            }
        }
    }

    /**
     * ???????????? ???????????? select update
     *
     * @param connection
     * @param tableName
     * @param table
     * @param pkNameList
     * @param uniqueNameList
     * @throws SQLException
     */
    private void fillUniqueIndex(Connection connection, String tableName, Table table,
                                 List<String> pkNameList, List<String> uniqueNameList)
        throws SQLException {

        Map<String, String> dbUniqueIndexMap = DataBaseInfoHandler.getDataBaseInfoService().getUniqueIndexs(connection,
            tableName);

        Map<String, UniqueIndex> uniqueIndexMap = Maps.newHashMap();
        if (MapUtils.isNotEmpty(dbUniqueIndexMap)) {
            for (Entry<String, String> ukEntry : dbUniqueIndexMap.entrySet()) {
                String uniqueName = ukEntry.getValue();
                if (pkNameList.contains(uniqueName)) {
                    continue;
                }
                if (!uniqueNameList.contains(uniqueName)) {
                    uniqueNameList.add(uniqueName);
                }
                //??????????????????
                UniqueIndex uniqueIndex = uniqueIndexMap.get(uniqueName);
                if (uniqueIndex == null) {
                    uniqueIndex = new UniqueIndex();
                    uniqueIndexMap.put(uniqueName, uniqueIndex);
                    table.addUniqueIndex(uniqueIndex);
                }
                uniqueIndex.setUkName(uniqueName);
                uniqueIndex.addColumn(table.getColumnByName(ukEntry.getKey()));
            }
        }
    }

    /**
     * ????????????
     *
     * @param connection
     * @param tableName
     * @param table
     * @param pkNameList
     * @throws SQLException
     */
    private void fillPrimaryKeys(Connection connection, String tableName, Table table,
                                 List<String> pkNameList) throws SQLException {
        //??????????????????
        Map<String, String> dbPrimaryKeyMaps = DataBaseInfoHandler.getDataBaseInfoService().getPrimaryKeys(connection,
            tableName);
        if (MapUtils.isNotEmpty(dbPrimaryKeyMaps)) {
            for (Entry<String, String> pkEntry : dbPrimaryKeyMaps.entrySet()) {
                if (!pkNameList.contains(pkEntry.getValue())) {
                    pkNameList.add(pkEntry.getValue());
                }
            }
            //???????????????????????????
            if (CollectionUtils.isNotEmpty(pkNameList)) {
                PrimaryKeys primaryKeys = new PrimaryKeys();
                String pkName = "";
                int pkCnt = dbPrimaryKeyMaps.keySet().size();
                for (String key : dbPrimaryKeyMaps.keySet()) {
                    if (pkCnt == 1) {
                        pkName = CamelCaseUtils.toCapitalizeCamelCase(key);
                    } else {
                        pkName = dbPrimaryKeyMaps.get(key);
                    }
                    primaryKeys.addColumn(table.getColumnByName(key));
                }
                primaryKeys.setPkName(pkName);
                table.setPrimaryKeys(primaryKeys);
            }
        }
    }

    /**
     * Fill columns.
     *
     * @param tableName  the table name
     * @param connection the connection meta data
     * @param table      the table
     * @param cfColumns  the cf columns
     * @throws SQLException the sql exception
     */
    private void fillColumns(String tableName, Connection connection, Table table,
                             List<CfColumn> cfColumns, CfTable cfTable) throws SQLException {

        List<Map<String, String>> dbColumnMaps = DataBaseInfoHandler.getDataBaseInfoService()
            .getAllColumnsByTableName(connection,
                tableName);
        //???????????????

        //????????????
        if (CollectionUtils.isNotEmpty(dbColumnMaps)) {

            for (Map<String, String> dbColumnMap : dbColumnMaps) {
                Long ordinalPosition = Long.valueOf(dbColumnMap.get("ORDINAL_POSITION"));
                SimpleDateFormat formate = new SimpleDateFormat("yyyy-MM-dd");
                formate.format(new Date());

                if (cfTable != null && cfTable.getOrdinalEffectiveDate() != null
                    && cfTable.getOrdinalEffectiveDate().getTime() < System.currentTimeMillis()) {
                    if (cfTable.getOrdinalMaxPosition() < ordinalPosition) {
                        break;
                    }
                }
                Column column = new Column();
                column.setSqlName(dbColumnMap.get("COLUMN_NAME"));
                column.setSqlType(TypeMapEnum.getByJdbcTypeWithOther(dbColumnMap.get("DATA_TYPE")).getJdbcType());
                column.setMySqlType(dbColumnMap.get("DATA_TYPE"));
                column.setDefaultValue(dbColumnMap.get("COLUMN_DEFAULT"));
                column.setJavaName(CamelCaseUtils.toCamelCase(column.getSqlName()));
                column.setTestVal(getTestVal(column, cfColumns));
                column.setTypeHandler(getTypeHandler(column, cfColumns));
                column.setRemarks(dbColumnMap.get("COLUMN_COMMENT"));
                column.setOrdinalPosition(ordinalPosition);
                column.setLength(dbColumnMap.get("C_LENGTH"));
                column.setPrecision(dbColumnMap.get("C_PRECISION"));
                column.setScale(dbColumnMap.get("C_SCALE"));
                column.setJavaType(getJavaType(column, cfColumns));
                table.addColumn(column);
            }
        } else {
            System.out.println(tableName + " fillColumns error");
        }
    }

    private String getTypeHandler(Column column, List<CfColumn> cfColumns) {
        if (cfColumns != null && cfColumns.size() > 0) {
            for (CfColumn cfColumn : cfColumns) {
                if (StringUtils.equalsIgnoreCase(column.getSqlName(), cfColumn.getName())) {
                    return cfColumn.getTypeHandler();
                }
            }
        }
        return null;
    }

    private String getTestVal(Column column, List<CfColumn> cfColumns) {
        if (cfColumns != null && cfColumns.size() > 0) {
            for (CfColumn cfColumn : cfColumns) {
                if (StringUtils.equalsIgnoreCase(column.getSqlName(), cfColumn.getName())) {
                    return cfColumn.getTestVal();
                }
            }
        }
        return null;
    }

    /**
     * Gets java type.
     *
     * @param column    the column
     * @param cfColumns the cf columns
     * @return the java type
     */
    private String getJavaType(Column column, List<CfColumn> cfColumns) {
        if (cfColumns != null && cfColumns.size() > 0) {
            for (CfColumn cfColumn : cfColumns) {
                if (StringUtils.equalsIgnoreCase(column.getSqlName(), cfColumn.getName())) {
                    return cfColumn.getJavatype();
                }
            }
        }
        if (StringUtils.equalsIgnoreCase(ConfigUtil.getCurrentDb().getType(), "oracle")) {
            if (StringUtils.equalsIgnoreCase(column.getMySqlType(), "CHAR") && StringUtils.equalsIgnoreCase("1",
                column.getLength())) {
                return "Boolean";
            } else if (StringUtils.equalsIgnoreCase(column.getMySqlType(), "NUMBER") && StringUtils.isNotBlank(
                column.getScale()) && !StringUtils.equals(column.getScale(), "0")) {
                return "java.math.BigDecimal";
            }

        }
        String javaType = TypeMapEnum.getByJdbcTypeWithOther(column.getMySqlType()).getJavaType();
        String custJavaType = ConfigUtil.getConfig().getTypeMap().get(javaType);
        return StringUtils.isBlank(custJavaType) ? javaType : custJavaType;
    }

}
