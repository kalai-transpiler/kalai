use crate::kalai;
pub fn cast_to_str(x: kalai::BValue) -> String {
    if x.is_type("Vector") {
        let v: crate::kalai::Vector = std::vec::Vec::from(x);
        let v_first: kalai::BValue = v.get(0 as usize).unwrap().clone();
        let table_name: String = String::from(v_first);
        let v_second: kalai::BValue = v.get(1 as usize).unwrap().clone();
        let table_alias: String = String::from(v_second);
        return format!("{}{}{}", table_name, String::from(" AS "), table_alias);
    } else {
        if x.is_type("String") {
            return format!("{}", String::from(x));
        } else {
            if x.is_type("i32") {
                return format!("{}", i32::from(x));
            } else {
                if x.is_type("i64") {
                    return format!("{}", i64::from(x));
                } else {
                    return String::from("");
                }
            }
        }
    }
}
pub fn select_str(select: crate::kalai::Vector) -> String {
    return select
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn from_str(from: crate::kalai::Vector) -> String {
    return from
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn join_str(join: crate::kalai::Vector) -> String {
    return join
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn where_str(clause: kalai::BValue) -> String {
    if clause.is_type("Vector") {
        let v: crate::kalai::Vector = std::vec::Vec::from(clause);
        let v_first: kalai::BValue = v.clone().into_iter().next().unwrap();
        let op: String = String::from(v_first);
        return format!(
            "{}{}{}",
            String::from("("),
            v.clone()
                .into_iter()
                .skip(1)
                .clone()
                .map(|kalai_elem| where_str(kalai_elem.clone()))
                .collect::<Vec<String>>()
                .join(&format!("{}{}{}", String::from(" "), op, String::from(" "))),
            String::from(")")
        );
    } else {
        return cast_to_str(clause);
    }
}
pub fn group_by_str(join: crate::kalai::Vector) -> String {
    return join
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn having_str(having: kalai::BValue) -> String {
    return where_str(having);
}
pub fn row_str(row: kalai::BValue) -> String {
    let mrow: crate::kalai::Vector = std::vec::Vec::from(row);
    return format!(
        "{}{}{}",
        String::from("("),
        mrow.clone()
            .into_iter()
            .clone()
            .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
            .collect::<Vec<String>>()
            .join(&String::from(", ")),
        String::from(")")
    );
}
pub fn format(query_map: crate::kalai::Map) -> String {
    let select: kalai::BValue = query_map
        .get(&String::from(":select"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let from: kalai::BValue = query_map
        .get(&String::from(":from"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let join: kalai::BValue = query_map
        .get(&String::from(":join"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let where_clause: kalai::BValue = query_map
        .get(&String::from(":where"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let group_by: kalai::BValue = query_map
        .get(&String::from(":group-by"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let having: kalai::BValue = query_map
        .get(&String::from(":having"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let insert_into: kalai::BValue = query_map
        .get(&String::from(":insert-into"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let columns: kalai::BValue = query_map
        .get(&String::from(":columns"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let values: kalai::BValue = query_map
        .get(&String::from(":values"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    return format!(
        "{}{}{}{}{}{}{}",
        if insert_into.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}{}{}{}{}{}",
                String::from("INSERT INTO "),
                from_str(std::vec::Vec::from(insert_into)),
                String::from("("),
                select_str(std::vec::Vec::from(columns)),
                String::from(")\n"),
                String::from("VALUES\n"),
                {
                    let v2: crate::kalai::Vector = std::vec::Vec::from(values);
                    v2.clone()
                        .into_iter()
                        .clone()
                        .map(|kalai_elem| row_str(kalai_elem.clone()))
                }
                .collect::<Vec<String>>()
                .join(&String::from(",\n"))
            )
        },
        if select.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from("SELECT "),
                select_str(std::vec::Vec::from(select))
            )
        },
        if from.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" FROM "),
                from_str(std::vec::Vec::from(from))
            )
        },
        if join.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" JOIN "),
                join_str(std::vec::Vec::from(join))
            )
        },
        if where_clause.is_type("Nil") {
            String::from("")
        } else {
            format!("{}{}", String::from(" WHERE "), where_str(where_clause))
        },
        if group_by.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" GROUP BY "),
                group_by_str(std::vec::Vec::from(group_by))
            )
        },
        if having.is_type("Nil") {
            String::from("")
        } else {
            format!("{}{}", String::from(" HAVING "), having_str(having))
        }
    );
}
