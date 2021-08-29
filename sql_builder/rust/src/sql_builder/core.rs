use crate::kalai;
pub fn cast_to_str(x: kalai::Value) -> String {
    if kalai::is_vector(x.clone()) {
        let v: std::vec::Vec<kalai::Value> = kalai::to_mvector(x.clone());
        let v_first: kalai::Value = v.get(0 as usize).unwrap().clone();
        let table_name: String = kalai::to_string(v_first.clone());
        let v_second: kalai::Value = v.get(1 as usize).unwrap().clone();
        let table_alias: String = kalai::to_string(v_second.clone());
        return format!("{}{}{}", table_name, String::from(" as "), table_alias);
    } else {
        return kalai::to_string(x.clone());
    }
}
pub fn select_str(select: std::vec::Vec<kalai::Value>) -> String {
    return select
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn from_str(from: std::vec::Vec<kalai::Value>) -> String {
    return from
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn join_str(join: std::vec::Vec<kalai::Value>) -> String {
    return join
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn where_str(clause: kalai::Value) -> String {
    if kalai::is_vector(clause.clone()) {
        let v: std::vec::Vec<kalai::Value> = kalai::to_mvector(clause.clone());
        let v_first: kalai::Value = v.clone().into_iter().next().unwrap();
        let op: String = kalai::to_string(v_first.clone());
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
        return kalai::to_string(clause.clone());
    }
}
pub fn group_by_str(join: std::vec::Vec<kalai::Value>) -> String {
    return join
        .clone()
        .into_iter()
        .clone()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn having_str(having: kalai::Value) -> String {
    return where_str(having);
}
pub fn format(query_map: std::collections::HashMap<String, kalai::Value>) -> String {
    let select: kalai::Value = query_map
        .get(&String::from(":select"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let from: kalai::Value = query_map
        .get(&String::from(":from"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let join: kalai::Value = query_map
        .get(&String::from(":join"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let where_clause: kalai::Value = query_map
        .get(&String::from(":where"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let group_by: kalai::Value = query_map
        .get(&String::from(":group-by"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    let having: kalai::Value = query_map
        .get(&String::from(":having"))
        .unwrap_or(&kalai::Value::Null)
        .clone();
    return format!(
        "{}{}{}{}{}{}",
        if kalai::is_null(select.clone()) {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from("SELECT "),
                select_str(kalai::to_mvector(select.clone()))
            )
        },
        if kalai::is_null(from.clone()) {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" FROM "),
                from_str(kalai::to_mvector(from.clone()))
            )
        },
        if kalai::is_null(join.clone()) {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" JOIN "),
                join_str(kalai::to_mvector(join.clone()))
            )
        },
        if kalai::is_null(where_clause.clone()) {
            String::from("")
        } else {
            format!("{}{}", String::from(" WHERE "), where_str(where_clause))
        },
        if kalai::is_null(group_by.clone()) {
            String::from("")
        } else {
            format!(
                "{}{}",
                String::from(" GROUP BY "),
                group_by_str(kalai::to_mvector(group_by.clone()))
            )
        },
        if kalai::is_null(having.clone()) {
            String::from("")
        } else {
            format!("{}{}", String::from(" HAVING "), having_str(having))
        }
    );
}
