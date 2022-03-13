use crate::kalai;
use crate::kalai::PMap;
pub fn cast_to_str(x: kalai::BValue) -> String {
    if (x.is_type("Vector") || x.is_type("Vec")) {
        let v: kalai::Vector = kalai::Vector::from(x);
        let v_first: kalai::BValue = v.get(0i32 as usize).unwrap().clone();
        let table_name: String = String::from(v_first);
        let v_second: kalai::BValue = v.get(1i32 as usize).unwrap().clone();
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
pub fn select_str(select: kalai::Vector) -> String {
    return select
        .clone()
        .iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn from_str(from: kalai::Vector) -> String {
    return from
        .clone()
        .iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn join_str(join: kalai::Vector) -> String {
    return join
        .clone()
        .iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn where_str(clause: kalai::BValue) -> String {
    if (clause.is_type("Vector") || clause.is_type("Vec")) {
        let v: kalai::Vector = kalai::Vector::from(clause);
        let v_first: kalai::BValue = v.clone().iter().next().unwrap();
        let op: String = String::from(v_first);
        return format!(
            "{}{}{}",
            String::from("("),
            v.clone()
                .iter()
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
pub fn group_by_str(join: kalai::Vector) -> String {
    return join
        .clone()
        .iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn having_str(having: kalai::BValue) -> String {
    return where_str(having);
}
pub fn row_str(row: kalai::BValue) -> String {
    let mrow: kalai::Vector = kalai::Vector::from(row);
    return format!(
        "{}{}{}",
        String::from("("),
        mrow.clone()
            .iter()
            .clone()
            .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
            .collect::<Vec<String>>()
            .join(&String::from(", ")),
        String::from(")")
    );
}
pub fn format(query_map: std::collections::HashMap<String, kalai::BValue>) -> String {
    let select: kalai::BValue = query_map
        .get(&String::from(":select"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let from: kalai::BValue = query_map
        .get(&String::from(":from"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let join: kalai::BValue = query_map
        .get(&String::from(":join"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let where_clause: kalai::BValue = query_map
        .get(&String::from(":where"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let group_by: kalai::BValue = query_map
        .get(&String::from(":group-by"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let having: kalai::BValue = query_map
        .get(&String::from(":having"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let insert_into: kalai::BValue = query_map
        .get(&String::from(":insert-into"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let columns: kalai::BValue = query_map
        .get(&String::from(":columns"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    let values: kalai::BValue = query_map
        .get(&String::from(":values"))
        .unwrap_or(&kalai::BValue::from(kalai::NIL))
        .clone();
    return format!(
        "{}{}{}{}{}{}{}",
        if insert_into.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}{}{}{}{}{}",
                String::from("INSERT INTO "),
                from_str(kalai::Vector::from(insert_into)),
                String::from("("),
                select_str(kalai::Vector::from(columns)),
                String::from(")\n"),
                String::from("VALUES\n"),
                {
                    let v2: kalai::Vector = kalai::Vector::from(values);
                    v2.clone()
                        .iter()
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
                select_str(kalai::Vector::from(select))
            )
        },
        if from.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" FROM "),
                from_str(kalai::Vector::from(from))
            )
        },
        if join.is_type("Nil") {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" JOIN "),
                join_str(kalai::Vector::from(join))
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
                group_by_str(kalai::Vector::from(group_by))
            )
        },
        if having.is_type("Nil") {
            String::from("")
        } else {
            format!("{}{}", String::from(" HAVING "), having_str(having))
        }
    );
}
