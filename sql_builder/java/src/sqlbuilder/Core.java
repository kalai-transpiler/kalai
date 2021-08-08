package sqlbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Core {
  public static final String castToStr(final Object x) {
    return (String) x;
  }

  public static final String selectStr(final ArrayList<Object> select) {
    return String.join(
        ", ",
        clojure.Core.seq(select).stream()
            .map(sqlbuilder.Core::castToStr)
            .collect(Collectors.toList()));
  }

  public static final String fromStr(final ArrayList<Object> from) {
    return String.join(
        ", ",
        clojure.Core.seq(from).stream()
            .map(sqlbuilder.Core::castToStr)
            .collect(Collectors.toList()));
  }

  public static final String joinStr(final ArrayList<Object> join) {
    return String.join(
        ", ",
        clojure.Core.seq(join).stream()
            .map(sqlbuilder.Core::castToStr)
            .collect(Collectors.toList()));
  }

  public static final String whereStr(final Object clause) {
    if ((clause instanceof List)) {
      final ArrayList<Object> v = (ArrayList) clause;
      final Object vFirst = clojure.Core.first(clojure.Core.seq(v));
      final String op = (String) vFirst;
      return (""
          + "("
          + String.join(
              ("" + " " + op + " "),
              clojure.Core.next(clojure.Core.seq(v)).stream()
                  .map(sqlbuilder.Core::whereStr)
                  .collect(Collectors.toList()))
          + ")");
    } else {
      return (String) clause;
    }
  }

  public static final String groupByStr(final ArrayList<Object> join) {
    return String.join(
        ", ",
        clojure.Core.seq(join).stream()
            .map(sqlbuilder.Core::castToStr)
            .collect(Collectors.toList()));
  }

  public static final String havingStr(final Object having) {
    return sqlbuilder.Core.whereStr(having);
  }

  public static final String format(final HashMap<String, Object> queryMap) {
    final Object select = clojure.lang.RT.get(queryMap, ":select", null);
    final Object from = clojure.lang.RT.get(queryMap, ":from", null);
    final Object join = clojure.lang.RT.get(queryMap, ":join", null);
    final Object whereClause = clojure.lang.RT.get(queryMap, ":where", null);
    final Object groupBy = clojure.lang.RT.get(queryMap, ":group-by", null);
    final Object having = clojure.lang.RT.get(queryMap, ":having", null);
    String tmp1;
    if ((select == null)) {
      tmp1 = "";
    } else {
      tmp1 = ("" + "SELECT " + sqlbuilder.Core.selectStr((ArrayList) select));
    }
    String tmp2;
    if ((from == null)) {
      tmp2 = "";
    } else {
      tmp2 = ("" + " FROM " + sqlbuilder.Core.fromStr((ArrayList) from));
    }
    String tmp3;
    if ((join == null)) {
      tmp3 = "";
    } else {
      tmp3 = ("" + " JOIN " + sqlbuilder.Core.joinStr((ArrayList) join));
    }
    String tmp4;
    if ((whereClause == null)) {
      tmp4 = "";
    } else {
      tmp4 = ("" + " WHERE " + sqlbuilder.Core.whereStr(whereClause));
    }
    String tmp5;
    if ((groupBy == null)) {
      tmp5 = "";
    } else {
      tmp5 = ("" + " GROUP BY " + sqlbuilder.Core.groupByStr((ArrayList) groupBy));
    }
    String tmp6;
    if ((having == null)) {
      tmp6 = "";
    } else {
      tmp6 = ("" + " HAVING " + sqlbuilder.Core.havingStr(having));
    }
    return ("" + tmp1 + tmp2 + tmp3 + tmp4 + tmp5 + tmp6);
  }
}
