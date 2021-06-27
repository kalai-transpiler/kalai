use crate::kalai;
pub fn cast_to_str(x: kalai::Value) -> String {
    return kalai::to_string(x.clone());
}
pub fn select_str(select: std::vec::Vec<kalai::Value>) -> String {
    return select
        .clone()
        .iter()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn from_str(from: std::vec::Vec<kalai::Value>) -> String {
    return from
        .clone()
        .iter()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn join_str(join: std::vec::Vec<kalai::Value>) -> String {
    return join
        .clone()
        .iter()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn where_str(join: kalai::Value) -> String {
    if kalai::is_vector(join.clone()) {
        let jj: std::vec::Vec<kalai::Value> = kalai::to_vector(join.clone());
        return format!(
            "{}{}{}",
            String::from("("),
            jj.clone()
                .iter()
                .map(|kalai_elem| where_str(kalai_elem.clone()))
                .collect::<Vec<String>>()
                .join(&format!("{}", String::from(" op "))),
            String::from(")")
        );
    } else {
        return kalai::to_string(join.clone());
    }
}
pub fn group_by_str(join: std::vec::Vec<kalai::Value>) -> String {
    return join
        .clone()
        .iter()
        .map(|kalai_elem| cast_to_str(kalai_elem.clone()))
        .collect::<Vec<String>>()
        .join(&String::from(", "));
}
pub fn having_str(having: kalai::Value) -> String {
    return where_str(having);
}
pub fn format(query_map: std::collections::HashMap<String, std::vec::Vec<kalai::Value>>) -> String {
    let select: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":select")).unwrap().clone();
    let from: std::vec::Vec<kalai::Value> = query_map.get(&String::from(":from")).unwrap().clone();
    let join: std::vec::Vec<kalai::Value> = query_map.get(&String::from(":join")).unwrap().clone();
    let where_clause: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":where")).unwrap().clone();
    let group_by: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":group-by")).unwrap().clone();
    let having: std::vec::Vec<kalai::Value> =
        query_map.get(&String::from(":having")).unwrap().clone();
    return format!(
        "{}{}{}{}{}{}",
        if select {
            format!("{}{}", String::from("SELECT "), select_str(select))
        } else {
            String::from("")
        },
        if from {
            format!("{}{}", String::from(" FROM "), from_str(from))
        } else {
            String::from("")
        },
        if join {
            format!("{}{}", String::from(" JOIN "), join_str(join))
        } else {
            String::from("")
        },
        if where_clause {
            format!("{}{}", String::from(" WHERE "), where_str(where_clause))
        } else {
            String::from("")
        },
        if group_by {
            format!("{}{}", String::from(" GROUP BY "), group_by_str(group_by))
        } else {
            String::from("")
        },
        if having {
            format!("{}{}", String::from(" HAVING "), having_str(having))
        } else {
            String::from("")
        }
    );
}
