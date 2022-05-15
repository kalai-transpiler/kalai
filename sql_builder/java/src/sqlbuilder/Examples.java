package sqlbuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class Examples {
  public static final String f1() {
    HashMap<String, Object> tmp1 = new HashMap<String, Object>();
    ArrayList<Object> tmp2 = new ArrayList<Object>();
    tmp2.add("foo");
    tmp1.put(":from", tmp2);
    ArrayList<Object> tmp3 = new ArrayList<Object>();
    tmp3.add("a");
    tmp3.add("b");
    tmp3.add("c");
    tmp1.put(":select", tmp3);
    ArrayList<Object> tmp4 = new ArrayList<Object>();
    tmp4.add("=");
    tmp4.add("foo.a");
    tmp4.add("'baz'");
    tmp1.put(":where", tmp4);
    final HashMap<String, Object> queryMap = tmp1;
    return sqlbuilder.Core.format(queryMap);
  }

  public static final String f2() {
    HashMap<String, Object> tmp5 = new HashMap<String, Object>();
    ArrayList<Object> tmp6 = new ArrayList<Object>();
    tmp6.add("foo");
    tmp5.put(":from", tmp6);
    ArrayList<Object> tmp7 = new ArrayList<Object>();
    tmp7.add("*");
    tmp5.put(":select", tmp7);
    ArrayList<Object> tmp8 = new ArrayList<Object>();
    tmp8.add("AND");
    ArrayList<Object> tmp9 = new ArrayList<Object>();
    tmp9.add("=");
    tmp9.add("a");
    tmp9.add(1L);
    tmp8.add(tmp9);
    ArrayList<Object> tmp10 = new ArrayList<Object>();
    tmp10.add("<");
    tmp10.add("b");
    tmp10.add(100L);
    tmp8.add(tmp10);
    tmp5.put(":where", tmp8);
    final HashMap<String, Object> queryMap = tmp5;
    return sqlbuilder.Core.format(queryMap);
  }

  public static final String f3() {
    HashMap<String, Object> tmp11 = new HashMap<String, Object>();
    ArrayList<Object> tmp12 = new ArrayList<Object>();
    ArrayList<Object> tmp13 = new ArrayList<Object>();
    tmp13.add("foo");
    tmp13.add("quux");
    tmp12.add(tmp13);
    tmp11.put(":from", tmp12);
    ArrayList<Object> tmp14 = new ArrayList<Object>();
    tmp14.add("a");
    ArrayList<Object> tmp15 = new ArrayList<Object>();
    tmp15.add("b");
    tmp15.add("bar");
    tmp14.add(tmp15);
    tmp14.add("c");
    ArrayList<Object> tmp16 = new ArrayList<Object>();
    tmp16.add("d");
    tmp16.add("x");
    tmp14.add(tmp16);
    tmp11.put(":select", tmp14);
    ArrayList<Object> tmp17 = new ArrayList<Object>();
    tmp17.add("AND");
    ArrayList<Object> tmp18 = new ArrayList<Object>();
    tmp18.add("=");
    tmp18.add("quux.a");
    tmp18.add(1L);
    tmp17.add(tmp18);
    ArrayList<Object> tmp19 = new ArrayList<Object>();
    tmp19.add("<");
    tmp19.add("bar");
    tmp19.add(100L);
    tmp17.add(tmp19);
    tmp11.put(":where", tmp17);
    final HashMap<String, Object> queryMap = tmp11;
    return sqlbuilder.Core.format(queryMap);
  }

  public static final String f4() {
    HashMap<String, Object> tmp20 = new HashMap<String, Object>();
    ArrayList<Object> tmp21 = new ArrayList<Object>();
    tmp21.add("name");
    tmp21.add("surname");
    tmp21.add("age");
    tmp20.put(":columns", tmp21);
    ArrayList<Object> tmp22 = new ArrayList<Object>();
    tmp22.add("properties");
    tmp20.put(":insert-into", tmp22);
    ArrayList<Object> tmp23 = new ArrayList<Object>();
    ArrayList<Object> tmp24 = new ArrayList<Object>();
    tmp24.add("'Jon'");
    tmp24.add("'Smith'");
    tmp24.add(34L);
    tmp23.add(tmp24);
    ArrayList<Object> tmp25 = new ArrayList<Object>();
    tmp25.add("'Andrew'");
    tmp25.add("'Cooper'");
    tmp25.add(12L);
    tmp23.add(tmp25);
    ArrayList<Object> tmp26 = new ArrayList<Object>();
    tmp26.add("'Jane'");
    tmp26.add("'Daniels'");
    tmp26.add(56L);
    tmp23.add(tmp26);
    tmp20.put(":values", tmp23);
    final HashMap<String, Object> queryMap = tmp20;
    return sqlbuilder.Core.format(queryMap);
  }

  public static final String f5() {
    HashMap<String, Object> tmp27 = new HashMap<String, Object>();
    ArrayList<Object> tmp28 = new ArrayList<Object>();
    tmp28.add("foo");
    tmp27.put(":from", tmp28);
    ArrayList<Object> tmp29 = new ArrayList<Object>();
    tmp29.add("a");
    tmp29.add("b");
    tmp29.add("c");
    tmp27.put(":select", tmp29);
    ArrayList<Object> tmp30 = new ArrayList<Object>();
    tmp30.add("=");
    tmp30.add("foo.a");
    tmp30.add("?");
    tmp27.put(":where", tmp30);
    final HashMap<String, Object> queryMap = tmp27;
    return sqlbuilder.Core.format(queryMap);
  }

  public static final void main(String[] _args) {
    {
      final String queryStr = sqlbuilder.Examples.f1();
      System.out.println(("" + "example 1 query string:\n---\n" + queryStr + "\n---\n\n"));
    }
    {
      final String queryStr = sqlbuilder.Examples.f2();
      System.out.println(("" + "example 2 query string:\n---\n" + queryStr + "\n---\n\n"));
    }
    {
      final String queryStr = sqlbuilder.Examples.f3();
      System.out.println(("" + "example 3 query string:\n---\n" + queryStr + "\n---\n\n"));
    }
    {
      final String queryStr = sqlbuilder.Examples.f4();
      System.out.println(("" + "example 4 query string:\n---\n" + queryStr + "\n---\n\n"));
    }
    {
      final String queryStr = sqlbuilder.Examples.f5();
      System.out.println(("" + "example 5 query string:\n---\n" + queryStr + "\n---\n\n"));
    }
  }
}
