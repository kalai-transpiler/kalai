package a;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class demo02 {
public static final HashMap<Character,Integer> getDigitsMap() {
final TYPE_MISSING tmp1 = new TYPE_MISSING();
tmp1.put('٠', 0);
tmp1.put('١', 1);
tmp1.put('٢', 2);
tmp1.put('٣', 3);
tmp1.put('٤', 4);
tmp1.put('٥', 5);
tmp1.put('০', 0);
tmp1.put('٦', 6);
tmp1.put('১', 1);
tmp1.put('٧', 7);
tmp1.put('২', 2);
tmp1.put('٨', 8);
tmp1.put('৩', 3);
tmp1.put('٩', 9);
tmp1.put('৪', 4);
tmp1.put('৫', 5);
tmp1.put('৬', 6);
tmp1.put('৭', 7);
tmp1.put('৮', 8);
tmp1.put('৯', 9);
tmp1.put('0', 0);
tmp1.put('1', 1);
tmp1.put('2', 2);
tmp1.put('3', 3);
tmp1.put('4', 4);
tmp1.put('5', 5);
tmp1.put('6', 6);
tmp1.put('7', 7);
tmp1.put('8', 8);
tmp1.put('9', 9);
final HashMap<Character,Integer> m = tmp1;
return m;
}
static final HashMap<Character,Integer> digitsMap = getDigitsMap();
public static final int parse(final String s) {
int result = 0;
final int strLength = s.length();
{
long i = 0;
while ((i < strLength)) {
{
final char digit = s.get(i);
if (digitsMap.containsKey(digit))
{
final int digitVal = digitsMap.get(digit);
result = ((10 * result) + digitVal);
}
}
i = ++i;
}
return result;
}
}
public static final HashMap<String,ArrayList<Character>> getNumberSystemsMap() {
final TYPE_MISSING tmp2 = new TYPE_MISSING();
final TYPE_MISSING tmp3 = new TYPE_MISSING();
tmp3.add('٠');
tmp3.add('١');
tmp3.add('٢');
tmp3.add('٣');
tmp3.add('٤');
tmp3.add('٥');
tmp3.add('٦');
tmp3.add('٧');
tmp3.add('٨');
tmp3.add('٩');
tmp2.put("ARABIC", tmp3);
final TYPE_MISSING tmp4 = new TYPE_MISSING();
tmp4.add('0');
tmp4.add('1');
tmp4.add('2');
tmp4.add('3');
tmp4.add('4');
tmp4.add('5');
tmp4.add('6');
tmp4.add('7');
tmp4.add('8');
tmp4.add('9');
tmp2.put("LATIN", tmp4);
final TYPE_MISSING tmp5 = new TYPE_MISSING();
tmp5.add('০');
tmp5.add('১');
tmp5.add('২');
tmp5.add('৩');
tmp5.add('৪');
tmp5.add('৫');
tmp5.add('৬');
tmp5.add('৭');
tmp5.add('৮');
tmp5.add('৯');
tmp2.put("BENGALI", tmp5);
final HashMap<String,ArrayList<Character>> m = tmp2;
return m;
}
static final HashMap<String,ArrayList<Character>> numberSystemsMap = getNumberSystemsMap();
public static final HashMap<String,Character> getGroupingSeparatorsMap() {
final TYPE_MISSING tmp6 = new TYPE_MISSING();
tmp6.put("ARABIC", '٬');
tmp6.put("LATIN", ',');
tmp6.put("BENGALI", ',');
final HashMap<String,Character> m = tmp6;
return m;
}
static final HashMap<String,Character> groupingSeparatorsMap = getGroupingSeparatorsMap();
public static final ArrayList<Integer> getSeparatorPositions(final int numLength, final String groupingStrategy) {
final TYPE_MISSING tmp7 = new TYPE_MISSING();
ArrayList<Integer> result = tmp7;
if ((groupingStrategy == "NONE"))
{
return result;
}
else
{
if ((groupingStrategy == "ON_ALIGNED_3_3"))
{
int i = (numLength - 3);
{
while ((0 < i)) {
result.add(i);
i = (i - 3);
}
return result;
}
}
else
{
if ((groupingStrategy == "ON_ALIGNED_3_2"))
{
int i = (numLength - 3);
{
while ((0 < i)) {
result.add(i);
i = (i - 2);
}
return result;
}
}
else
{
if ((groupingStrategy == "MIN_2"))
{
if ((numLength <= 4))
{
return result;
}
else
{
int i = (numLength - 3);
{
while ((0 < i)) {
result.add(i);
i = (i - 3);
}
return result;
}
}
}
else
{
if (true)
{
return result;
}
else
{
return null;
}
}
}
}
}
}
public static final String format(final int num, final String numberSystem, final String groupingStrategy) {
int i = num;
final java.lang.StringBuffer result = new StringBuffer();
{
while (!(i == 0)) {
final int quotient = (i / 10);
final int remainder = (i % 10);
final ArrayList<Character> numberSystemDigits = numberSystemsMap.get(numberSystem);
final Object localDigit = numberSystemDigits.get(remainder);
{
result.insert(0, localDigit);
i = quotient;
}
}
{
final Object sep = groupingSeparatorsMap.get(numberSystem);
final int numLength = result.length();
final ArrayList<Integer> separatorPositions = getSeparatorPositions(numLength, groupingStrategy);
final int numPositions = separatorPositions.length();
long idx = 0;
while ((idx < numPositions)) {
{
final int position = separatorPositions.get(idx);
result.insert(position, sep);
}
idx = ++idx;
}
}
return result.toString();
}
}
}