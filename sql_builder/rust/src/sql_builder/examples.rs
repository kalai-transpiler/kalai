use crate::kalai;
pub fn f1() -> String {
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp1: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp1.insert(
            String::from(":from"),
            kalai::Value::MVector(
                {
                    let mut tmp2: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp2.push(kalai::Value::String(String::from("foo")));
                    tmp2
                }
                .clone(),
            ),
        );
        tmp1.insert(
            String::from(":select"),
            kalai::Value::MVector(
                {
                    let mut tmp3: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp3.push(kalai::Value::String(String::from("a")));
                    tmp3.push(kalai::Value::String(String::from("b")));
                    tmp3.push(kalai::Value::String(String::from("c")));
                    tmp3
                }
                .clone(),
            ),
        );
        tmp1.insert(
            String::from(":where"),
            kalai::Value::MVector(
                {
                    let mut tmp4: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp4.push(kalai::Value::String(String::from("=")));
                    tmp4.push(kalai::Value::String(String::from("f.a")));
                    tmp4.push(kalai::Value::String(String::from("'baz'")));
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
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp5: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp5.insert(
            String::from(":from"),
            kalai::Value::MVector(
                {
                    let mut tmp6: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp6.push(kalai::Value::String(String::from("foo")));
                    tmp6
                }
                .clone(),
            ),
        );
        tmp5.insert(
            String::from(":select"),
            kalai::Value::MVector(
                {
                    let mut tmp7: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp7.push(kalai::Value::String(String::from("*")));
                    tmp7
                }
                .clone(),
            ),
        );
        tmp5.insert(
            String::from(":where"),
            kalai::Value::MVector(
                {
                    let mut tmp8: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp8.push(kalai::Value::String(String::from("AND")));
                    tmp8.push(kalai::Value::MVector(
                        {
                            let mut tmp9: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp9.push(kalai::Value::String(String::from("=")));
                            tmp9.push(kalai::Value::String(String::from("a")));
                            tmp9.push(kalai::Value::Long(1));
                            tmp9
                        }
                        .clone(),
                    ));
                    tmp8.push(kalai::Value::MVector(
                        {
                            let mut tmp10: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp10.push(kalai::Value::String(String::from("<")));
                            tmp10.push(kalai::Value::String(String::from("b")));
                            tmp10.push(kalai::Value::Long(100));
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
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp11: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp11.insert(
            String::from(":from"),
            kalai::Value::MVector(
                {
                    let mut tmp12: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp12.push(kalai::Value::MVector(
                        {
                            let mut tmp13: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp13.push(kalai::Value::String(String::from("foo")));
                            tmp13.push(kalai::Value::String(String::from("quux")));
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
            kalai::Value::MVector(
                {
                    let mut tmp14: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp14.push(kalai::Value::String(String::from("a")));
                    tmp14.push(kalai::Value::MVector(
                        {
                            let mut tmp15: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp15.push(kalai::Value::String(String::from("b")));
                            tmp15.push(kalai::Value::String(String::from("bar")));
                            tmp15
                        }
                        .clone(),
                    ));
                    tmp14.push(kalai::Value::String(String::from("c")));
                    tmp14.push(kalai::Value::MVector(
                        {
                            let mut tmp16: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp16.push(kalai::Value::String(String::from("d")));
                            tmp16.push(kalai::Value::String(String::from("x")));
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
            kalai::Value::MVector(
                {
                    let mut tmp17: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp17.push(kalai::Value::String(String::from("AND")));
                    tmp17.push(kalai::Value::MVector(
                        {
                            let mut tmp18: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp18.push(kalai::Value::String(String::from("=")));
                            tmp18.push(kalai::Value::String(String::from("quux.a")));
                            tmp18.push(kalai::Value::Long(1));
                            tmp18
                        }
                        .clone(),
                    ));
                    tmp17.push(kalai::Value::MVector(
                        {
                            let mut tmp19: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp19.push(kalai::Value::String(String::from("<")));
                            tmp19.push(kalai::Value::String(String::from("bar")));
                            tmp19.push(kalai::Value::Long(100));
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
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp20: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp20.insert(
            String::from(":columns"),
            kalai::Value::MVector(
                {
                    let mut tmp21: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp21.push(kalai::Value::String(String::from("name")));
                    tmp21.push(kalai::Value::String(String::from("surname")));
                    tmp21.push(kalai::Value::String(String::from("age")));
                    tmp21
                }
                .clone(),
            ),
        );
        tmp20.insert(
            String::from(":insert-into"),
            kalai::Value::MVector(
                {
                    let mut tmp22: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp22.push(kalai::Value::String(String::from("properties")));
                    tmp22
                }
                .clone(),
            ),
        );
        tmp20.insert(
            String::from(":values"),
            kalai::Value::MVector(
                {
                    let mut tmp23: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp23.push(kalai::Value::MVector(
                        {
                            let mut tmp24: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp24.push(kalai::Value::String(String::from("'Jon'")));
                            tmp24.push(kalai::Value::String(String::from("'Smith'")));
                            tmp24.push(kalai::Value::Long(34));
                            tmp24
                        }
                        .clone(),
                    ));
                    tmp23.push(kalai::Value::MVector(
                        {
                            let mut tmp25: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp25.push(kalai::Value::String(String::from("'Andrew'")));
                            tmp25.push(kalai::Value::String(String::from("'Cooper'")));
                            tmp25.push(kalai::Value::Long(12));
                            tmp25
                        }
                        .clone(),
                    ));
                    tmp23.push(kalai::Value::MVector(
                        {
                            let mut tmp26: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp26.push(kalai::Value::String(String::from("'Jane'")));
                            tmp26.push(kalai::Value::String(String::from("'Daniels'")));
                            tmp26.push(kalai::Value::Long(56));
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
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        {
            let query_str: String = f1();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 1 query string: ["),
                    query_str,
                    String::from("]")
                )
            );
        }
        {
            let query_str: String = f2();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 2 query string: ["),
                    query_str,
                    String::from("]")
                )
            );
        }
        {
            let query_str: String = f3();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 3 query string: ["),
                    query_str,
                    String::from("]")
                )
            );
        }
        {
            let query_str: String = f4();
            println!(
                "{}",
                format!(
                    "{}{}{}",
                    String::from("example 4 query string: ["),
                    query_str,
                    String::from("]")
                )
            );
        }
    }
}
