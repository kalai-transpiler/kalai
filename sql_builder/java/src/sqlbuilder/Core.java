package sqlbuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class Core {
public static final TYPE_MISSING selectStr(final TYPE_MISSING select) {
return clojure.String.join(", ", select);
}
public static final TYPE_MISSING fromStr(final TYPE_MISSING from) {
return clojure.String.join(", ", from);
}
public static final TYPE_MISSING joinStr(final TYPE_MISSING join) {
return clojure.String.join(", ", join);
}
public static final TYPE_MISSING whereStr(final TYPE_MISSING join) {
if (clojure.Core.vector?(join))
{
final Object op = clojure.Core.first(join);
final Object more = clojure.Core.rest(join);
return clojure.Core.str("(", clojure.String.join(clojure.Core.interpose(clojure.Core.str(" ", op, " "), clojure.Core.map(whereStr, more))), ")");
}
else
{
return join;
}
}
public static final TYPE_MISSING groupByStr(final TYPE_MISSING join) {
return clojure.String.join(", ", join);
}
public static final TYPE_MISSING havingStr(final TYPE_MISSING having) {
return sqlbuilder.Core.whereStr(having);
}
public static final String format(final HashMap<String,ArrayList<Object>> queryMap) {
final TYPE_MISSING select = queryMap.get(":select");
final TYPE_MISSING from = queryMap.get(":from");
final TYPE_MISSING join = queryMap.get(":join");
final TYPE_MISSING where = queryMap.get(":where");
final TYPE_MISSING groupBy = queryMap.get(":group-by");
final TYPE_MISSING having = queryMap.get(":having");
"MISSING_TYPE" tmp1;
if (select)
{
tmp1 = clojure.Core.str("SELECT ", sqlbuilder.Core.selectStr(select));
}
"MISSING_TYPE" tmp2;
if (from)
{
tmp2 = clojure.Core.str(" FROM ", sqlbuilder.Core.fromStr(from));
}
"MISSING_TYPE" tmp3;
if (join)
{
tmp3 = clojure.Core.str(" JOIN ", sqlbuilder.Core.joinStr(join));
}
"MISSING_TYPE" tmp4;
if (where)
{
tmp4 = clojure.Core.str(" WHERE ", sqlbuilder.Core.whereStr(where));
}
"MISSING_TYPE" tmp5;
if (groupBy)
{
tmp5 = clojure.Core.str(" GROUP BY ", sqlbuilder.Core.groupByStr(groupBy));
}
"MISSING_TYPE" tmp6;
if (having)
{
tmp6 = clojure.Core.str(" HAVING ", sqlbuilder.Core.havingStr(having));
}
return clojure.Core.str(tmp1, tmp2, tmp3, tmp4, tmp5, tmp6);
}
}
