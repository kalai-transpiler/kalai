use crate::kalai::kalai;
use crate::kalai::kalai::*;
pub fn diff_associative_key(a: kalai::BValue, b: kalai::BValue, k: kalai::BValue) -> kalai::BValue {
    let va: kalai::BValue = a.get(&k).unwrap().clone();
    let vb: kalai::BValue = b.get(&k).unwrap().clone();
    let vec_18690: kalai::BValue = diff(va, vb);
    let aa: kalai::BValue = {
        let get1 = vec_18690.get((0i64 as usize));
        if get1.is_some() {
            get1.unwrap().clone()
        } else {
            kalai::BValue::from(kalai::NIL)
        }
    };
    let bb: kalai::BValue = {
        let get2 = vec_18690.get((1i64 as usize));
        if get2.is_some() {
            get2.unwrap().clone()
        } else {
            kalai::BValue::from(kalai::NIL)
        }
    };
    let ab: kalai::BValue = {
        let get3 = vec_18690.get((2i64 as usize));
        if get3.is_some() {
            get3.unwrap().clone()
        } else {
            kalai::BValue::from(kalai::NIL)
        }
    };
    let in_a: kalai::BValue = a.contains_key(&k);
    let in_b: kalai::BValue = b.contains_key(&k);
    let same = {
        let and_5579_auto = in_a;
        if and_5579_auto {
            let and_5579_auto = in_b;
            if and_5579_auto {
                let or_5581_auto: kalai::BValue = !ab.is_type("Nil");
                if or_5581_auto {
                    or_5581_auto
                } else {
                    let and_5579_auto: bool = va.is_type("Nil");
                    if and_5579_auto {
                        vb.is_type("Nil")
                    } else {
                        and_5579_auto
                    }
                }
            } else {
                and_5579_auto
            }
        } else {
            and_5579_auto
        }
    };
    return kalai::BValue::new()
        .push_back(
            if {
                let and_5579_auto = in_a;
                if and_5579_auto {
                    let or_5581_auto: kalai::BValue = !aa.is_type("Nil");
                    if or_5581_auto {
                        or_5581_auto
                    } else {
                        !same
                    }
                } else {
                    and_5579_auto
                }
            } {
                kalai::BValue::new().insert(k.clone(), aa.clone())
            }
            .clone(),
        )
        .push_back(
            if {
                let and_5579_auto = in_b;
                if and_5579_auto {
                    let or_5581_auto: kalai::BValue = !bb.is_type("Nil");
                    if or_5581_auto {
                        or_5581_auto
                    } else {
                        !same
                    }
                } else {
                    and_5579_auto
                }
            } {
                kalai::BValue::new().insert(k.clone(), bb.clone())
            }
            .clone(),
        )
        .push_back(
            if same {
                kalai::BValue::new().insert(k.clone(), ab.clone())
            }
            .clone(),
        );
}
pub fn merge2(m1: kalai::BValue, m2: kalai::BValue) -> kalai::BValue {
    return reduce(conj, m1, m2);
}
pub fn diff_associative(a: kalai::BValue, b: kalai::BValue, ks: kalai::BValue) -> kalai::BValue {
    return reduce(
        |diff1, diff2| {
            return std::iter::zip(seq(diff1), seq(diff2))
                .map(|t| |a, b| { merge2(a, b) }(t.0, t.1));
        },
        kalai::BValue::new()
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone()),
        seq(ks).clone().into_iter().map(|k| {
            return diff_associative_key(a, b, k);
        }),
    );
}
pub fn union(s1: kalai::BValue, s2: kalai::BValue) -> kalai::BValue {
    if ((s1.len() as i32) < (s2.len() as i32)) {
        return reduce(conj, s2, s1);
    } else {
        return reduce(conj, s1, s2);
    }
}
pub fn difference(s1: kalai::BValue, s2: kalai::BValue) -> kalai::BValue {
    if ((s1.len() as i32) < (s2.len() as i32)) {
        return reduce(
            |result, item| {
                if s2.contains_key(&item) {
                    return result.remove(item);
                } else {
                    return result;
                }
            },
            s1,
            s1,
        );
    } else {
        return reduce(disj, s1, s2);
    }
}
pub fn intersection(s1: kalai::BValue, s2: kalai::BValue) -> kalai::BValue {
    if ((s2.len() as i32) < (s1.len() as i32)) {
        return intersection(s2, s1);
    } else {
        return reduce(
            |result, item| {
                if s2.contains_key(&item) {
                    return result;
                } else {
                    return result.remove(item);
                }
            },
            s1,
            s1,
        );
    }
}
pub fn atom_diff(a: kalai::BValue, b: kalai::BValue) -> kalai::BValue {
    if (a == b) {
        return kalai::BValue::new()
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(a.clone());
    } else {
        return kalai::BValue::new()
            .push_back(a.clone())
            .push_back(b.clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone());
    }
}
pub fn equality_partition(x: kalai::BValue) -> kalai::BValue {
    if x.is_type("Set") {
        return String::from(":set");
    } else {
        if (x.is_type("Map") || x.is_type("PMap")) {
            return String::from(":map");
        } else {
            if (x.is_type("Vector") || x.is_type("Vec")) {
                return String::from(":sequence");
            } else {
                return String::from(":atom");
            }
        }
    }
}
pub fn map_diff(a: kalai::BValue, b: kalai::BValue) -> kalai::BValue {
    let ab_keys: kalai::BValue = union(keys(a), keys(b));
    return diff_associative(a, b, ab_keys);
}
pub fn set_diff(a: kalai::BValue, b: kalai::BValue) -> kalai::BValue {
    return kalai::BValue::new()
        .push_back(not_empty(difference(a, b)).clone())
        .push_back(not_empty(difference(b, a)).clone())
        .push_back(not_empty(intersection(a, b)).clone());
}
pub fn vectorize(m: kalai::BValue) -> kalai::BValue {
    if seq(m) {
        return reduce(
            |result, p_18733| {
                let vec_18735 = p_18733;
                let k: kalai::BValue = {
                    let get4 = vec_18735.get((0i64 as usize));
                    if get4.is_some() {
                        get4.unwrap().clone()
                    } else {
                        kalai::BValue::from(kalai::NIL)
                    }
                };
                let v: kalai::BValue = {
                    let get5 = vec_18735.get((1i64 as usize));
                    if get5.is_some() {
                        get5.unwrap().clone()
                    } else {
                        kalai::BValue::from(kalai::NIL)
                    }
                };
                return assoc(result, k, v);
            },
            vec(repeat(
                reduce(max, keys(m)),
                kalai::BValue::from(kalai::NIL),
            )),
            m,
        );
    } else {
        return kalai::BValue::from(kalai::NIL);
    }
}
pub fn sequence_diff(a: kalai::BValue, b: kalai::BValue) -> kalai::BValue {
    return vec(diff_associative(
        if (a.is_type("Vector") || a.is_type("Vec")) {
            a
        } else {
            vec(a)
        },
        if (b.is_type("Vector") || b.is_type("Vec")) {
            b
        } else {
            vec(b)
        },
        range(max((a.len() as i32), (b.len() as i32))),
    )
    .clone()
    .into_iter()
    .map(|a| vectorize(a)));
}
pub fn diff_similar(a: kalai::BValue, b: kalai::BValue) -> kalai::BValue {
    let partition_a: kalai::BValue = equality_partition(a);
    let partition_b: kalai::BValue = equality_partition(b);
    if (partition_a == partition_b) {
        if (partition_a == String::from(":set")) {
            return set_diff(a, b);
        } else {
            if (partition_a == String::from(":map")) {
                return map_diff(a, b);
            } else {
                if (partition_a == String::from(":sequence")) {
                    return sequence_diff(a, b);
                } else {
                    if (partition_a == String::from(":atom")) {
                        return atom_diff(a, b);
                    } else {
                        return kalai::BValue::from(kalai::NIL);
                    }
                }
            }
        }
    } else {
        return atom_diff(a, b);
    }
}
pub fn diff(a: kalai::BValue, b: kalai::BValue) -> kalai::BValue {
    if (a == b) {
        return kalai::BValue::new()
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(a.clone());
    } else {
        return diff_similar(a, b);
    }
}
