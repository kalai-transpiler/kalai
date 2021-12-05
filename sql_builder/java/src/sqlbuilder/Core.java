package sqlbuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Core {
public static final String castToStr(final Object x) {
if ((x instanceof List))
{
final ArrayList<Object> v = (ArrayList)x;
final Object vFirst = v.get(0);
final String tableName = (String)vFirst;
final Object vSecond = v.get(1);
final String tableAlias = (String)vSecond;
return ("" + tableName + " AS " + tableAlias);
}
else
{
if ((x instanceof String))
{
return ("" + (String)x);
}
else
{
if ((x instanceof Integer))
{
return ("" + (int)x);
}
else
{
if ((x instanceof Long))
{
return ("" + (long)x);
}
else
{
return "";
}
}
}
}
}
public static final String selectStr(final ArrayList<Object> select) {
return String.join(", ", select.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String fromStr(final ArrayList<Object> from) {
return String.join(", ", from.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String joinStr(final ArrayList<Object> join) {
return String.join(", ", join.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String whereStr(final Object clause) {
if ((clause instanceof List))
{
final ArrayList<Object> v = (ArrayList)clause;
final Object vFirst = v.stream().findFirst().get();
final String op = (String)vFirst;
return ("" + "(" + String.join(("" + " " + op + " "), v.stream().skip(1L).map(sqlbuilder.Core::whereStr).collect(Collectors.toList())) + ")");
}
else
{
return sqlbuilder.Core.castToStr(clause);
}
}
public static final String groupByStr(final ArrayList<Object> join) {
return String.join(", ", join.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList()));
}
public static final String havingStr(final Object having) {
return sqlbuilder.Core.whereStr(having);
}
public static final String rowStr(final Object row) {
final ArrayList<Object> mrow = (ArrayList)row;
return ("" + "(" + String.join(", ", mrow.stream().map(sqlbuilder.Core::castToStr).collect(Collectors.toList())) + ")");
}
public static final String format(final HashMap<String,Object> queryMap) {
final Object select = queryMap.getOrDefault(":select", null);
final Object from = queryMap.getOrDefault(":from", null);
final Object join = queryMap.getOrDefault(":join", null);
final Object whereClause = queryMap.getOrDefault(":where", null);
final Object groupBy = queryMap.getOrDefault(":group-by", null);
final Object having = queryMap.getOrDefault(":having", null);
final Object insertInto = queryMap.getOrDefault(":insert-into", null);
final Object columns = queryMap.getOrDefault(":columns", null);
final Object values = queryMap.getOrDefault(":values", null);
String tmp1;
if ((insertInto == null))
{
tmp1 = "";
}
else
{
final ArrayList<Object> v2 = (ArrayList)values;
{
tmp1 = ("" + "INSERT INTO " + sqlbuilder.Core.fromStr((ArrayList)insertInto) + "(" + sqlbuilder.Core.selectStr((ArrayList)columns) + ")\n" + "VALUES\n" + String.join(",\n", v2.stream().map(sqlbuilder.Core::rowStr).collect(Collectors.toList())));
}
}
String tmp2;
if ((select == null))
{
tmp2 = "";
}
else
{
tmp2 = ("" + "SELECT " + sqlbuilder.Core.selectStr((ArrayList)select));
}
String tmp3;
if ((from == null))
{
tmp3 = "";
}
else
{
tmp3 = ("" + " FROM " + sqlbuilder.Core.fromStr((ArrayList)from));
}
String tmp4;
if ((join == null))
{
tmp4 = "";
}
else
{
tmp4 = ("" + " JOIN " + sqlbuilder.Core.joinStr((ArrayList)join));
}
String tmp5;
if ((whereClause == null))
{
tmp5 = "";
}
else
{
tmp5 = ("" + " WHERE " + sqlbuilder.Core.whereStr(whereClause));
}
String tmp6;
if ((groupBy == null))
{
tmp6 = "";
}
else
{
tmp6 = ("" + " GROUP BY " + sqlbuilder.Core.groupByStr((ArrayList)groupBy));
}
String tmp7;
if ((having == null))
{
tmp7 = "";
}
else
{
tmp7 = ("" + " HAVING " + sqlbuilder.Core.havingStr(having));
}
return ("" + tmp1 + tmp2 + tmp3 + tmp4 + tmp5 + tmp6 + tmp7);
}
}
