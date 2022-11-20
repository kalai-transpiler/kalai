package sqlbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Core {
  public static final String castToStr(final Object x) {
    boolean tmp1 = (x instanceof List);
    if (tmp1) {
      final ArrayList<Object> v = (ArrayList<Object>) x;
      final Object vFirst = v.get(0);
      final String tableName = (String) vFirst;
      final Object vSecond = v.get(1);
      final String tableAlias = (String) vSecond;
      return ("" + tableName + " AS " + tableAlias);
    } else {
      boolean tmp2 = (x instanceof String);
      if (tmp2) {
        return ("" + (String) x);
      } else {
        boolean tmp3 = (x instanceof Integer);
        if (tmp3) {
          return ("" + (int) x);
        } else {
          boolean tmp4 = (x instanceof Long);
          if (tmp4) {
            return ("" + (long) x);
          } else {
            return "";
          }
        }
      }
    }
  }

  public static final String selectStr(final ArrayList<Object> select) {
    return String.join(
        ", ",
        select.stream()
            .map(
                (a) -> {
                  return castToStr(a);
                })
            .collect(Collectors.toList()));
  }

  public static final String fromStr(final ArrayList<Object> from) {
    return String.join(
        ", ",
        from.stream()
            .map(
                (a) -> {
                  return castToStr(a);
                })
            .collect(Collectors.toList()));
  }

  public static final String joinStr(final ArrayList<Object> join) {
    return String.join(
        ", ",
        join.stream()
            .map(
                (a) -> {
                  return castToStr(a);
                })
            .collect(Collectors.toList()));
  }

  public static final String whereStr(final Object clause) {
    boolean tmp5 = (clause instanceof List);
    if (tmp5) {
      final ArrayList<Object> v = (ArrayList<Object>) clause;
      final Object vFirst = v.stream().findFirst().get();
      final String op = (String) vFirst;
      return (""
          + "("
          + String.join(
              ("" + " " + op + " "),
              v.stream()
                  .skip(1L)
                  .map(
                      (a) -> {
                        return whereStr(a);
                      })
                  .collect(Collectors.toList()))
          + ")");
    } else {
      return castToStr(clause);
    }
  }

  public static final String groupByStr(final ArrayList<Object> join) {
    return String.join(
        ", ",
        join.stream()
            .map(
                (a) -> {
                  return castToStr(a);
                })
            .collect(Collectors.toList()));
  }

  public static final String havingStr(final Object having) {
    return whereStr(having);
  }

  public static final String rowStr(final Object row) {
    final ArrayList<Object> mrow = (ArrayList<Object>) row;
    return (""
        + "("
        + String.join(
            ", ",
            mrow.stream()
                .map(
                    (a) -> {
                      return castToStr(a);
                    })
                .collect(Collectors.toList()))
        + ")");
  }

  public static final String format(final HashMap<String, Object> queryMap) {
    final Object select = queryMap.getOrDefault(":select", null);
    final Object from = queryMap.getOrDefault(":from", null);
    final Object join = queryMap.getOrDefault(":join", null);
    final Object whereClause = queryMap.getOrDefault(":where", null);
    final Object groupBy = queryMap.getOrDefault(":group-by", null);
    final Object having = queryMap.getOrDefault(":having", null);
    final Object insertInto = queryMap.getOrDefault(":insert-into", null);
    final Object columns = queryMap.getOrDefault(":columns", null);
    final Object values = queryMap.getOrDefault(":values", null);
    String tmp6;
    boolean tmp7 = (insertInto == null);
    if (tmp7) {
      tmp6 = "";
    } else {
      final ArrayList<Object> v2 = (ArrayList<Object>) values;
      {
        tmp6 =
            (""
                + "INSERT INTO "
                + fromStr((ArrayList<Object>) insertInto)
                + "("
                + selectStr((ArrayList<Object>) columns)
                + ")\n"
                + "VALUES\n"
                + String.join(
                    ",\n",
                    v2.stream()
                        .map(
                            (a) -> {
                              return rowStr(a);
                            })
                        .collect(Collectors.toList())));
      }
    }
    String tmp8;
    boolean tmp9 = (select == null);
    if (tmp9) {
      tmp8 = "";
    } else {
      tmp8 = ("" + "SELECT " + selectStr((ArrayList<Object>) select));
    }
    String tmp10;
    boolean tmp11 = (from == null);
    if (tmp11) {
      tmp10 = "";
    } else {
      tmp10 = ("" + " FROM " + fromStr((ArrayList<Object>) from));
    }
    String tmp12;
    boolean tmp13 = (join == null);
    if (tmp13) {
      tmp12 = "";
    } else {
      tmp12 = ("" + " JOIN " + joinStr((ArrayList<Object>) join));
    }
    String tmp14;
    boolean tmp15 = (whereClause == null);
    if (tmp15) {
      tmp14 = "";
    } else {
      tmp14 = ("" + " WHERE " + whereStr(whereClause));
    }
    String tmp16;
    boolean tmp17 = (groupBy == null);
    if (tmp17) {
      tmp16 = "";
    } else {
      tmp16 = ("" + " GROUP BY " + groupByStr((ArrayList<Object>) groupBy));
    }
    String tmp18;
    boolean tmp19 = (having == null);
    if (tmp19) {
      tmp18 = "";
    } else {
      tmp18 = ("" + " HAVING " + havingStr(having));
    }
    return ("" + tmp6 + tmp8 + tmp10 + tmp12 + tmp14 + tmp16 + tmp18);
  }
}
