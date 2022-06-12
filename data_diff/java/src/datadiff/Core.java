package datadiff;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class Core {
public static final TYPE_MISSING diffAssociativeKey(final TYPE_MISSING a, final TYPE_MISSING b, final TYPE_MISSING k) {
final Object va = a.get(k);
final Object vb = b.get(k);
final Object vec18629 = datadiff.Core.diff(va, vb);
final Object aa = clojure.lang.RT.nth(vec__18629, 0L, null);
final Object bb = clojure.lang.RT.nth(vec__18629, 1L, null);
final Object ab = clojure.lang.RT.nth(vec__18629, 2L, null);
final Object inA = a.containsKey(k);
final Object inB = b.containsKey(k);
final TYPE_MISSING and5531Auto = inA;
"MISSING_TYPE" tmp1;
if (and__5531__auto__)
{
final TYPE_MISSING and5531Auto = inB;
"MISSING_TYPE" tmp2;
if (and__5531__auto__)
{
final Object or5533Auto = !(ab == null);
"MISSING_TYPE" tmp3;
if (or__5533__auto__)
{
tmp3 = or__5533__auto__;
}
else
{
final boolean and5531Auto = (va == null);
"MISSING_TYPE" tmp4;
if (and__5531__auto__)
{
tmp4 = (vb == null);
}
else
{
tmp4 = and__5531__auto__;
}
{
tmp3 = tmp4;
}
}
{
tmp2 = tmp3;
}
}
else
{
tmp2 = and__5531__auto__;
}
{
tmp1 = tmp2;
}
}
else
{
tmp1 = and__5531__auto__;
}
final TYPE_MISSING same = tmp1;
final TYPE_MISSING and5531Auto = inA;
"MISSING_TYPE" tmp5;
if (and__5531__auto__)
{
final Object or5533Auto = !(aa == null);
"MISSING_TYPE" tmp6;
if (or__5533__auto__)
{
tmp6 = or__5533__auto__;
}
else
{
tmp6 = !same;
}
{
tmp5 = tmp6;
}
}
else
{
tmp5 = and__5531__auto__;
}
final TYPE_MISSING p = tmp5;
final TYPE_MISSING and5531Auto = inB;
"MISSING_TYPE" tmp7;
if (and__5531__auto__)
{
final Object or5533Auto = !(bb == null);
"MISSING_TYPE" tmp8;
if (or__5533__auto__)
{
tmp8 = or__5533__auto__;
}
else
{
tmp8 = !same;
}
{
tmp7 = tmp8;
}
}
else
{
tmp7 = and__5531__auto__;
}
final TYPE_MISSING q = tmp7;
clojure.lang.PersistentArrayMap tmp9;
if (p)
{
tmp9 = new TYPE_MISSING().put(k, aa, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
}
clojure.lang.PersistentArrayMap tmp10;
if (q)
{
tmp10 = new TYPE_MISSING().put(k, bb, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
}
clojure.lang.PersistentArrayMap tmp11;
if (same)
{
tmp11 = new TYPE_MISSING().put(k, ab, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);
}
return new TYPE_MISSING().addLast(tmp9).addLast(tmp10).addLast(tmp11);
}
public static final TYPE_MISSING diffAssociative(final TYPE_MISSING a, final TYPE_MISSING b, final TYPE_MISSING ks) {
return kalai.Kalai.foldLeft(ks.stream().map((k) -> {
return datadiff.Core.diffAssociativeKey(a, b, k);
}), new TYPE_MISSING().addLast(null).addLast(null).addLast(null), (diff1, diff2) -> {
return clojure.Core.doall(clojure.Core.map(merge, diff1, diff2));
});
}
public static final TYPE_MISSING union(final TYPE_MISSING s1, final TYPE_MISSING s2) {
if ((s1.length() < s2.length()))
{
return kalai.Kalai.foldLeft(s1, s2, conj);
}
else
{
return kalai.Kalai.foldLeft(s2, s1, conj);
}
}
public static final TYPE_MISSING difference(final TYPE_MISSING s1, final TYPE_MISSING s2) {
if ((s1.length() < s2.length()))
{
return kalai.Kalai.foldLeft(s1, s1, (result, item) -> {
if (s2.containsKey(item))
{
return clojure.Core.disj(result, item);
}
else
{
return result;
}
});
}
else
{
return kalai.Kalai.foldLeft(s2, s1, disj);
}
}
public static final TYPE_MISSING intersection(final TYPE_MISSING s1, final TYPE_MISSING s2) {
if ((s2.length() < s1.length()))
{
return recur(s2, s1);
}
else
{
return kalai.Kalai.foldLeft(s1, s1, (result, item) -> {
if (s2.containsKey(item))
{
return result;
}
else
{
return clojure.Core.disj(result, item);
}
});
}
}
public static final TYPE_MISSING atomDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
if ((a == b))
{
return new TYPE_MISSING().addLast(null).addLast(null).addLast(a);
}
else
{
return new TYPE_MISSING().addLast(a).addLast(b).addLast(null);
}
}
public static final TYPE_MISSING equalityPartition(final TYPE_MISSING x) {
if ((x instanceof Set))
{
return ":set";
}
else
{
if ((x instanceof Map))
{
return ":map";
}
else
{
if ((x instanceof List))
{
return ":sequence";
}
else
{
return ":atom";
}
}
}
}
public static final TYPE_MISSING mapDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
final Object abKeys = datadiff.Core.union(clojure.Core.set(clojure.Core.keys(a)), clojure.Core.set(clojure.Core.keys(b)));
return datadiff.Core.diffAssociative(a, b, abKeys);
}
public static final TYPE_MISSING setDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
return new TYPE_MISSING().addLast(clojure.Core.notEmpty(datadiff.Core.difference(a, b))).addLast(clojure.Core.notEmpty(datadiff.Core.difference(b, a))).addLast(clojure.Core.notEmpty(datadiff.Core.intersection(a, b)));
}
public static final TYPE_MISSING vectorize(final TYPE_MISSING m) {
if (m.stream())
{
return kalai.Kalai.foldLeft(m, clojure.Core.vec(clojure.Core.repeat(clojure.Core.apply(max, clojure.Core.keys(m)), null)), (result, p__18670) -> {
final TYPE_MISSING vec18672 = p__18670;
final Object k = clojure.lang.RT.nth(vec__18672, 0L, null);
final Object v = clojure.lang.RT.nth(vec__18672, 1L, null);
return result.put(k, v);
});
}
else
{
return null;
}
}
public static final TYPE_MISSING sequenceDiff(final TYPE_MISSING a, final TYPE_MISSING b) {
"MISSING_TYPE" tmp12;
if ((a instanceof List))
{
tmp12 = a;
}
else
{
tmp12 = clojure.Core.vec(a);
}
"MISSING_TYPE" tmp13;
if ((b instanceof List))
{
tmp13 = b;
}
else
{
tmp13 = clojure.Core.vec(b);
}
return clojure.Core.vec(datadiff.Core.diffAssociative(tmp12, tmp13, clojure.Core.range(clojure.lang.Numbers.max(a.length(), b.length()))).stream().map(datadiff.Core::vectorize));
}
public static final TYPE_MISSING diffSimilar(final TYPE_MISSING a, final TYPE_MISSING b) {
final Object partitionA = datadiff.Core.equalityPartition(a);
final Object partitionB = datadiff.Core.equalityPartition(b);
if ((partitionA == partitionB))
{
if ((partitionA == ":set"))
{
return datadiff.Core.setDiff(a, b);
}
else
{
if ((partitionA == ":map"))
{
return datadiff.Core.mapDiff(a, b);
}
else
{
if ((partitionA == ":sequence"))
{
return datadiff.Core.sequenceDiff(a, b);
}
else
{
if ((partitionA == ":atom"))
{
return datadiff.Core.atomDiff(a, b);
}
else
{
return null;
}
}
}
}
}
else
{
return datadiff.Core.atomDiff(a, b);
}
}
public static final TYPE_MISSING diff(final Object a, final Object b) {
if ((a == b))
{
return new TYPE_MISSING().addLast(null).addLast(null).addLast(a);
}
else
{
return datadiff.Core.diffSimilar(a, b);
}
}
}
