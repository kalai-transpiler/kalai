use crate::kalai;
use crate::kalai::PMap;
pub fn f1() -> String {
    let query_map: std::collections::HashMap<String, kalai::BValue> = {
        let mut tmp1: std::collections::HashMap<String, kalai::BValue> =
            std::collections::HashMap::new();
        tmp1.insert(
            String::from(":from"),
            kalai::BValue::from(
                {
                    let mut tmp2: kalai::Vector = kalai::Vector::new();
                    tmp2.push(kalai::BValue::from(String::from("foo")));
                    tmp2
                }
                .clone(),
            ),
        );
        tmp1.insert(
            String::from(":select"),
            kalai::BValue::from(
                {
                    let mut tmp3: kalai::Vector = kalai::Vector::new();
                    tmp3.push(kalai::BValue::from(String::from("a")));
                    tmp3.push(kalai::BValue::from(String::from("b")));
                    tmp3.push(kalai::BValue::from(String::from("c")));
                    tmp3
                }
                .clone(),
            ),
        );
        tmp1.insert(
            String::from(":where"),
            kalai::BValue::from(
                {
                    let mut tmp4: kalai::Vector = kalai::Vector::new();
                    tmp4.push(kalai::BValue::from(String::from("=")));
                    tmp4.push(kalai::BValue::from(String::from("foo.a")));
                    tmp4.push(kalai::BValue::from(String::from("'baz'")));
                    tmp4
                }
                .clone(),
            ),
        );
        tmp1
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn f2() -> String {
    let query_map: std::collections::HashMap<String, kalai::BValue> = {
        let mut tmp5: std::collections::HashMap<String, kalai::BValue> =
            std::collections::HashMap::new();
        tmp5.insert(
            String::from(":from"),
            kalai::BValue::from(
                {
                    let mut tmp6: kalai::Vector = kalai::Vector::new();
                    tmp6.push(kalai::BValue::from(String::from("foo")));
                    tmp6
                }
                .clone(),
            ),
        );
        tmp5.insert(
            String::from(":select"),
            kalai::BValue::from(
                {
                    let mut tmp7: kalai::Vector = kalai::Vector::new();
                    tmp7.push(kalai::BValue::from(String::from("*")));
                    tmp7
                }
                .clone(),
            ),
        );
        tmp5.insert(
            String::from(":where"),
            kalai::BValue::from(
                {
                    let mut tmp8: kalai::Vector = kalai::Vector::new();
                    tmp8.push(kalai::BValue::from(String::from("AND")));
                    tmp8.push(kalai::BValue::from(
                        {
                            let mut tmp9: kalai::Vector = kalai::Vector::new();
                            tmp9.push(kalai::BValue::from(String::from("=")));
                            tmp9.push(kalai::BValue::from(String::from("a")));
                            tmp9.push(kalai::BValue::from(1i64));
                            tmp9
                        }
                        .clone(),
                    ));
                    tmp8.push(kalai::BValue::from(
                        {
                            let mut tmp10: kalai::Vector = kalai::Vector::new();
                            tmp10.push(kalai::BValue::from(String::from("<")));
                            tmp10.push(kalai::BValue::from(String::from("b")));
                            tmp10.push(kalai::BValue::from(100i64));
                            tmp10
                        }
                        .clone(),
                    ));
                    tmp8
                }
                .clone(),
            ),
        );
        tmp5
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn f3() -> String {
    let query_map: std::collections::HashMap<String, kalai::BValue> = {
        let mut tmp11: std::collections::HashMap<String, kalai::BValue> =
            std::collections::HashMap::new();
        tmp11.insert(
            String::from(":from"),
            kalai::BValue::from(
                {
                    let mut tmp12: kalai::Vector = kalai::Vector::new();
                    tmp12.push(kalai::BValue::from(
                        {
                            let mut tmp13: kalai::Vector = kalai::Vector::new();
                            tmp13.push(kalai::BValue::from(String::from("foo")));
                            tmp13.push(kalai::BValue::from(String::from("quux")));
                            tmp13
                        }
                        .clone(),
                    ));
                    tmp12
                }
                .clone(),
            ),
        );
        tmp11.insert(
            String::from(":select"),
            kalai::BValue::from(
                {
                    let mut tmp14: kalai::Vector = kalai::Vector::new();
                    tmp14.push(kalai::BValue::from(String::from("a")));
                    tmp14.push(kalai::BValue::from(
                        {
                            let mut tmp15: kalai::Vector = kalai::Vector::new();
                            tmp15.push(kalai::BValue::from(String::from("b")));
                            tmp15.push(kalai::BValue::from(String::from("bar")));
                            tmp15
                        }
                        .clone(),
                    ));
                    tmp14.push(kalai::BValue::from(String::from("c")));
                    tmp14.push(kalai::BValue::from(
                        {
                            let mut tmp16: kalai::Vector = kalai::Vector::new();
                            tmp16.push(kalai::BValue::from(String::from("d")));
                            tmp16.push(kalai::BValue::from(String::from("x")));
                            tmp16
                        }
                        .clone(),
                    ));
                    tmp14
                }
                .clone(),
            ),
        );
        tmp11.insert(
            String::from(":where"),
            kalai::BValue::from(
                {
                    let mut tmp17: kalai::Vector = kalai::Vector::new();
                    tmp17.push(kalai::BValue::from(String::from("AND")));
                    tmp17.push(kalai::BValue::from(
                        {
                            let mut tmp18: kalai::Vector = kalai::Vector::new();
                            tmp18.push(kalai::BValue::from(String::from("=")));
                            tmp18.push(kalai::BValue::from(String::from("quux.a")));
                            tmp18.push(kalai::BValue::from(1i64));
                            tmp18
                        }
                        .clone(),
                    ));
                    tmp17.push(kalai::BValue::from(
                        {
                            let mut tmp19: kalai::Vector = kalai::Vector::new();
                            tmp19.push(kalai::BValue::from(String::from("<")));
                            tmp19.push(kalai::BValue::from(String::from("bar")));
                            tmp19.push(kalai::BValue::from(100i64));
                            tmp19
                        }
                        .clone(),
                    ));
                    tmp17
                }
                .clone(),
            ),
        );
        tmp11
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn f4() -> String {
    let query_map: std::collections::HashMap<String, kalai::BValue> = {
        let mut tmp20: std::collections::HashMap<String, kalai::BValue> =
            std::collections::HashMap::new();
        tmp20.insert(
            String::from(":columns"),
            kalai::BValue::from(
                {
                    let mut tmp21: kalai::Vector = kalai::Vector::new();
                    tmp21.push(kalai::BValue::from(String::from("name")));
                    tmp21.push(kalai::BValue::from(String::from("surname")));
                    tmp21.push(kalai::BValue::from(String::from("age")));
                    tmp21
                }
                .clone(),
            ),
        );
        tmp20.insert(
            String::from(":insert-into"),
            kalai::BValue::from(
                {
                    let mut tmp22: kalai::Vector = kalai::Vector::new();
                    tmp22.push(kalai::BValue::from(String::from("properties")));
                    tmp22
                }
                .clone(),
            ),
        );
        tmp20.insert(
            String::from(":values"),
            kalai::BValue::from(
                {
                    let mut tmp23: kalai::Vector = kalai::Vector::new();
                    tmp23.push(kalai::BValue::from(
                        {
                            let mut tmp24: kalai::Vector = kalai::Vector::new();
                            tmp24.push(kalai::BValue::from(String::from("'Jon'")));
                            tmp24.push(kalai::BValue::from(String::from("'Smith'")));
                            tmp24.push(kalai::BValue::from(34i64));
                            tmp24
                        }
                        .clone(),
                    ));
                    tmp23.push(kalai::BValue::from(
                        {
                            let mut tmp25: kalai::Vector = kalai::Vector::new();
                            tmp25.push(kalai::BValue::from(String::from("'Andrew'")));
                            tmp25.push(kalai::BValue::from(String::from("'Cooper'")));
                            tmp25.push(kalai::BValue::from(12i64));
                            tmp25
                        }
                        .clone(),
                    ));
                    tmp23.push(kalai::BValue::from(
                        {
                            let mut tmp26: kalai::Vector = kalai::Vector::new();
                            tmp26.push(kalai::BValue::from(String::from("'Jane'")));
                            tmp26.push(kalai::BValue::from(String::from("'Daniels'")));
                            tmp26.push(kalai::BValue::from(56i64));
                            tmp26
                        }
                        .clone(),
                    ));
                    tmp23
                }
                .clone(),
            ),
        );
        tmp20
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn f5() -> String {
    let query_map: std::collections::HashMap<String, kalai::BValue> = {
        let mut tmp27: std::collections::HashMap<String, kalai::BValue> =
            std::collections::HashMap::new();
        tmp27.insert(
            String::from(":from"),
            kalai::BValue::from(
                {
                    let mut tmp28: kalai::Vector = kalai::Vector::new();
                    tmp28.push(kalai::BValue::from(String::from("foo")));
                    tmp28
                }
                .clone(),
            ),
        );
        tmp27.insert(
            String::from(":select"),
            kalai::BValue::from(
                {
                    let mut tmp29: kalai::Vector = kalai::Vector::new();
                    tmp29.push(kalai::BValue::from(String::from("a")));
                    tmp29.push(kalai::BValue::from(String::from("b")));
                    tmp29.push(kalai::BValue::from(String::from("c")));
                    tmp29
                }
                .clone(),
            ),
        );
        tmp27.insert(
            String::from(":where"),
            kalai::BValue::from(
                {
                    let mut tmp30: kalai::Vector = kalai::Vector::new();
                    tmp30.push(kalai::BValue::from(String::from("=")));
                    tmp30.push(kalai::BValue::from(String::from("foo.a")));
                    tmp30.push(kalai::BValue::from(String::from("?")));
                    tmp30
                }
                .clone(),
            ),
        );
        tmp27
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        {
            let query_str: String = f1();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 1 query string:\n---\n"),
                    query_str,
                    String::from("\n---\n\n")
                )
            );
        }
        {
            let query_str: String = f2();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 2 query string:\n---\n"),
                    query_str,
                    String::from("\n---\n\n")
                )
            );
        }
        {
            let query_str: String = f3();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 3 query string:\n---\n"),
                    query_str,
                    String::from("\n---\n\n")
                )
            );
        }
        {
            let query_str: String = f4();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 4 query string:\n---\n"),
                    query_str,
                    String::from("\n---\n\n")
                )
            );
        }
        {
            let query_str: String = f5();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 5 query string:\n---\n"),
                    query_str,
                    String::from("\n---\n\n")
                )
            );
        }
    }
}
