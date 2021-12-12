use crate::kalai;
pub fn f1() -> String {
    let query_map: crate::kalai::Map = {
        let mut tmp1: crate::kalai::Map = std::collections::HashMap::new();
        tmp1.insert(
            String::from(":from"),
            crate::kalai::BValue::from(
                {
                    let mut tmp2: crate::kalai::Vector = std::vec::Vec::new();
                    tmp2.push(crate::kalai::BValue::from(String::from("foo")));
                    tmp2
                }
                .clone(),
            ),
        );
        tmp1.insert(
            String::from(":select"),
            crate::kalai::BValue::from(
                {
                    let mut tmp3: crate::kalai::Vector = std::vec::Vec::new();
                    tmp3.push(crate::kalai::BValue::from(String::from("a")));
                    tmp3.push(crate::kalai::BValue::from(String::from("b")));
                    tmp3.push(crate::kalai::BValue::from(String::from("c")));
                    tmp3
                }
                .clone(),
            ),
        );
        tmp1.insert(
            String::from(":where"),
            crate::kalai::BValue::from(
                {
                    let mut tmp4: crate::kalai::Vector = std::vec::Vec::new();
                    tmp4.push(crate::kalai::BValue::from(String::from("=")));
                    tmp4.push(crate::kalai::BValue::from(String::from("f.a")));
                    tmp4.push(crate::kalai::BValue::from(String::from("'baz'")));
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
    let query_map: crate::kalai::Map = {
        let mut tmp5: crate::kalai::Map = std::collections::HashMap::new();
        tmp5.insert(
            String::from(":from"),
            crate::kalai::BValue::from(
                {
                    let mut tmp6: crate::kalai::Vector = std::vec::Vec::new();
                    tmp6.push(crate::kalai::BValue::from(String::from("foo")));
                    tmp6
                }
                .clone(),
            ),
        );
        tmp5.insert(
            String::from(":select"),
            crate::kalai::BValue::from(
                {
                    let mut tmp7: crate::kalai::Vector = std::vec::Vec::new();
                    tmp7.push(crate::kalai::BValue::from(String::from("*")));
                    tmp7
                }
                .clone(),
            ),
        );
        tmp5.insert(
            String::from(":where"),
            crate::kalai::BValue::from(
                {
                    let mut tmp8: crate::kalai::Vector = std::vec::Vec::new();
                    tmp8.push(crate::kalai::BValue::from(String::from("AND")));
                    tmp8.push(crate::kalai::BValue::from(
                        {
                            let mut tmp9: crate::kalai::Vector = std::vec::Vec::new();
                            tmp9.push(crate::kalai::BValue::from(String::from("=")));
                            tmp9.push(crate::kalai::BValue::from(String::from("a")));
                            tmp9.push(crate::kalai::BValue::from(1));
                            tmp9
                        }
                        .clone(),
                    ));
                    tmp8.push(crate::kalai::BValue::from(
                        {
                            let mut tmp10: crate::kalai::Vector = std::vec::Vec::new();
                            tmp10.push(crate::kalai::BValue::from(String::from("<")));
                            tmp10.push(crate::kalai::BValue::from(String::from("b")));
                            tmp10.push(crate::kalai::BValue::from(100));
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
    let query_map: crate::kalai::Map = {
        let mut tmp11: crate::kalai::Map = std::collections::HashMap::new();
        tmp11.insert(
            String::from(":from"),
            crate::kalai::BValue::from(
                {
                    let mut tmp12: crate::kalai::Vector = std::vec::Vec::new();
                    tmp12.push(crate::kalai::BValue::from(
                        {
                            let mut tmp13: crate::kalai::Vector = std::vec::Vec::new();
                            tmp13.push(crate::kalai::BValue::from(String::from("foo")));
                            tmp13.push(crate::kalai::BValue::from(String::from("quux")));
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
            crate::kalai::BValue::from(
                {
                    let mut tmp14: crate::kalai::Vector = std::vec::Vec::new();
                    tmp14.push(crate::kalai::BValue::from(String::from("a")));
                    tmp14.push(crate::kalai::BValue::from(
                        {
                            let mut tmp15: crate::kalai::Vector = std::vec::Vec::new();
                            tmp15.push(crate::kalai::BValue::from(String::from("b")));
                            tmp15.push(crate::kalai::BValue::from(String::from("bar")));
                            tmp15
                        }
                        .clone(),
                    ));
                    tmp14.push(crate::kalai::BValue::from(String::from("c")));
                    tmp14.push(crate::kalai::BValue::from(
                        {
                            let mut tmp16: crate::kalai::Vector = std::vec::Vec::new();
                            tmp16.push(crate::kalai::BValue::from(String::from("d")));
                            tmp16.push(crate::kalai::BValue::from(String::from("x")));
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
            crate::kalai::BValue::from(
                {
                    let mut tmp17: crate::kalai::Vector = std::vec::Vec::new();
                    tmp17.push(crate::kalai::BValue::from(String::from("AND")));
                    tmp17.push(crate::kalai::BValue::from(
                        {
                            let mut tmp18: crate::kalai::Vector = std::vec::Vec::new();
                            tmp18.push(crate::kalai::BValue::from(String::from("=")));
                            tmp18.push(crate::kalai::BValue::from(String::from("quux.a")));
                            tmp18.push(crate::kalai::BValue::from(1));
                            tmp18
                        }
                        .clone(),
                    ));
                    tmp17.push(crate::kalai::BValue::from(
                        {
                            let mut tmp19: crate::kalai::Vector = std::vec::Vec::new();
                            tmp19.push(crate::kalai::BValue::from(String::from("<")));
                            tmp19.push(crate::kalai::BValue::from(String::from("bar")));
                            tmp19.push(crate::kalai::BValue::from(100));
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
    let query_map: crate::kalai::Map = {
        let mut tmp20: crate::kalai::Map = std::collections::HashMap::new();
        tmp20.insert(
            String::from(":columns"),
            crate::kalai::BValue::from(
                {
                    let mut tmp21: crate::kalai::Vector = std::vec::Vec::new();
                    tmp21.push(crate::kalai::BValue::from(String::from("name")));
                    tmp21.push(crate::kalai::BValue::from(String::from("surname")));
                    tmp21.push(crate::kalai::BValue::from(String::from("age")));
                    tmp21
                }
                .clone(),
            ),
        );
        tmp20.insert(
            String::from(":insert-into"),
            crate::kalai::BValue::from(
                {
                    let mut tmp22: crate::kalai::Vector = std::vec::Vec::new();
                    tmp22.push(crate::kalai::BValue::from(String::from("properties")));
                    tmp22
                }
                .clone(),
            ),
        );
        tmp20.insert(
            String::from(":values"),
            crate::kalai::BValue::from(
                {
                    let mut tmp23: crate::kalai::Vector = std::vec::Vec::new();
                    tmp23.push(crate::kalai::BValue::from(
                        {
                            let mut tmp24: crate::kalai::Vector = std::vec::Vec::new();
                            tmp24.push(crate::kalai::BValue::from(String::from("'Jon'")));
                            tmp24.push(crate::kalai::BValue::from(String::from("'Smith'")));
                            tmp24.push(crate::kalai::BValue::from(34));
                            tmp24
                        }
                        .clone(),
                    ));
                    tmp23.push(crate::kalai::BValue::from(
                        {
                            let mut tmp25: crate::kalai::Vector = std::vec::Vec::new();
                            tmp25.push(crate::kalai::BValue::from(String::from("'Andrew'")));
                            tmp25.push(crate::kalai::BValue::from(String::from("'Cooper'")));
                            tmp25.push(crate::kalai::BValue::from(12));
                            tmp25
                        }
                        .clone(),
                    ));
                    tmp23.push(crate::kalai::BValue::from(
                        {
                            let mut tmp26: crate::kalai::Vector = std::vec::Vec::new();
                            tmp26.push(crate::kalai::BValue::from(String::from("'Jane'")));
                            tmp26.push(crate::kalai::BValue::from(String::from("'Daniels'")));
                            tmp26.push(crate::kalai::BValue::from(56));
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
