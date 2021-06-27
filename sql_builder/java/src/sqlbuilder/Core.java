package sqlbuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Core {
public static final String castToStr(final Object x) {
return x;
}
public static final String selectStr(final ArrayList<Object> select) {
return clojure.String.join(", ", clojure.Core.map(castToStr, select));
}
public static final String fromStr(final ArrayList<Object> from) {
return clojure.String.join(", ", clojure.Core.map(castToStr, from));
}
public static final String joinStr(final ArrayList<Object> join) {
return clojure.String.join(", ", clojure.Core.map(castToStr, join));
}
public static final String whereStr(final Object join) {
if (clojure.Core.vector?(join))
{
final ArrayList<Object> jj = join;
return clojure.Core.str("(", clojure.String.join(clojure.Core.str(" op "), clojure.Core.map(whereStr, jj)), ")");
}
else
{
return join;
}
}
public static final String groupByStr(final ArrayList<Object> join) {
return clojure.String.join(", ", clojure.Core.map(castToStr, join));
}
public static final String havingStr(final Object having) {
return sqlbuilder.Core.whereStr(having);
}
public static final String format(final HashMap<String,ArrayList<Object>> queryMap) {
final ArrayList<Object> select = queryMap.get(":select");
final ArrayList<Object> from = queryMap.get(":from");
final ArrayList<Object> join = queryMap.get(":join");
final ArrayList<Object> whereClause = queryMap.get(":where");
final ArrayList<Object> groupBy = queryMap.get(":group-by");
final ArrayList<Object> having = queryMap.get(":having");
"MISSING_TYPE" tmp1;
if (select)
{
tmp1 = clojure.Core.str("SELECT ", sqlbuilder.Core.selectStr(select));
}
else
{
tmp1 = "";
}
"MISSING_TYPE" tmp2;
if (from)
{
tmp2 = clojure.Core.str(" FROM ", sqlbuilder.Core.fromStr(from));
}
else
{
tmp2 = "";
}
"MISSING_TYPE" tmp3;
if (join)
{
tmp3 = clojure.Core.str(" JOIN ", sqlbuilder.Core.joinStr(join));
}
else
{
tmp3 = "";
}
"MISSING_TYPE" tmp4;
if (whereClause)
{
tmp4 = clojure.Core.str(" WHERE ", sqlbuilder.Core.whereStr(whereClause));
}
else
{
tmp4 = "";
}
"MISSING_TYPE" tmp5;
if (groupBy)
{
tmp5 = clojure.Core.str(" GROUP BY ", sqlbuilder.Core.groupByStr(groupBy));
}
else
{
tmp5 = "";
}
"MISSING_TYPE" tmp6;
if (having)
{
tmp6 = clojure.Core.str(" HAVING ", sqlbuilder.Core.havingStr(having));
}
else
{
tmp6 = "";
}
return clojure.Core.str(tmp1, tmp2, tmp3, tmp4, tmp5, tmp6);
}
}
