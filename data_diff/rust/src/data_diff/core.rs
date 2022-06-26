use crate::kalai::kalai;
use crate::kalai::kalai::PMap;
pub fn diff_associative_key(a: TYPE_MISSING, b: TYPE_MISSING, k: TYPE_MISSING) -> TYPE_MISSING {
    let va: kalai::BValue = a.get(&k).unwrap().clone();
    let vb: kalai::BValue = b.get(&k).unwrap().clone();
    let vec_18694: kalai::BValue = diff(va, vb);
    let aa: kalai::BValue = {
        let get1 = vec_18694.get((0i64 as usize));
        if get1.is_some() {
            get1.unwrap().clone()
        } else {
            kalai::BValue::from(kalai::NIL)
        }
    };
    let bb: kalai::BValue = {
        let get2 = vec_18694.get((1i64 as usize));
        if get2.is_some() {
            get2.unwrap().clone()
        } else {
            kalai::BValue::from(kalai::NIL)
        }
    };
    let ab: kalai::BValue = {
        let get3 = vec_18694.get((2i64 as usize));
        if get3.is_some() {
            get3.unwrap().clone()
        } else {
            kalai::BValue::from(kalai::NIL)
        }
    };
    let in_a: kalai::BValue = a.contains_key(&k);
    let in_b: kalai::BValue = b.contains_key(&k);
    let same = {
        let and_5531_auto = in_a;
        if and_5531_auto {
            let and_5531_auto = in_b;
            if and_5531_auto {
                let or_5533_auto: kalai::BValue = !ab.is_type("Nil");
                if or_5533_auto {
                    or_5533_auto
                } else {
                    let and_5531_auto: bool = va.is_type("Nil");
                    if and_5531_auto {
                        vb.is_type("Nil")
                    } else {
                        and_5531_auto
                    }
                }
            } else {
                and_5531_auto
            }
        } else {
            and_5531_auto
        }
    };
    return TYPE_MISSING::new()
        .push_back(
            if {
                let and_5531_auto = in_a;
                if and_5531_auto {
                    let or_5533_auto: kalai::BValue = !aa.is_type("Nil");
                    if or_5533_auto {
                        or_5533_auto
                    } else {
                        !same
                    }
                } else {
                    and_5531_auto
                }
            } {
                TYPE_MISSING::new().insert(k.clone(), aa.clone())
            }
            .clone(),
        )
        .push_back(
            if {
                let and_5531_auto = in_b;
                if and_5531_auto {
                    let or_5533_auto: kalai::BValue = !bb.is_type("Nil");
                    if or_5533_auto {
                        or_5533_auto
                    } else {
                        !same
                    }
                } else {
                    and_5531_auto
                }
            } {
                TYPE_MISSING::new().insert(k.clone(), bb.clone())
            }
            .clone(),
        )
        .push_back(
            if same {
                TYPE_MISSING::new().insert(k.clone(), ab.clone())
            }
            .clone(),
        );
}
pub fn diff_associative(a: TYPE_MISSING, b: TYPE_MISSING, ks: TYPE_MISSING) -> TYPE_MISSING {
    return ks
        .clone()
        .into_iter()
        .map(|k| {
            return diff_associative_key(a, b, k);
        })
        .clone()
        .into_iter()
        .fold(
            TYPE_MISSING::new()
                .push_back(kalai::BValue::from(kalai::NIL).clone())
                .push_back(kalai::BValue::from(kalai::NIL).clone())
                .push_back(kalai::BValue::from(kalai::NIL).clone()),
            |diff1, diff2| {
                return std::iter::zip(diff1, diff2).map(|t| |a, b| { merge(a, b) }(t.0, t.1));
            },
        );
}
pub fn union(s1: TYPE_MISSING, s2: TYPE_MISSING) -> TYPE_MISSING {
    if ((s1.len() as i32) < (s2.len() as i32)) {
        return s1.clone().into_iter().fold(s2, conj);
    } else {
        return s2.clone().into_iter().fold(s1, conj);
    }
}
pub fn difference(s1: TYPE_MISSING, s2: TYPE_MISSING) -> TYPE_MISSING {
    if ((s1.len() as i32) < (s2.len() as i32)) {
        return s1.clone().into_iter().fold(s1, |result, item| {
            if s2.contains_key(&item) {
                return result.remove(item);
            } else {
                return result;
            }
        });
    } else {
        return s2.clone().into_iter().fold(s1, disj);
    }
}
pub fn intersection(s1: TYPE_MISSING, s2: TYPE_MISSING) -> TYPE_MISSING {
    if ((s2.len() as i32) < (s1.len() as i32)) {
        return recur(s2, s1);
    } else {
        return s1.clone().into_iter().fold(s1, |result, item| {
            if s2.contains_key(&item) {
                return result;
            } else {
                return result.remove(item);
            }
        });
    }
}
pub fn atom_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
    if (a == b) {
        return TYPE_MISSING::new()
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(a.clone());
    } else {
        return TYPE_MISSING::new()
            .push_back(a.clone())
            .push_back(b.clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone());
    }
}
pub fn equality_partition(x: TYPE_MISSING) -> TYPE_MISSING {
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
pub fn map_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
    let ab_keys: kalai::BValue = union(set(keys(a)), set(keys(b)));
    return diff_associative(a, b, ab_keys);
}
pub fn set_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
    return TYPE_MISSING::new()
        .push_back(not_empty(difference(a, b)).clone())
        .push_back(not_empty(difference(b, a)).clone())
        .push_back(not_empty(intersection(a, b)).clone());
}
pub fn vectorize(m: TYPE_MISSING) -> TYPE_MISSING {
    if m.clone().into_iter() {
        return m.clone().into_iter().fold(
            vec(repeat(apply(max, keys(m)), kalai::BValue::from(kalai::NIL))),
            |result, p_18735| {
                let vec_18737 = p_18735;
                let k: kalai::BValue = {
                    let get4 = vec_18737.get((0i64 as usize));
                    if get4.is_some() {
                        get4.unwrap().clone()
                    } else {
                        kalai::BValue::from(kalai::NIL)
                    }
                };
                let v: kalai::BValue = {
                    let get5 = vec_18737.get((1i64 as usize));
                    if get5.is_some() {
                        get5.unwrap().clone()
                    } else {
                        kalai::BValue::from(kalai::NIL)
                    }
                };
                return assoc(result, k, v);
            },
        );
    } else {
        return kalai::BValue::from(kalai::NIL);
    }
}
pub fn sequence_diff(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
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
        range(clojure.lang._numbers / max((a.len() as i32), (b.len() as i32))),
    )
    .clone()
    .into_iter()
    .map(|a| vectorize(a)));
}
pub fn diff_similar(a: TYPE_MISSING, b: TYPE_MISSING) -> TYPE_MISSING {
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
pub fn diff(a: kalai::BValue, b: kalai::BValue) -> TYPE_MISSING {
    if (a == b) {
        return TYPE_MISSING::new()
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(kalai::BValue::from(kalai::NIL).clone())
            .push_back(a.clone());
    } else {
        return diff_similar(a, b);
    }
}
