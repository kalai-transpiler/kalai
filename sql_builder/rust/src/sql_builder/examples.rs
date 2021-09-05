use crate::kalai;
pub fn f_1() -> String {
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp_1: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp_1.insert(
            String::from(":from"),
            kalai::Value::MVector(
                {
                    let mut tmp_2: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_2.push(kalai::Value::String(String::from("foo")));
                    tmp_2
                }
                .clone(),
            ),
        );
        tmp_1.insert(
            String::from(":select"),
            kalai::Value::MVector(
                {
                    let mut tmp_3: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_3.push(kalai::Value::String(String::from("a")));
                    tmp_3.push(kalai::Value::String(String::from("b")));
                    tmp_3.push(kalai::Value::String(String::from("c")));
                    tmp_3
                }
                .clone(),
            ),
        );
        tmp_1.insert(
            String::from(":where"),
            kalai::Value::MVector(
                {
                    let mut tmp_4: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_4.push(kalai::Value::String(String::from("=")));
                    tmp_4.push(kalai::Value::String(String::from("f.a")));
                    tmp_4.push(kalai::Value::String(String::from("'baz'")));
                    tmp_4
                }
                .clone(),
            ),
        );
        tmp_1
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn f_2() -> String {
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp_5: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp_5.insert(
            String::from(":from"),
            kalai::Value::MVector(
                {
                    let mut tmp_6: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_6.push(kalai::Value::String(String::from("foo")));
                    tmp_6
                }
                .clone(),
            ),
        );
        tmp_5.insert(
            String::from(":select"),
            kalai::Value::MVector(
                {
                    let mut tmp_7: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_7.push(kalai::Value::String(String::from("*")));
                    tmp_7
                }
                .clone(),
            ),
        );
        tmp_5.insert(
            String::from(":where"),
            kalai::Value::MVector(
                {
                    let mut tmp_8: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_8.push(kalai::Value::String(String::from("AND")));
                    tmp_8.push(kalai::Value::MVector(
                        {
                            let mut tmp_9: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_9.push(kalai::Value::String(String::from("=")));
                            tmp_9.push(kalai::Value::String(String::from("a")));
                            tmp_9.push(kalai::Value::Long(1));
                            tmp_9
                        }
                        .clone(),
                    ));
                    tmp_8.push(kalai::Value::MVector(
                        {
                            let mut tmp_10: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_10.push(kalai::Value::String(String::from("<")));
                            tmp_10.push(kalai::Value::String(String::from("b")));
                            tmp_10.push(kalai::Value::Long(100));
                            tmp_10
                        }
                        .clone(),
                    ));
                    tmp_8
                }
                .clone(),
            ),
        );
        tmp_5
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn f_3() -> String {
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp_11: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp_11.insert(
            String::from(":from"),
            kalai::Value::MVector(
                {
                    let mut tmp_12: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_12.push(kalai::Value::MVector(
                        {
                            let mut tmp_13: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_13.push(kalai::Value::String(String::from("foo")));
                            tmp_13.push(kalai::Value::String(String::from("quux")));
                            tmp_13
                        }
                        .clone(),
                    ));
                    tmp_12
                }
                .clone(),
            ),
        );
        tmp_11.insert(
            String::from(":select"),
            kalai::Value::MVector(
                {
                    let mut tmp_14: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_14.push(kalai::Value::String(String::from("a")));
                    tmp_14.push(kalai::Value::MVector(
                        {
                            let mut tmp_15: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_15.push(kalai::Value::String(String::from("b")));
                            tmp_15.push(kalai::Value::String(String::from("bar")));
                            tmp_15
                        }
                        .clone(),
                    ));
                    tmp_14.push(kalai::Value::String(String::from("c")));
                    tmp_14.push(kalai::Value::MVector(
                        {
                            let mut tmp_16: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_16.push(kalai::Value::String(String::from("d")));
                            tmp_16.push(kalai::Value::String(String::from("x")));
                            tmp_16
                        }
                        .clone(),
                    ));
                    tmp_14
                }
                .clone(),
            ),
        );
        tmp_11.insert(
            String::from(":where"),
            kalai::Value::MVector(
                {
                    let mut tmp_17: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_17.push(kalai::Value::String(String::from("AND")));
                    tmp_17.push(kalai::Value::MVector(
                        {
                            let mut tmp_18: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_18.push(kalai::Value::String(String::from("=")));
                            tmp_18.push(kalai::Value::String(String::from("quux.a")));
                            tmp_18.push(kalai::Value::Long(1));
                            tmp_18
                        }
                        .clone(),
                    ));
                    tmp_17.push(kalai::Value::MVector(
                        {
                            let mut tmp_19: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_19.push(kalai::Value::String(String::from("<")));
                            tmp_19.push(kalai::Value::String(String::from("bar")));
                            tmp_19.push(kalai::Value::Long(100));
                            tmp_19
                        }
                        .clone(),
                    ));
                    tmp_17
                }
                .clone(),
            ),
        );
        tmp_11
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn f_4() -> String {
    let query_map: std::collections::HashMap<String, kalai::Value> = {
        let mut tmp_20: std::collections::HashMap<String, kalai::Value> =
            std::collections::HashMap::new();
        tmp_20.insert(
            String::from(":columns"),
            kalai::Value::MVector(
                {
                    let mut tmp_21: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_21.push(kalai::Value::String(String::from("name")));
                    tmp_21.push(kalai::Value::String(String::from("surname")));
                    tmp_21.push(kalai::Value::String(String::from("age")));
                    tmp_21
                }
                .clone(),
            ),
        );
        tmp_20.insert(
            String::from(":insert-into"),
            kalai::Value::MVector(
                {
                    let mut tmp_22: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_22.push(kalai::Value::String(String::from("properties")));
                    tmp_22
                }
                .clone(),
            ),
        );
        tmp_20.insert(
            String::from(":values"),
            kalai::Value::MVector(
                {
                    let mut tmp_23: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                    tmp_23.push(kalai::Value::MVector(
                        {
                            let mut tmp_24: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_24.push(kalai::Value::String(String::from("'Jon'")));
                            tmp_24.push(kalai::Value::String(String::from("'Smith'")));
                            tmp_24.push(kalai::Value::Long(34));
                            tmp_24
                        }
                        .clone(),
                    ));
                    tmp_23.push(kalai::Value::MVector(
                        {
                            let mut tmp_25: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_25.push(kalai::Value::String(String::from("'Andrew'")));
                            tmp_25.push(kalai::Value::String(String::from("'Cooper'")));
                            tmp_25.push(kalai::Value::Long(12));
                            tmp_25
                        }
                        .clone(),
                    ));
                    tmp_23.push(kalai::Value::MVector(
                        {
                            let mut tmp_26: std::vec::Vec<kalai::Value> = std::vec::Vec::new();
                            tmp_26.push(kalai::Value::String(String::from("'Jane'")));
                            tmp_26.push(kalai::Value::String(String::from("'Daniels'")));
                            tmp_26.push(kalai::Value::Long(56));
                            tmp_26
                        }
                        .clone(),
                    ));
                    tmp_23
                }
                .clone(),
            ),
        );
        tmp_20
    };
    return crate::sql_builder::core::format(query_map);
}
pub fn main() {
    let _args: std::vec::Vec<String> = std::env::args().collect();
    {
        {
            let query_str: String = f_1();
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
            let query_str: String = f_2();
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
            let query_str: String = f_3();
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
            let query_str: String = f_4();
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
